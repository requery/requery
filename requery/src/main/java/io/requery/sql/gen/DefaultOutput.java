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

package io.requery.sql.gen;

import io.requery.meta.Attribute;
import io.requery.meta.QueryAttribute;
import io.requery.query.Aliasable;
import io.requery.query.AliasedExpression;
import io.requery.query.Condition;
import io.requery.query.Expression;
import io.requery.query.ExpressionType;
import io.requery.query.NamedExpression;
import io.requery.query.Operator;
import io.requery.query.OrderingExpression;
import io.requery.query.element.JoinConditionElement;
import io.requery.query.element.JoinOnElement;
import io.requery.query.element.LogicalElement;
import io.requery.query.element.LogicalOperator;
import io.requery.query.element.QueryElement;
import io.requery.query.element.QueryWrapper;
import io.requery.query.function.Case;
import io.requery.query.function.Function;
import io.requery.sql.BoundParameters;
import io.requery.sql.QueryBuilder;
import io.requery.sql.RuntimeConfiguration;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static io.requery.sql.Keyword.*;

/**
 * Generates a parameterizable SQL statement from a given query.
 *
 * @author Nikhil Purushe
 */
public class DefaultOutput implements Output {

    private final QueryElement<?> query;
    private final Aliases inheritedAliases;
    private final boolean parameterize;
    private final BoundParameters parameters;
    private final StatementGenerator statementGenerator;
    private final QueryBuilder qb;
    private Aliases aliases;
    private boolean autoAlias;

    public DefaultOutput(RuntimeConfiguration configuration, QueryElement<?> query) {
        this(configuration.getStatementGenerator(), query,
            new QueryBuilder(configuration.getQueryBuilderOptions()), null, true);
    }

    public DefaultOutput(StatementGenerator statementGenerator, QueryElement<?> query,
                         QueryBuilder qb, Aliases inherited, boolean parameterize) {
        this.query = query;
        this.qb = qb;
        this.inheritedAliases = inherited;
        this.parameterize = parameterize;
        this.statementGenerator = statementGenerator;
        this.parameters = parameterize? new BoundParameters() : null;
    }

    @Override
    public QueryBuilder builder() {
        return qb;
    }

    @Override
    public BoundParameters parameters() {
        return parameters;
    }

    public String toSql() {
        aliases = inheritedAliases == null ? new Aliases() : inheritedAliases;
        Set<Expression<?>> from = query.fromExpressions();
        Set<?> joins = query.joinElements();
        autoAlias = from.size() > 1 || (joins != null && joins.size() > 0);
        statementGenerator.write(this, query);
        return qb.toString();
    }

    @Override
    public void appendTables() {
        Set<Expression<?>> from = query.fromExpressions();
        if (from.size() == 1) {
            Expression first = from.iterator().next();
            if (first instanceof QueryWrapper) {
                appendFromExpression(first);
            } else {
                if (autoAlias) {
                    aliases.append(qb, first.getName());
                } else {
                    qb.tableName(first.getName());
                }
            }
        } else if (from.size() > 1) {
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
        appendJoins();
    }

    private void appendJoins() {
        if (query.joinElements() != null && !query.joinElements().isEmpty()) {
            for (JoinOnElement<?> join : query.joinElements()) {
                appendJoin(join);
            }
        }
    }

    private void appendFromExpression(Expression expression) {
        if (expression.getExpressionType() == ExpressionType.QUERY) {
            QueryWrapper wrapper = (QueryWrapper) expression;
            String alias = wrapper.unwrapQuery().getAlias();
            if (alias == null) {
                throw new IllegalStateException(
                    "query in 'from' expression must have an alias");
            }
            qb.openParenthesis();
            appendQuery(wrapper);
            qb.closeParenthesis().space();
            qb.append(alias).space();
        } else {
            qb.append(expression.getName());
        }
    }

    private static Expression<?> unwrapExpression(Expression<?> expression) {
        if (expression.getExpressionType() == ExpressionType.ALIAS) {
            AliasedExpression aliased = (AliasedExpression) expression;
            return aliased.getInnerExpression();
        } else if (expression.getExpressionType() == ExpressionType.ORDERING) {
            OrderingExpression ordering = (OrderingExpression) expression;
            return ordering.getInnerExpression();
        }
        return expression;
    }

    private String findAlias(Expression<?> expression) {
        String alias = null;
        if (expression instanceof Aliasable) {
            Aliasable aliasable = (Aliasable) expression;
            alias = aliasable.getAlias();
        }
        return alias;
    }

    @Override
    public void appendColumn(Expression<?> expression) {
        String alias = findAlias(expression);
        if (expression instanceof Function) {
            appendFunction((Function) expression);
        } else if (autoAlias && alias == null) {
            aliases.prefix(qb, expression);
        } else {
            if(alias == null || alias.length() == 0) {
                appendColumnExpression(expression);
            } else {
                qb.append(alias).space();
            }
        }
    }

    @Override
    public void appendColumnForSelect(Expression<?> expression) {
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
        switch (expression.getExpressionType()) {
            case ATTRIBUTE:
                Attribute attribute = (Attribute) expression;
                qb.attribute(attribute);
                break;
            default:
                qb.append(expression.getName()).space();
                break;
        }
    }

    private void appendFunction(Function function) {
        if (function instanceof Case) {
            appendCaseFunction((Case) function);
        } else {
            String name = function.getName();
            qb.append(name);
            qb.openParenthesis();
            int index = 0;
            for (Object arg : function.arguments()) {
                if (index > 0) {
                    qb.comma();
                }
                if (arg instanceof Expression) {
                    Expression expression = (Expression) arg;
                    switch (expression.getExpressionType()) {
                        case ATTRIBUTE:
                            appendColumnForSelect(expression);
                            break;
                        case FUNCTION:
                            Function inner = (Function) arg;
                            appendFunction(inner);
                            break;
                        default:
                            qb.append(expression.getName());
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
            appendOperation(condition.condition(), 0);
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

    private void appendJoin(JoinOnElement<?> join) {
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
            appendQuery((QueryWrapper<?>) join.subQuery());
            qb.closeParenthesis().space();
            if (join.subQuery().getAlias() != null) {
                qb.append(join.subQuery().getAlias()).space();
            }
        }
        qb.keyword(ON);
        for (JoinConditionElement<?> where : join.conditions()) {
            appendConditional(where);
        }
    }

    private void appendOperation(Condition condition, int depth) {
        Object leftOperand = condition.getLeftOperand();
        if (leftOperand instanceof Expression) {
            final Expression<?> expression = (Expression<?>) condition.getLeftOperand();
            appendColumn(expression);
            Object value = condition.getRightOperand();
            appendOperator(condition.getOperator());

            if (value instanceof Collection &&
                    (condition.getOperator() == Operator.IN ||
                     condition.getOperator() == Operator.NOT_IN)) {
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
                if (condition.getOperator() == Operator.BETWEEN) {
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
                appendQuery(wrapper);
                qb.closeParenthesis().space();
            } else if (value instanceof Condition) {
                appendOperation((Condition) value, depth + 1);
            } else if (value != null) {
                appendConditionValue(expression, value);
            }
        } else if(leftOperand instanceof Condition) {
            if (depth > 0) {
                qb.openParenthesis();
            }
            appendOperation((Condition) leftOperand, depth + 1);
            appendOperator(condition.getOperator());
            Object value = condition.getRightOperand();
            if (value instanceof Condition) {
                appendOperation((Condition) value, depth + 1);
            } else {
                throw new IllegalStateException();
            }
            if (depth > 0) {
                qb.closeParenthesis().space();
            }
        } else {
            throw new IllegalStateException("unknown start expression type " + leftOperand);
        }
    }

    @Override
    public void appendConditionValue(Expression expression, Object value) {
        appendConditionValue(expression, value, true);
    }

    private void appendConditionValue(Expression expression, Object value, boolean parameterize) {
        if (value instanceof QueryAttribute) {
            QueryAttribute a = (QueryAttribute) value;
            appendColumn(a);
        } else if (value instanceof NamedExpression) {
            NamedExpression namedExpression = (NamedExpression) value;
            qb.append(namedExpression.getName());
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

    @Override
    public void appendConditional(LogicalElement element) {
        LogicalOperator op = element.getOperator();
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
        Condition condition = element.getCondition();
        boolean nested = condition.getRightOperand() instanceof Condition;
        if (nested) {
            qb.openParenthesis();
        }
        appendOperation(condition, 0);
        if (nested) {
            qb.closeParenthesis().space();
        }
    }

    @Override
    public void appendOperator(Operator operator) {
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
            case IS_NULL:
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

    @Override
    public void appendQuery(QueryWrapper<?> wrapper) {
        QueryElement<?> query = wrapper.unwrapQuery();
        DefaultOutput generator =
            new DefaultOutput(statementGenerator, query, qb, aliases, parameterize);
        generator.toSql();
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
            String key = attribute.getDeclaringType().getName();
            String alias = alias(key);
            qb.append(alias + ".").attribute(attribute);
        }

        void prefix(QueryBuilder qb, Expression expression) {
            Expression inner = unwrapExpression(expression);
            String key = inner.getName();
            if(inner.getExpressionType() == ExpressionType.ATTRIBUTE) {
                Attribute attribute = (Attribute) inner;
                key = attribute.getDeclaringType().getName();
            }
            String alias = alias(key);
            qb.append(alias + "." + expression.getName()).space();
        }
    }
}
