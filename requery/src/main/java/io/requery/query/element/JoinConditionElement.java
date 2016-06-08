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
import io.requery.query.Exists;
import io.requery.query.Expression;
import io.requery.query.JoinAndOr;
import io.requery.query.JoinOn;
import io.requery.query.Limit;
import io.requery.query.Offset;
import io.requery.query.Return;
import io.requery.query.Selectable;
import io.requery.query.SetGroupByOrderByLimit;
import io.requery.query.SetHavingOrderByLimit;
import io.requery.query.WhereAndOr;

import java.util.Set;

public class JoinConditionElement<E> extends BaseLogicalElement<JoinConditionElement<E>, JoinAndOr<E>>
    implements JoinAndOr<E>, QueryWrapper<E>, LogicalElement {

    private final QueryElement<E> query;

    JoinConditionElement(QueryElement<E> query,
                         Set<JoinConditionElement<E>> elements,
                         Condition<?,?> condition,
                         LogicalOperator operator) {
        super(elements, condition, operator);
        this.query = query;
    }

    @Override
    JoinConditionElement<E> newElement(Set<JoinConditionElement<E>> elements, Condition<?,?> condition,
                                       LogicalOperator operator) {
        return new JoinConditionElement<>(query, elements, condition, operator);
    }

    @Override
    public QueryElement<E> unwrapQuery() {
        return query;
    }

    @Override
    public E get() {
        return query.get();
    }

    @Override
    public SetHavingOrderByLimit<E> groupBy(Expression<?>... expressions) {
        return query.groupBy(expressions);
    }

    @Override
    public <V> SetHavingOrderByLimit<E> groupBy(Expression<V> expression) {
        return query.groupBy(expression);
    }

    @Override
    public <J> JoinOn<E> join(Class<J> type) {
        return query.join(type);
    }

    @Override
    public <J> JoinOn<E> leftJoin(Class<J> type) {
        return query.leftJoin(type);
    }

    @Override
    public <J> JoinOn<E> rightJoin(Class<J> type) {
        return query.rightJoin(type);
    }

    @Override
    public <J> JoinOn<E> join(Return<J> query) {
        return this.query.join(query);
    }

    @Override
    public <J> JoinOn<E> leftJoin(Return<J> query) {
        return this.query.leftJoin(query);
    }

    @Override
    public <J> JoinOn<E> rightJoin(Return<J> query) {
        return this.query.rightJoin(query);
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
    public <V> WhereAndOr<E> where(Condition<V, ?> condition) {
        return query.where(condition);
    }

    @Override
    public Exists<SetGroupByOrderByLimit<E>> where() {
        return query.where();
    }

    @Override
    public Offset<E> limit(int limit) {
        return query.limit(limit);
    }

    @Override
    public Selectable<E> union() {
        return query.union();
    }

    @Override
    public Selectable<E> unionAll() {
        return query.unionAll();
    }

    @Override
    public Selectable<E> intersect() {
        return query.intersect();
    }

    @Override
    public Selectable<E> except() {
        return query.except();
    }

    @Override
    public Return<E> as(String alias) {
        return query.as(alias);
    }

    @Override
    public String getAlias() {
        return query.getAlias();
    }
}
