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
import io.requery.meta.Type;
import io.requery.query.Expression;
import io.requery.query.ExpressionType;
import io.requery.sql.Keyword;
import io.requery.sql.QueryBuilder;

import java.util.LinkedHashSet;
import java.util.Map;

import static io.requery.sql.Keyword.*;

/**
 * Performs an upsert (insert/update) using the SQL standard MERGE statement. This is the most
 * portable type of upsert.
 *
 * @author Nikhil Purushe
 */
public class UpsertMergeGenerator implements Generator<Map<Expression<?>, Object>> {

    protected final String alias = "val";

    @Override
    public void write(Output output, Map<Expression<?>, Object> values) {
        QueryBuilder qb = output.builder();
        // TODO only supporting 1 type for now
        Type<?> type = null;
        for (Expression<?> expression : values.keySet()) {
            if (expression.getExpressionType() == ExpressionType.ATTRIBUTE) {
                Attribute attribute = (Attribute) expression;
                type = attribute.getDeclaringType();
                break;
            }
        }
        if (type == null) {
            throw new IllegalStateException();
        }
        qb.keyword(MERGE).keyword(INTO)
            .tableName(type.getName())
            .keyword(USING);
            appendUsing(output, values);
            qb.keyword(ON)
            .openParenthesis();
        int count = 0;
        for (Attribute<?, ?> attribute : type.getKeyAttributes()) {
            if (count > 0) {
                qb.keyword(Keyword.AND);
            }
            qb.aliasAttribute(type.getName(), attribute);
            qb.append(" = ");
            qb.aliasAttribute(alias, attribute);
            count++;
        }
        qb.closeParenthesis().space();
        // update fragment
        LinkedHashSet<Attribute<?, ?>> updates = new LinkedHashSet<>();
        for (Expression<?> expression : values.keySet()) {
            if (expression.getExpressionType() == ExpressionType.ATTRIBUTE) {
                Attribute attribute = (Attribute) expression;
                if (!attribute.isKey()) {
                    updates.add(attribute);
                }
            }
        }
        qb.keyword(WHEN, MATCHED, THEN, UPDATE, SET)
            .commaSeparated(updates, new QueryBuilder.Appender<Attribute<?, ?>>() {
                @Override
                public void append(QueryBuilder qb, Attribute<?, ?> value) {
                    qb.attribute(value);
                    qb.append(" = " + alias + "." + value.getName());
                }
            }).space();
        // insert fragment
        qb.keyword(WHEN, NOT, MATCHED, THEN, INSERT)
            .openParenthesis()
            .commaSeparatedExpressions(values.keySet())
            .closeParenthesis().space()
            .keyword(VALUES)
            .openParenthesis()
            .commaSeparated(values.keySet(), new QueryBuilder.Appender<Expression<?>>() {
                @Override
                public void append(QueryBuilder qb, Expression<?> value) {
                    qb.aliasAttribute(alias, (Attribute) value);
                }
            })
            .closeParenthesis();
    }

    protected void appendUsing(final Output writer, final Map<Expression<?>, Object> values) {
        QueryBuilder qb = writer.builder();
        qb.openParenthesis()
            .keyword(VALUES).openParenthesis()
            .commaSeparated(values.keySet(), new QueryBuilder.Appender<Expression>() {
                @Override
                public void append(QueryBuilder qb, Expression expression) {
                    qb.append("?");
                    writer.parameters().add(expression, values.get(expression));
                }
            }).closeParenthesis()
            .closeParenthesis().space()
            .keyword(AS)
            .append(alias)
            .openParenthesis()
            .commaSeparatedExpressions(values.keySet())
            .closeParenthesis().space();
    }
}
