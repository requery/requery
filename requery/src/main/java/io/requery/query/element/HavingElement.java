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

import io.requery.query.Condition;
import io.requery.query.Expression;
import io.requery.query.HavingAndOr;
import io.requery.query.Limit;
import io.requery.query.Offset;
import io.requery.query.Return;

import java.util.Set;

/**
 * Having clause element.
 *
 * @param <E> result type
 */
public class HavingElement<E> extends BaseLogicalElement<HavingElement<E>, HavingAndOr<E>>
    implements HavingAndOr<E>, QueryWrapper<E>, LogicalElement {

    private final QueryElement<E> query;

    public HavingElement(QueryElement<E> query,
                         Set<HavingElement<E>> elements,
                         Condition<?,?> condition,
                         LogicalOperator operator) {
        super(elements, condition, operator);
        this.query = query;
    }

    @Override
    HavingElement<E> newElement(Set<HavingElement<E>> elements, Condition<?,?> condition,
                                LogicalOperator operator) {
        return new HavingElement<>(query, elements, condition, operator);
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
    public Return<E> as(String alias) {
        return query.as(alias);
    }

    @Override
    public String aliasName() {
        return query.aliasName();
    }
}
