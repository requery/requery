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

import io.requery.meta.Attribute;
import io.requery.meta.EntityModel;
import io.requery.meta.Type;
import io.requery.query.Aliasable;
import io.requery.query.AliasedExpression;
import io.requery.query.Condition;
import io.requery.query.Deletion;
import io.requery.query.DistinctSelection;
import io.requery.query.Exists;
import io.requery.query.Expression;
import io.requery.query.ExpressionType;
import io.requery.query.HavingAndOr;
import io.requery.query.Insertion;
import io.requery.query.JoinOn;
import io.requery.query.JoinWhereGroupByOrderBy;
import io.requery.query.Limit;
import io.requery.query.Offset;
import io.requery.query.Return;
import io.requery.query.Selectable;
import io.requery.query.Selection;
import io.requery.query.SetGroupByOrderByLimit;
import io.requery.query.SetHavingOrderByLimit;
import io.requery.query.Update;
import io.requery.query.WhereAndOr;
import io.requery.query.function.Function;
import io.requery.util.Objects;
import io.requery.util.function.Supplier;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Represents a SQL query.
 *
 * @param <E> result type
 *
 * @author Nikhil Purushe
 */
public class QueryElement<E> implements Selectable<E>,
    Selection<E>,
    DistinctSelection<E>,
    Insertion<E>,
    Deletion<E>,
    Update<E>,
    JoinWhereGroupByOrderBy<E>,
    SetGroupByOrderByLimit<E>,
    SetHavingOrderByLimit<E>,
    Offset<E>,
    Aliasable<Return<E>>,
    Expression<QueryElement>,
    QueryWrapper<E>,
    SelectionElement,
    LimitedElement,
    OrderByElement,
    GroupByElement,
    SetOperationElement,
    WhereElement {

    private final QueryType queryType;
    private final EntityModel model;
    private QueryOperation<E> operator;
    private String aliasName;
    private boolean selectDistinct;
    private Set<WhereConditionElement<E>> where;
    private Set<JoinOnElement<E>> joins;
    private Set<Expression<?>> groupBy;
    private Set<HavingConditionElement<E>> having;
    private Set<Expression<?>> orderBy;
    private Map<Expression<?>, Object> updates;
    private Set<Expression<?>> from;
    private Set<? extends Expression<?>> selection;
    private QueryElement<E> parent;
    private ExistsElement<?> whereSubQuery;
    private QueryElement<E> setQuery;
    private SetOperator setOperator;
    private Integer limit;
    private Integer offset;
    private Set<Type<?>> types;

    public QueryElement(QueryType queryType, EntityModel model, QueryOperation<E> operator) {
        this.queryType = Objects.requireNotNull(queryType);
        this.model = model;
        this.operator = operator;
        this.where = new LinkedHashSet<>();
    }

    private QueryElement(QueryElement<E> parent) {
        this(parent.queryType, parent.model, parent.operator);
        this.parent = parent;
    }

    public QueryType queryType() {
        return queryType;
    }

    @Override
    public Set<? extends Expression<?>> getSelection() {
        return selection;
    }

    @Override
    public boolean isDistinct() {
        return selectDistinct;
    }

    public Map<Expression<?>, Object> updateValues() {
        return updates == null ? Collections.<Expression<?>, Object>emptyMap() : updates;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Set<WhereConditionElement<?>> getWhereElements() {
        return (Set)where;
    }

    @Override
    public ExistsElement<?> getWhereExistsElement() {
        return whereSubQuery;
    }

    public Set<JoinOnElement<E>> joinElements() {
        return joins;
    }

    @Override
    public SetOperator getOperator() {
        return setOperator;
    }

    @Override
    public Set<Expression<?>> getOrderByExpressions() {
        return orderBy;
    }

    @Override
    public Set<Expression<?>> getGroupByExpressions() {
        return groupBy;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Set<HavingConditionElement<?>> getHavingElements() {
        return (Set)having;
    }

    @Override
    public QueryElement<E> getInnerSetQuery() {
        return setQuery;
    }

    @Override
    public Integer getLimit() {
        return limit;
    }

    @Override
    public Integer getOffset() {
        return offset;
    }

    public Set<Type<?>> entityTypes() {
        return types;
    }

    public Set<Expression<?>> fromExpressions() {
        if (from == null) {
            types = new LinkedHashSet<>();
            Set<? extends Expression<?>> expressions;
            switch (queryType) {
                case SELECT:
                    expressions = getSelection();
                    break;
                case INSERT:
                case UPDATE:
                case UPSERT:
                    expressions = updates.keySet();
                    break;
                default:
                    expressions = Collections.emptySet();
            }
            for (Expression<?> expression : expressions) {
                if (expression instanceof AliasedExpression) {
                    expression = ((AliasedExpression) expression).getInnerExpression();
                }
                if (expression instanceof Attribute) {
                    Type type = ((Attribute) expression).getDeclaringType();
                    types.add(type);
                } else if (expression instanceof Function) {
                    Function function = (Function) expression;
                    for (Object arg : function.arguments()) {
                        Type type = null;
                        if (arg instanceof Attribute) {
                            type = ((Attribute) arg).getDeclaringType();
                            types.add(type);
                        } else if (arg instanceof Class) {
                            type = model.typeOf((Class) arg);
                        }
                        if (type != null) {
                            types.add(type);
                        }
                    }
                }
            }
            if (from == null) {
                from = new LinkedHashSet<>();
            }
            if (!types.isEmpty()) {
                from.addAll(types);
            }
        }
        return from;
    }

    @Override
    public String getName() {
        return "";
    }

    @Override
    public Class<QueryElement> getClassType() {
        return QueryElement.class;
    }

    @Override
    public ExpressionType getExpressionType() {
        return ExpressionType.QUERY;
    }

    @Override
    public Return<E> as(String alias) {
        this.aliasName = alias;
        return this;
    }

    @Override
    public String getAlias() {
        return aliasName;
    }

    @Override
    public QueryElement<E> select(Expression<?>... selection) {
        this.selection = selection == null ?
            null : new LinkedHashSet<>(Arrays.asList(selection));
        return this;
    }

    @Override
    public QueryElement<E> select(Set<? extends Expression<?>> select) {
        this.selection = select;
        return this;
    }

    @Override
    public QueryElement<E> unwrapQuery() {
        return this;
    }

    @Override
    public DistinctSelection<E> distinct() {
        selectDistinct = true;
        return this;
    }

    @Override
    public QueryElement<E> from(Class<?>... types) {
        this.types = new LinkedHashSet<>();
        for (Class<?> cls : types) {
            Type<?> type = model.typeOf(cls);
            this.types.add(type);
        }
        if (from == null) {
            from = new LinkedHashSet<>();
        }
        from.addAll(this.types);
        return this;
    }

    @Override
    public QueryElement<E> from(Supplier<?>... subqueries) {
        if (from == null) {
            from = new LinkedHashSet<>();
        }
        for (Supplier supplier : subqueries) {
            if (supplier instanceof Expression) {
                from.add((Expression<?>) supplier);
            } else {
                throw new UnsupportedOperationException();
            }
        }
        return this;
    }

    @Override
    public E get() {
        return operator.evaluate(parent == null ? this : parent);
    }

    @SuppressWarnings("unchecked")
    public <F extends E> QueryElement<F> extend(io.requery.util.function.Function<E, F> transform) {
        operator = new ExtendQueryOperation<>(transform, operator);
        return (QueryElement<F>) this;
    }

    @Override
    public SetHavingOrderByLimit<E> groupBy(Expression<?>... expressions) {
        if (groupBy == null) {
            groupBy = new LinkedHashSet<>();
        }
        Collections.addAll(groupBy, expressions);
        return this;
    }

    @Override
    public <V> SetHavingOrderByLimit<E> groupBy(Expression<V> expression) {
        if (groupBy == null) {
            groupBy = new LinkedHashSet<>();
        }
        groupBy.add(expression);
        return this;
    }

    private void addJoinElement(JoinOnElement<E> element) {
        if (joins == null) {
            joins = new LinkedHashSet<>();
        }
        joins.add(element);
    }

    private <J> JoinOn<E> createJoin(Class<J> type, JoinType joinType) {
        String table = model.typeOf(type).getName();
        JoinOnElement<E> join = new JoinOnElement<>(this, table, joinType);
        addJoinElement(join);
        return join;
    }

    private <J> JoinOn<E> createJoin(Return<J> query, JoinType joinType) {
        JoinOnElement<E> join = new JoinOnElement<>(this, query, joinType);
        addJoinElement(join);
        return join;
    }

    @Override
    public <J> JoinOn<E> join(Class<J> type) {
        return createJoin(type, JoinType.INNER);
    }

    @Override
    public <J> JoinOn<E> leftJoin(Class<J> type) {
        return createJoin(type, JoinType.LEFT);
    }

    @Override
    public <J> JoinOn<E> rightJoin(Class<J> type) {
        return createJoin(type, JoinType.RIGHT);
    }

    @Override
    public <J> JoinOn<E> join(Return<J> query) {
        return createJoin(query, JoinType.INNER);
    }

    @Override
    public <J> JoinOn<E> leftJoin(Return<J> query) {
        return createJoin(query, JoinType.LEFT);
    }

    @Override
    public <J> JoinOn<E> rightJoin(Return<J> query) {
        return createJoin(query, JoinType.RIGHT);
    }

    @Override
    public <V> Limit<E> orderBy(Expression<V> expression) {
        if (orderBy == null) {
            orderBy = new LinkedHashSet<>();
        }
        orderBy.add(expression);
        return this;
    }

    @Override
    public Limit<E> orderBy(Expression<?>... expressions) {
        if (orderBy == null) {
            orderBy = new LinkedHashSet<>();
        }
        orderBy.addAll(Arrays.asList(expressions));
        return this;
    }

    @Override
    public Exists<SetGroupByOrderByLimit<E>> where() {
        ExistsElement<SetGroupByOrderByLimit<E>> element =
                new ExistsElement<SetGroupByOrderByLimit<E>>(this);
        whereSubQuery = element;
        return element;
    }

    @Override
    public <V> WhereAndOr<E> where(Condition<V, ?> condition) {
        if (where == null) {
            where = new LinkedHashSet<>();
        }
        LogicalOperator operator = where.size() > 0 ? LogicalOperator.AND : null;
        WhereConditionElement<E> element = new WhereConditionElement<>(this, where, condition, operator);
        where.add(element);
        return element;
    }

    @Override
    public <V> HavingAndOr<E> having(Condition<V, ?> condition) {
        if (having == null) {
            having = new LinkedHashSet<>();
        }
        HavingConditionElement<E> element = new HavingConditionElement<>(this, having, condition, null);
        having.add(element);
        return element;
    }

    @Override
    public Offset<E> limit(int limit) {
        this.limit = limit;
        return this;
    }

    @Override
    public Return<E> offset(int offset) {
        this.offset = offset;
        return this;
    }

    @Override
    public <V> Update<E> set(Expression<V> expression, V value) {
        value(expression, value);
        return this;
    }

    @Override
    public <V> Insertion<E> value(Expression<V> expression, V value) {
        Objects.requireNotNull(expression);
        if (updates == null) {
            updates = new LinkedHashMap<>();
        }
        updates.put(expression, value);
        return this;
    }

    @Override
    public Selectable<E> union() {
        setOperator = SetOperator.UNION;
        return setQuery = new QueryElement<>(this);
    }

    @Override
    public Selectable<E> unionAll() {
        setOperator = SetOperator.UNION_ALL;
        return setQuery = new QueryElement<>(this);
    }

    @Override
    public Selectable<E> intersect() {
        setOperator = SetOperator.INTERSECT;
        return setQuery = new QueryElement<>(this);
    }

    @Override
    public Selectable<E> except() {
        setOperator = SetOperator.EXCEPT;
        return setQuery = new QueryElement<>(this);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof QueryElement) {
            QueryElement other = (QueryElement) obj;
            return queryType == other.queryType &&
                   selectDistinct == other.selectDistinct &&
                   Objects.equals(selection, other.selection) &&
                   Objects.equals(updates, other.updates) &&
                   Objects.equals(joins, other.joins) &&
                   Objects.equals(where, other.where) &&
                   Objects.equals(orderBy, other.orderBy) &&
                   Objects.equals(groupBy, other.groupBy) &&
                   Objects.equals(having, other.having) &&
                   Objects.equals(setQuery, other.setQuery) &&
                   Objects.equals(setOperator, other.setOperator) &&
                   Objects.equals(limit, other.limit) &&
                   Objects.equals(offset, other.offset);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(queryType, selectDistinct, selection, updates, joins,
                where, orderBy, groupBy, having, limit, offset);
    }
}
