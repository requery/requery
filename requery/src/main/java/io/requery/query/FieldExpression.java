/*
 * Copyright 2018 requery.io
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

import io.requery.meta.QueryExpression;
import io.requery.query.function.Function;
import io.requery.query.function.Max;
import io.requery.query.function.Min;
import io.requery.util.Objects;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Represents a field expression for which functions, aliasing etc can be applied.
 *
 * @author Nikhil Purushe
 *
 * @param <V> field type
 */
public abstract class FieldExpression<V> implements QueryExpression<V> {

    protected FieldExpression() {
    }

    @Override
    public abstract String getName();

    @Override
    public abstract ExpressionType getExpressionType();

    @Override
    public abstract Class<V> getClassType();

    @Override
    public Expression<V> getInnerExpression() {
        return null;
    }

    @Override
    public FieldExpression<V> as(String alias) {
        return new AliasedExpression<>(this, alias);
    }

    @Override
    public String getAlias() {
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
    public Max<V> max() {
        return Max.max(this);
    }

    @Override
    public Min<V> min() {
        return Min.min(this);
    }

    @Override
    public Function<V> function(String name) {
        return new Function<V>(name, getClassType()) {
            @Override
            public Object[] arguments() {
                return new Object[]{FieldExpression.this};
            }
        };
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
    public LogicalCondition<? extends Expression<V>, ? extends Expression<V>>
        equal(Expression<V> value) {
        return new ExpressionCondition<>(this, Operator.EQUAL, value);
    }

    @Override
    public LogicalCondition<? extends Expression<V>, ? extends Expression<V>>
        notEqual(Expression<V> value) {
        return new ExpressionCondition<>(this, Operator.NOT_EQUAL, value);
    }

    @Override
    public LogicalCondition<? extends Expression<V>, ? extends Expression<V>>
        lessThan(Expression<V> value) {
        return new ExpressionCondition<>(this, Operator.LESS_THAN, value);
    }

    @Override
    public LogicalCondition<? extends Expression<V>, ? extends Expression<V>>
        greaterThan(Expression<V> value) {
        return new ExpressionCondition<>(this, Operator.GREATER_THAN, value);
    }

    @Override
    public LogicalCondition<? extends Expression<V>, ? extends Expression<V>>
        lessThanOrEqual(Expression<V> value) {
        return new ExpressionCondition<>(this, Operator.LESS_THAN_OR_EQUAL, value);
    }

    @Override
    public LogicalCondition<? extends Expression<V>, ? extends Expression<V>>
        greaterThanOrEqual(Expression<V> value) {
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
    public LogicalCondition<? extends Expression<V>, ? extends Expression<V>>
        eq(Expression<V> value) {
        return equal(value);
    }

    @Override
    public LogicalCondition<? extends Expression<V>, ? extends Expression<V>>
        ne(Expression<V> value) {
        return notEqual(value);
    }

    @Override
    public LogicalCondition<? extends Expression<V>, ? extends Expression<V>>
        lt(Expression<V> value) {
        return lessThan(value);
    }

    @Override
    public LogicalCondition<? extends Expression<V>, ? extends Expression<V>>
        gt(Expression<V> value) {
        return greaterThan(value);
    }

    @Override
    public LogicalCondition<? extends Expression<V>, ? extends Expression<V>>
        lte(Expression<V> value) {
        return lessThanOrEqual(value);
    }

    @Override
    public LogicalCondition<? extends Expression<V>, ? extends Expression<V>>
        gte(Expression<V> value) {
        return greaterThanOrEqual(value);
    }

    @Override
    public LogicalCondition<? extends Expression<V>, Collection<V>> in(Collection<V> values) {
        Objects.requireNotNull(values);
        return new ExpressionCondition<>(this, Operator.IN, values);
    }

    @SuppressWarnings("unchecked")
    @Override
    public LogicalCondition<? extends Expression<V>, ?> in(V first, Object... values) {
        Collection<V> collection = new ArrayList<>(1 + values.length);
        collection.add(first);
        for (Object o : values) {
            collection.add((V)o);
        }
        return in(collection);
    }

    @Override
    public LogicalCondition<? extends Expression<V>, Collection<V>> notIn(Collection<V> values) {
        Objects.requireNotNull(values);
        return new ExpressionCondition<>(this, Operator.NOT_IN, values);
    }

    @SuppressWarnings("unchecked")
    @Override
    public LogicalCondition<? extends Expression<V>, ?> notIn(V first, Object... values) {
        Collection<V> collection = new ArrayList<>(1 + values.length);
        collection.add(first);
        for (Object o : values) {
            collection.add((V)o);
        }
        return notIn(collection);
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
        return new ExpressionCondition<>(this, Operator.IS_NULL, null);
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
            return Objects.equals(getName(), other.getName()) &&
                   Objects.equals(getClassType(), other.getClassType()) &&
                   Objects.equals(getAlias(), other.getAlias());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(getName(), getClassType(), getAlias());
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
        public <V> LogicalCondition<LogicalCondition<L, R>, Condition<?, ?>> and(
                Condition<V, ?> condition) {
            return new ExpressionCondition<LogicalCondition<L, R>, Condition<?, ?>>(
                    this, Operator.AND, condition);
        }

        @Override
        public <V> LogicalCondition<LogicalCondition<L, R>, Condition<?, ?>> or(
                Condition<V, ?> condition) {
            return new ExpressionCondition<LogicalCondition<L, R>, Condition<?, ?>>(
                    this, Operator.OR, condition);
        }

        @Override
        public LogicalCondition<LogicalCondition<L, R>, Condition<?, ?>> not() {
            return new ExpressionCondition<LogicalCondition<L, R>, Condition<?, ?>>(
                    this, Operator.NOT, new NullOperand());
        }

        @Override
        public Operator getOperator() {
            return operator;
        }

        @Override
        public R getRightOperand() {
            return rightOperand;
        }

        @Override
        public L getLeftOperand() {
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
        public String getName() {
            return expression.getName();
        }

        @Override
        public Class<X> getClassType() {
            return expression.getClassType();
        }

        @Override
        public ExpressionType getExpressionType() {
            return ExpressionType.ORDERING;
        }

        @Override
        public Expression<X> getInnerExpression() {
            return expression;
        }
    }
}
