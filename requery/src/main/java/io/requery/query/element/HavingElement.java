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

package io.requery.query.element;

import io.requery.query.Expression;
import io.requery.query.Offset;
import io.requery.query.Return;
import io.requery.util.Objects;
import io.requery.query.Condition;
import io.requery.query.HavingAndOr;
import io.requery.query.Limit;

import java.util.Set;

/**
 * Having clause element.
 *
 * @param <E> result type
 */
public class HavingElement<E> implements HavingAndOr<E>, QueryWrapper<E>, LogicalElement {

    private final Set<HavingElement<E>> elements;
    private final QueryElement<E> query;
    private final LogicalOperator operator;
    private final Condition condition;

    public HavingElement(QueryElement<E> query,
                         Set<HavingElement<E>> elements,
                         Condition<?> condition,
                         LogicalOperator operator) {
        this.elements = elements;
        this.query = query;
        this.condition = condition;
        this.operator = operator;
    }

    @Override
    public <V> HavingAndOr<E> and(Condition<V> condition) {
        HavingElement<E> w = new HavingElement<>(query, elements, condition, LogicalOperator.AND);
        elements.add(w);
        return w;
    }

    @Override
    public <V> HavingAndOr<E> or(Condition<V> condition) {
        HavingElement<E> w = new HavingElement<>(query, elements, condition, LogicalOperator.OR);
        elements.add(w);
        return w;
    }

    @Override
    public Condition<?> condition() {
        return condition;
    }

    @Override
    public LogicalOperator operator() {
        return operator;
    }

    @Override
    public Offset<E> limit(int limit) {
        return query.limit(limit);
    }

    @Override
    public E get() {
        return query.get();
    }

    @Override
    public <V> Limit<E> orderBy(Expression<V> expression) {
        return query.orderBy(expression);
    }

    @Override
    public Limit<E> orderBy(Expression<?>... expressions) {
        return query.orderBy(expressions);
    }

    @Override
    public QueryElement<E> unwrapQuery() {
        return query;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof HavingElement) {
            HavingElement other = (HavingElement) obj;
            return Objects.equals(operator, other.operator) &&
                    Objects.equals(condition, other.condition);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(operator, condition);
    }

    @Override
    public Return<E> as(String alias) {
        return query.as(alias);
    }

    @Override
    public String aliasName() {
        return query.aliasName();
    }
}
