/*
 * Copyright 2017 requery.io
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
import io.requery.query.Expression;
import io.requery.query.element.InsertType;
import io.requery.query.element.QueryElement;
import io.requery.sql.QueryBuilder;

import java.util.Map;

import static io.requery.sql.Keyword.DEFAULT;
import static io.requery.sql.Keyword.INSERT;
import static io.requery.sql.Keyword.INTO;
import static io.requery.sql.Keyword.VALUES;

public class InsertGenerator implements Generator<QueryElement<?>> {

    @Override
    public void write(final Output output, QueryElement<?> query) {
        Map<Expression<?>, Object> values = query.updateValues();
        InsertType insertType = query.insertType();
        QueryBuilder qb = output.builder();
        qb.keyword(INSERT, INTO);
        output.appendTables();

        if (values.isEmpty()) {
            if (insertType == InsertType.VALUES) {
                qb.keyword(DEFAULT, VALUES);
            }
        } else {
            qb.openParenthesis()
                .commaSeparated(values.entrySet(),
                new QueryBuilder.Appender<Map.Entry<Expression<?>, Object>>() {
                    @Override
                    public void append(QueryBuilder qb, Map.Entry<Expression<?>, Object> value) {
                        Expression<?> key = value.getKey();
                        switch (key.getExpressionType()) {
                            case ATTRIBUTE:
                                Attribute attribute = (Attribute) key;
                                if (attribute.isGenerated()) {
                                    throw new IllegalStateException();
                                }
                                qb.attribute(attribute);
                                break;
                            default:
                                qb.append(key.getName()).space();
                                break;
                        }
                    }
                })
                .closeParenthesis()
                .space();

            if (insertType == InsertType.VALUES) {
                qb.keyword(VALUES)
                    .openParenthesis()
                    .commaSeparated(values.entrySet(),
                        new QueryBuilder.Appender<Map.Entry<Expression<?>, Object>>() {
                            @Override
                            public void append(QueryBuilder qb, Map.Entry<Expression<?>, Object> value) {
                                output.appendConditionValue(value.getKey(), value.getValue());
                            }
                        })
                    .closeParenthesis();
            } else {
                output.appendQuery(query.subQuery());
            }
        }
    }
}
