/*
 * Copyright 2016 requery.io
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.requery.query;

import io.requery.query.function.Abs;
import io.requery.query.function.Avg;
import io.requery.query.function.Lower;
import io.requery.query.function.Max;
import io.requery.query.function.Min;
import io.requery.query.function.Round;
import io.requery.query.function.Sum;
import io.requery.query.function.Trim;
import io.requery.query.function.Upper;
import io.requery.util.Objects;

import java.util.Collection;

/**
 * Represents a field expression for which functions, aliasing etc can be applied.
 *
 * @author Nikhil Purushe
 *
 * @param <V> field type
 */
public abstract class FieldExpression<V> implements
    Expression<V>,
    Functional<V>,
    Aliasable<Expression<V>>,
    Conditional<LogicalCondition<? extends Expression<V>, ?>, V> {

    protected FieldExpression() {
    }

    @Override
    public abstract String name();

    @Override
    public abstract ExpressionType type();

    @Override
    public abstract Class<V> classType();

    @Override
    public Expression<V> as(String alias) {
        return new AliasedExpression<>(this, alias);
    }

    @Override
    public String aliasName() {
        return null;
    }

    @Override
    public OrderingExpression<V> asc() {
        return new OrderExpression<>(this, Order.ASC);
    }

    @Override
    public OrderingExpression<V> desc() {
        return new OrderExpression<>(this, Order.DESC);
    }

    @Override
    public Abs<V> abs() {
        return Abs.abs(this);
    }

    @Override
    public Max<V> max() {
        return Max.max(this);
    }

    @Override
    public Min<V> min() {
        return Min.min(this);
    }

    @Override
    public Avg<V> avg() {
        return Avg.avg(this);
    }

    @Override
    public Sum<V> sum() {
        return Sum.sum(this);
    }

    @Override
    public Round<V> round() {
        return round(0);
    }

    @Override
    public Round<V> round(int decimals) {
        return Round.round(this, decimals);
    }

    @Override
    public Trim<V> trim(String chars) {
        return Trim.trim(this, chars);
    }

    @Override
    public Trim<V> trim() {
        return trim(null);
    }

    @Override
    public Upper<V> upper() {
        return Upper.upper(this);
    }

    @Override
    public Lower<V> lower() {
        return Lower.lower(this);
    }

    @Override
    public LogicalCondition<? extends Expression<V>, V> equal(V value) {
        if (value == null) { // allowing null value
            return isNull();
        }
        return new ExpressionCondition<>(this, Operator.EQUAL, value);
    }

    @Override
    public LogicalCondition<? extends Expression<V>, V> notEqual(V value) {
        Objects.requireNotNull(value);
        return new ExpressionCondition<>(this, Operator.NOT_EQUAL, value);
    }

    @Override
    public LogicalCondition<? extends Expression<V>, V> lessThan(V value) {
        Objects.requireNotNull(value);
        return new ExpressionCondition<>(this, Operator.LESS_THAN, value);
    }

    @Override
    public LogicalCondition<? extends Expression<V>, V> greaterThan(V value) {
        Objects.requireNotNull(value);
        return new ExpressionCondition<>(this, Operator.GREATER_THAN, value);
    }

    @Override
    public LogicalCondition<? extends Expression<V>, V> lessThanOrEqual(V value) {
        Objects.requireNotNull(value);
        return new ExpressionCondition<>(this, Operator.LESS_THAN_OR_EQUAL, value);
    }

    @Override
    public LogicalCondition<? extends Expression<V>, V> greaterThanOrEqual(V value) {
        Objects.requireNotNull(value);
        return new ExpressionCondition<>(this, Operator.GREATER_THAN_OR_EQUAL, value);
    }

    @Override
    public LogicalCondition<? extends Expression<V>, V> eq(V value) {
        return equal(value);
    }

    @Override
    public LogicalCondition<? extends Expression<V>, V> ne(V value) {
        return notEqual(value);
    }

    @Override
    public LogicalCondition<? extends Expression<V>, V> lt(V value) {
        return lessThan(value);
    }

    @Override
    public LogicalCondition<? extends Expression<V>, V> gt(V value) {
        return greaterThan(value);
    }

    @Override
    public LogicalCondition<? extends Expression<V>, V> lte(V value) {
        return lessThanOrEqual(value);
    }

    @Override
    public LogicalCondition<? extends Expression<V>, V> gte(V value) {
        return greaterThanOrEqual(value);
    }

    @Override
    public LogicalCondition<? extends Expression<V>, Collection<V>> in(Collection<V> values) {
        Objects.requireNotNull(values);
        return new ExpressionCondition<>(this, Operator.IN, values);
    }

    @Override
    public LogicalCondition<? extends Expression<V>, Collection<V>> notIn(Collection<V> values) {
        Objects.requireNotNull(values);
        return new ExpressionCondition<>(this, Operator.NOT_IN, values);
    }

    @Override
    public LogicalCondition<? extends Expression<V>, ? extends Return<?>> in(Return<?> query) {
        Objects.requireNotNull(query);
        return new ExpressionCondition<>(this, Operator.IN, query);
    }

    @Override
    public LogicalCondition<? extends Expression<V>, ? extends Return<?>> notIn(Return<?> query) {
        Objects.requireNotNull(query);
        return new ExpressionCondition<>(this, Operator.NOT_IN, query);
    }

    @Override
    public LogicalCondition<? extends Expression<V>, V> isNull() {
        return new ExpressionCondition<>(this, Operator.NULL, null);
    }

    @Override
    public LogicalCondition<? extends Expression<V>, V> notNull() {
        return new ExpressionCondition<>(this, Operator.NOT_NULL, null);
    }

    @Override
    public LogicalCondition<? extends Expression<V>, String> like(String expression) {
        Objects.requireNotNull(expression);
        return new ExpressionCondition<>(this, Operator.LIKE, expression);
    }

    @Override
    public LogicalCondition<? extends Expression<V>, String> notLike(String expression) {
        Objects.requireNotNull(expression);
        return new ExpressionCondition<>(this, Operator.NOT_LIKE, expression);
    }

    @Override
    public LogicalCondition<? extends Expression<V>, Object> between(V start, V end) {
        Objects.requireNotNull(start);
        Objects.requireNotNull(end);
        Object value = new Object[]{start, end};
        return new ExpressionCondition<>(this, Operator.BETWEEN, value);
    }

    @Override
    public boolean equals(Object obj) {
        if(obj == this) {
            return true;
        }
        if(obj instanceof FieldExpression) {
            FieldExpression other = (FieldExpression) obj;
            return Objects.equals(name(), other.name()) &&
                   Objects.equals(classType(), other.classType()) &&
                   Objects.equals(aliasName(), other.aliasName());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name(), classType(), aliasName());
    }

    private static class ExpressionCondition<L, R> implements LogicalCondition<L, R> {

        private final Operator operator;
        private final L leftOperand;
        private final R rightOperand;

        ExpressionCondition(L leftOperand, Operator operator, R rightOperand) {
            this.leftOperand = leftOperand;
            this.operator = operator;
            this.rightOperand = rightOperand;
        }

        @Override
        public <V> LogicalCondition<?, ?> and(Condition<V, ?> condition) {
            return new ExpressionCondition<>(this, Operator.AND, condition);
        }

        @Override
        public <V> LogicalCondition<?, ?> or(Condition<V, ?> condition) {
            return new ExpressionCondition<>(this, Operator.OR, condition);
        }

        @Override
        public Operator operator() {
            return operator;
        }

        @Override
        public R rightOperand() {
            return rightOperand;
        }

        @Override
        public L leftOperand() {
            return leftOperand;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof ExpressionCondition) {
                ExpressionCondition other = (ExpressionCondition) obj;
                return Objects.equals(leftOperand, other.leftOperand) &&
                    Objects.equals(operator, other.operator) &&
                    Objects.equals(rightOperand, other.rightOperand);
            }
            return false;
        }

        @Override
        public int hashCode() {
            return Objects.hash(leftOperand, rightOperand, operator);
        }
    }

    private static class OrderExpression<X> implements OrderingExpression<X> {

        private final Expression<X> expression;
        private final Order order;
        private NullOrder nullOrder;

        OrderExpression(Expression<X> expression, Order order) {
            this.expression = expression;
            this.order = order;
        }

        @Override
        public OrderingExpression<X> nullsFirst() {
            nullOrder = NullOrder.FIRST;
            return this;
        }

        @Override
        public OrderingExpression<X> nullsLast() {
            nullOrder = NullOrder.LAST;
            return this;
        }

        @Override
        public Order getOrder() {
            return order;
        }

        @Override
        public NullOrder getNullOrder() {
            return nullOrder;
        }

        @Override
        public String name() {
            return expression.name();
        }

        @Override
        public Class<X> classType() {
            return expression.classType();
        }

        @Override
        public ExpressionType type() {
            return ExpressionType.ORDERING;
        }
    }
}
