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

package io.requery.sql;

import io.requery.meta.Attribute;
import io.requery.meta.QueryAttribute;
import io.requery.meta.Type;
import io.requery.query.Aliasable;
import io.requery.query.AliasedExpression;
import io.requery.query.Condition;
import io.requery.query.Expression;
import io.requery.query.ExpressionType;
import io.requery.query.NamedExpression;
import io.requery.query.Operator;
import io.requery.query.Order;
import io.requery.query.OrderingExpression;
import io.requery.query.element.ExistsElement;
import io.requery.query.element.HavingElement;
import io.requery.query.element.JoinElement;
import io.requery.query.element.JoinOnElement;
import io.requery.query.element.LogicalElement;
import io.requery.query.element.LogicalOperator;
import io.requery.query.element.QueryElement;
import io.requery.query.element.QueryWrapper;
import io.requery.query.element.WhereElement;
import io.requery.query.function.Case;
import io.requery.query.function.Function;
import io.requery.util.function.Supplier;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static io.requery.sql.Keyword.*;

/**
 * Generates a parameterizable SQL statement from a given query.
 *
 * @param <E> result type
 *
 * @author Nikhil Purushe
 */
class QueryGenerator<E> {

    private final QueryElement<E> query;
    private final Aliases inheritedAliases;
    private final boolean parameterize;
    private QueryBuilder qb;
    private Aliases aliases;
    private boolean autoAlias;
    private BoundParameters parameters;
    private Platform platform;

    QueryGenerator(QueryElement<E> query) {
        this(query, null);
    }

    QueryGenerator(QueryElement<E> query, Aliases inherited) {
        this(query, inherited, true);
    }

    QueryGenerator(QueryElement<E> query, Aliases inherited, boolean parameterize) {
        this.query = query;
        this.inheritedAliases = inherited;
        this.parameterize = parameterize;
    }

    public BoundParameters parameters() {
        return parameters;
    }

    public String toSql(QueryBuilder qb, Platform platform) {
        this.qb = qb;
        this.platform = platform;
        aliases = inheritedAliases == null ? new Aliases() : inheritedAliases;
        if (parameterize) {
            parameters = new BoundParameters();
        }
        Set<Expression<?>> from = query.fromExpressions();
        Set<JoinOnElement<E>> joins = query.joinElements();
        autoAlias = from.size() > 1 || (joins != null && joins.size() > 0);
        switch (query.queryType()) {
            case SELECT:
                appendSelect();
                break;
            case INSERT:
                appendInsert();
                break;
            case UPDATE:
            case UPSERT:
                appendUpdate();
                break;
            case DELETE:
                appendDelete();
                break;
            case TRUNCATE:
                appendTruncate();
                break;
        }
        appendWhere();
        appendGroupBy();
        appendOrderBy();
        appendLimit(platform.limitDefinition());
        appendSetQuery();
        return qb.toString();
    }

    private void forceOrderBy(LimitDefinition limitSupport) {
        if (limitSupport.requireOrderBy() && query.getLimit() != null &&
            (query.orderByExpressions() == null || query.orderByExpressions().isEmpty())) {

            Set<Type<?>> types = query.entityTypes();
            if (types != null && !types.isEmpty()) {
                Type<?> type = types.iterator().next();
                for (Attribute attribute : type.attributes()) {
                    if (attribute.isKey()) {
                        query.orderBy((Expression) attribute);
                        break;
                    }
                }
            }
        }
    }

    private void appendSelect() {
        qb.keyword(SELECT);
        if (query.isDistinct()) {
            qb.keyword(DISTINCT);
        }
        Set<? extends Expression<?>> selection = query.selection();
        if (selection == null || selection.isEmpty()) {
            qb.append("*");
        } else {
            qb.commaSeparated(selection,
                new QueryBuilder.Appender<Expression<?>>() {
                    @Override
                    public void append(QueryBuilder qb, Expression<?> value) {
                        appendColumnForSelect(value);
                    }
                });
        }
        appendTables(FROM);
    }

    private void checkEmptyValues(Map<Expression<?>, Object> values) {
        if (values == null || values.isEmpty()) {
            throw new IllegalStateException(
                "Cannot generate insert/update statement with an empty set of values");
        }
    }

    private void appendInsert() {
        Map<Expression<?>, Object> updates = query.updateValues();
        checkEmptyValues(updates);
        qb.keyword(INSERT);
        appendTables(INTO);
        qb.openParenthesis()
        .commaSeparated(updates.entrySet(),
            new QueryBuilder.Appender<Map.Entry<Expression<?>, Object>>() {
                @Override
                public void append(QueryBuilder qb, Map.Entry<Expression<?>, Object> value) {
                    Expression<?> key = value.getKey();
                    switch (key.type()) {
                        case ATTRIBUTE:
                            Attribute attribute = (Attribute) key;
                            if (attribute.isGenerated()) {
                                throw new IllegalStateException();
                            }
                            qb.attribute(attribute);
                            break;
                        default:
                            qb.append(key.name()).space();
                            break;
                    }
                }
            })
        .closeParenthesis()
        .space()
        .keyword(VALUES)
        .openParenthesis()
        .commaSeparated(updates.entrySet(),
            new QueryBuilder.Appender<Map.Entry<Expression<?>, Object>>() {
                @Override
                public void append(QueryBuilder qb, Map.Entry<Expression<?>, Object> value) {
                    appendConditionValue(value.getKey(), value.getValue());
                }
            })
        .closeParenthesis();
    }

    private void appendUpdate() {
        Map<Expression<?>, Object> updates = query.updateValues();
        checkEmptyValues(updates);
        qb.keyword(UPDATE);
        appendTables(null);
        qb.keyword(SET);
        int index = 0;
        for (Map.Entry<Expression<?>, Object> entry : updates.entrySet()) {
            if (index > 0) {
                qb.append(",");
            }
            appendColumn(entry.getKey());
            appendOperator(Operator.EQUAL);
            appendConditionValue(entry.getKey(), entry.getValue());
            index++;
        }
    }

    private void appendDelete() {
        qb.keyword(DELETE);
        appendTables(FROM);
    }

    private void appendTruncate() {
        qb.keyword(TRUNCATE);
        appendTables(null);
    }

    private void appendSetQuery() {
        if(query.innerSetQuery() != null) {
            switch (query.setOperator()) {
                case UNION:
                    qb.keyword(UNION);
                    break;
                case UNION_ALL:
                    qb.keyword(UNION, ALL);
                    break;
                case INTERSECT:
                    qb.keyword(INTERSECT);
                    break;
                case EXCEPT:
                    qb.keyword(EXCEPT);
                    break;
            }
            mergeQuery(query.innerSetQuery());
        }
    }

    private void appendWhere() {
        ExistsElement<?> whereExists = query.whereExistsElement();
        if (whereExists != null) {
            qb.keyword(WHERE);
            appendWhereSubQuery(whereExists);
        } else if (query.whereElements() != null && query.whereElements().size() > 0) {
            qb.keyword(WHERE);
            for (WhereElement<E> w : query.whereElements()) {
                appendConditional(w);
            }
        }
    }

    private void appendGroupBy() {
        Set<Expression<?>> groupBy = query.groupByExpressions();
        if (groupBy != null && groupBy.size() > 0) {
            qb.keyword(GROUP, BY);
            qb.commaSeparated(groupBy, new QueryBuilder.Appender<Expression<?>>() {
                @Override
                public void append(QueryBuilder qb, Expression<?> value) {
                    appendColumn(value);
                }
            });
            Set<HavingElement<E>> having = query.havingElements();
            if (having != null) {
                qb.keyword(HAVING);
                for (HavingElement<E> clause : having) {
                    appendConditional(clause);
                }
            }
        }
    }

    private void appendOrderBy() {
        forceOrderBy(platform.limitDefinition());
        Set<Expression<?>> orderBy = query.orderByExpressions();
        if (orderBy != null && orderBy.size() > 0) {
            qb.keyword(ORDER, BY);
            int i = 0;
            int size = orderBy.size();
            for (Expression<?> order : orderBy) {
                appendColumn(order);
                if (order.type() == ExpressionType.ORDERING) {
                    OrderingExpression orderingExpression = (OrderingExpression) order;
                    qb.keyword(orderingExpression.getOrder() == Order.ASC ? ASC : DESC);
                    if(orderingExpression.getNullOrder() != null) {
                        qb.keyword(NULLS);
                        switch (orderingExpression.getNullOrder()) {
                            case FIRST:
                                qb.keyword(FIRST);
                                break;
                            case LAST:
                                qb.keyword(LAST);
                                break;
                        }
                    }
                }
                if (i < size - 1) {
                    qb.append(",");
                }
                i++;
            }
        }
    }

    private void appendLimit(LimitDefinition limitSupport) {
        Integer limit = query.getLimit();
        if (limit != null && limit > 0) {
            Integer offset = query.getOffset();
            if(limitSupport == null) {
                qb.keyword(LIMIT).value(limit);
                if (offset != null) {
                    qb.keyword(OFFSET).value(offset);
                }
            } else {
                limitSupport.appendLimit(qb, limit, offset);
            }
        }
    }

    private void appendTables(Keyword prefix) {
        Set<Expression<?>> from = query.fromExpressions();
        if (from.size() == 1) {
            if (prefix != null) {
                qb.value(prefix);
            }
            Expression first = from.iterator().next();
            if (first instanceof QueryWrapper) {
                appendFromExpression(first);
            } else {
                if (autoAlias) {
                    aliases.append(qb, first.name());
                } else {
                    qb.tableName(first.name());
                }
            }
        } else if (from.size() > 1) {
            if (prefix != null) {
                qb.value(prefix);
            }
            qb.openParenthesis();
            int index = 0;
            for (Expression expression : from) {
                if (index > 0) {
                    qb.comma();
                }
                appendFromExpression(expression);
                index++;
            }
            qb.closeParenthesis();
        }
        Set<JoinOnElement<E>> joins = query.joinElements();
        if (joins != null && !joins.isEmpty()) {
            for (JoinOnElement<E> join : joins) {
                appendJoin(join);
            }
        }
    }

    private void appendFromExpression(Expression expression) {
        if (expression.type() == ExpressionType.QUERY) {
            QueryWrapper wrapper = (QueryWrapper) expression;
            String alias = wrapper.unwrapQuery().aliasName();
            if (alias == null) {
                throw new IllegalStateException(
                    "query in 'from' expression must have an alias");
            }
            qb.openParenthesis();
            mergeQuery(wrapper);
            qb.closeParenthesis().space();
            qb.append(alias).space();
        } else {
            qb.append(expression.name());
        }
    }

    private static Expression<?> unwrapExpression(Expression<?> expression) {
        if (expression.type() == ExpressionType.ALIAS) {
            AliasedExpression aliasable = (AliasedExpression) expression;
            return aliasable.innerExpression();
        }
        return expression;
    }

    private String findAlias(Expression<?> expression) {
        String alias = null;
        if (expression instanceof Aliasable) {
            Aliasable aliasable = (Aliasable) expression;
            alias = aliasable.aliasName();
        }
        return alias;
    }

    private void appendColumn(Expression<?> expression) {
        String alias = findAlias(expression);
        if (expression instanceof Function) {
            appendFunction((Function) expression);
        } else if (autoAlias && expression instanceof Attribute && alias == null) {
            aliases.prefix(qb, (Attribute) expression);
        } else {
            if(alias == null || alias.length() == 0) {
                appendColumnExpression(expression);
            } else {
                qb.append(alias).space();
            }
        }
    }

    private void appendColumnForSelect(Expression<?> expression) {
        String alias = findAlias(expression);
        if (expression instanceof Function) {
            appendFunction((Function) expression);
        } else if (autoAlias) {
            if (expression instanceof Attribute) {
                aliases.prefix(qb, (Attribute) expression);
            } else {
                aliases.prefix(qb, expression);
            }
        } else {
            appendColumnExpression(expression);
        }
        if (alias != null && alias.length() > 0) {
            qb.keyword(AS);
            qb.append(alias).space();
        }
    }

    private void appendColumnExpression(Expression expression) {
        switch (expression.type()) {
            case ATTRIBUTE:
                Attribute attribute = (Attribute) expression;
                qb.attribute(attribute);
                break;
            default:
                qb.append(expression.name()).space();
                break;
        }
    }

    private void appendFunction(Function function) {
        if (function instanceof Case) {
            appendCaseFunction((Case) function);
        } else {
            String name = function.name();
            qb.append(name);
            qb.openParenthesis();
            int index = 0;
            for (Object arg : function.arguments()) {
                if (index > 0) {
                    qb.comma();
                }
                if (arg instanceof Expression) {
                    Expression expression = (Expression) arg;
                    switch (expression.type()) {
                        case ATTRIBUTE:
                            appendColumnForSelect(expression);
                            break;
                        case FUNCTION:
                            Function inner = (Function) arg;
                            appendFunction(inner);
                            break;
                        default:
                            qb.append(expression.name());
                            break;
                    }
                } else if (arg instanceof Class) {
                    qb.append("*");
                } else {
                    appendConditionValue(function.expressionForArgument(index), arg);
                }
                index++;
            }
            qb.closeParenthesis().space();
        }
    }

    private void appendCaseFunction(Case<?> function) {
        qb.keyword(CASE);
        for (Case.CaseCondition<?,?> condition : function.conditions()) {
            qb.keyword(WHEN);
            appendOperation(condition.condition());
            qb.keyword(THEN);
            // TODO just some databases need the value inline in a case statement
            if (condition.thenValue() instanceof CharSequence ||
                condition.thenValue() instanceof Number) {
                appendConditionValue(function, condition.thenValue(), false);
            } else {
                appendConditionValue(function, condition.thenValue());
            }
        }
        if (function.elseValue() != null) {
            qb.keyword(ELSE);
            appendConditionValue(function, function.elseValue());
        }
        qb.keyword(END);
    }

    private void appendJoin(JoinOnElement<E> join) {
        switch (join.joinType()) {
            case INNER:
                qb.keyword(INNER, JOIN);
                break;
            case LEFT:
                qb.keyword(LEFT, JOIN);
                break;
            case RIGHT:
                qb.keyword(RIGHT, JOIN);
                break;
        }
        if (join.tableName() != null) {
            if (autoAlias) {
                aliases.append(qb, join.tableName());
            } else {
                qb.tableName(join.tableName());
            }
        } else if (join.subQuery() != null) {
            qb.openParenthesis();
            mergeQuery((QueryWrapper<?>) join.subQuery());
            qb.closeParenthesis().space();
            if (join.subQuery().aliasName() != null) {
                qb.append(join.subQuery().aliasName()).space();
            }
        }
        qb.keyword(ON);
        for (JoinElement<E> where : join.conditions()) {
            appendConditional(where);
        }
    }

    private void appendOperation(Condition condition) {
        Object leftOperand = condition.leftOperand();
        if (leftOperand instanceof Expression) {
            final Expression<?> expression = (Expression<?>) condition.leftOperand();
            appendColumn(expression);
            Object value = condition.rightOperand();
            appendOperator(condition.operator());

            if (value instanceof Collection) {
                Collection collection = (Collection) value;
                qb.openParenthesis();
                qb.commaSeparated(collection, new QueryBuilder.Appender() {
                    @Override
                    public void append(QueryBuilder qb, Object value) {
                        appendConditionValue(expression, value);
                    }
                });
                qb.closeParenthesis();
            } else if (value instanceof Object[]) {
                Object[] values = (Object[]) value;
                if (condition.operator() == Operator.BETWEEN) {
                    Object begin = values[0];
                    Object end = values[1];
                    appendConditionValue(expression, begin);
                    qb.keyword(AND);
                    appendConditionValue(expression, end);
                } else {
                    for (Object o : values) {
                        appendConditionValue(expression, o);
                    }
                }
            } else if (value instanceof QueryWrapper) {
                QueryWrapper wrapper = (QueryWrapper) value;
                qb.openParenthesis();
                mergeQuery(wrapper);
                qb.closeParenthesis().space();
            } else if (value instanceof Condition) {
                appendOperation((Condition) value);
            } else if (value != null) {
                appendConditionValue(expression, value);
            }
        } else if(leftOperand instanceof Condition) {
            appendOperation((Condition) leftOperand);
            appendOperator(condition.operator());
            Object value = condition.rightOperand();
            if (value instanceof Condition) {
                appendOperation((Condition) value);
            } else {
                throw new IllegalStateException();
            }
        } else {
            throw new IllegalStateException("unknown start expression type " + leftOperand);
        }
    }

    private void appendConditionValue(Expression expression, Object value) {
        appendConditionValue(expression, value, true);
    }

    private void appendConditionValue(Expression expression, Object value, boolean parameterize) {
        if (value instanceof QueryAttribute) {
            QueryAttribute a = (QueryAttribute) value;
            appendColumn(a);
        } else if (value instanceof NamedExpression) {
            NamedExpression namedExpression = (NamedExpression) value;
            qb.append(namedExpression.name());
        } else {
            if (parameterize) {
                if (parameters != null) {
                    parameters.add(expression, value);
                }
                qb.append("?").space();
            } else {
                if (value instanceof CharSequence) {
                    qb.appendQuoted(value.toString()).space();
                } else {
                    qb.append(value).space();
                }
            }
        }
    }

    private void appendConditional(LogicalElement element) {
        LogicalOperator op = element.operator();
        if (op != null) {
            switch (op) {
                case AND:
                    qb.keyword(AND);
                    break;
                case OR:
                    qb.keyword(OR);
                    break;
            }
        }
        Condition condition = element.condition();
        boolean nested = false;
        if (condition.rightOperand() instanceof Condition) {
            nested = true;
        }
        if (nested) {
            qb.openParenthesis();
        }
        appendOperation(condition);
        if (nested) {
            qb.closeParenthesis().space();
        }
    }

    private void appendWhereSubQuery(ExistsElement<?> subQuery) {
        if (subQuery.isNotExists()) {
            qb.keyword(NOT);
        }
        qb.keyword(EXISTS);
        qb.openParenthesis();
        Supplier<?> query = subQuery.getQuery();
        mergeQuery((QueryWrapper) query);
        qb.closeParenthesis().space();
    }

    private void appendOperator(Operator operator) {
        switch (operator) {
            case EQUAL:
                qb.value("=");
                break;
            case NOT_EQUAL:
                qb.value("!=");
                break;
            case LESS_THAN:
                qb.value("<");
                break;
            case LESS_THAN_OR_EQUAL:
                qb.value("<=");
                break;
            case GREATER_THAN:
                qb.value(">");
                break;
            case GREATER_THAN_OR_EQUAL:
                qb.value(">=");
                break;
            case IN:
                qb.keyword(IN);
                break;
            case NOT_IN:
                qb.keyword(NOT, IN);
                break;
            case LIKE:
                qb.keyword(LIKE);
                break;
            case NOT_LIKE:
                qb.keyword(NOT, LIKE);
                break;
            case BETWEEN:
                qb.keyword(BETWEEN);
                break;
            case NULL:
                qb.keyword(IS, NULL);
                break;
            case NOT_NULL:
                qb.keyword(IS, NOT, NULL);
                break;
            case AND:
                qb.keyword(AND);
                break;
            case OR:
                qb.keyword(OR);
                break;
        }
    }

    private void mergeQuery(QueryWrapper<?> wrapper) {
        QueryElement<?> query = wrapper.unwrapQuery();
        QueryGenerator generator = new QueryGenerator<>(query, aliases);
        generator.toSql(qb, platform);
        if (parameters != null) {
            parameters.addAll(generator.parameters());
        }
    }

    private static class Aliases {

        private final Map<String, String> aliases = new HashMap<>();
        private char index = 'a';

        private String alias(String key) {
            String alias = aliases.get(key);
            if (alias == null) {
                if (index > 'z') {
                    throw new IllegalStateException();
                }
                aliases.put(key, alias = String.valueOf(index));
                index++;
            }
            return alias;
        }

        void append(QueryBuilder qb, String table) {
            String key = table.replaceAll("\"", "");
            String alias = alias(key);
            qb.tableName(table).value(alias);
        }

        void prefix(QueryBuilder qb, Attribute attribute) {
            String key = attribute.declaringType().name();
            String alias = alias(key);
            qb.append(alias + ".").attribute(attribute);
        }

        void prefix(QueryBuilder qb, Expression expression) {
            Expression inner = unwrapExpression(expression);
            String key = inner.name();
            if(inner.type() == ExpressionType.ATTRIBUTE) {
                Attribute attribute = (Attribute) inner;
                key = attribute.declaringType().name();
            }
            String alias = alias(key);
            qb.append(alias + "." + expression.name()).space();
        }
    }
}
