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

package io.requery.sql.platform;

import io.requery.meta.Attribute;
import io.requery.meta.Type;
import io.requery.query.Expression;
import io.requery.sql.AutoIncrementColumnDefinition;
import io.requery.sql.GeneratedColumnDefinition;
import io.requery.sql.QueryBuilder;
import io.requery.sql.gen.Generator;
import io.requery.sql.gen.LimitGenerator;
import io.requery.sql.gen.Output;

import java.util.Map;

import static io.requery.sql.Keyword.FROM;
import static io.requery.sql.Keyword.INTO;
import static io.requery.sql.Keyword.KEY;
import static io.requery.sql.Keyword.MERGE;
import static io.requery.sql.Keyword.SELECT;

/**
 * H2 Database.
 */
public class H2 extends Generic {

    private final AutoIncrementColumnDefinition autoIncrementColumn;

    public H2() {
        autoIncrementColumn = new AutoIncrementColumnDefinition();
    }

    @Override
    public GeneratedColumnDefinition generatedColumnDefinition() {
        return autoIncrementColumn;
    }

    @Override
    public LimitGenerator limitGenerator() {
        return new LimitGenerator();
    }

    @Override
    public Generator<Map<Expression<?>, Object>> upsertGenerator() {
        return new UpsertMergeDual();
    }

    private static class UpsertMergeDual implements Generator<Map<Expression<?>, Object>> {

        @Override
        public void write(final Output output, final Map<Expression<?>, Object> values) {
            QueryBuilder qb = output.builder();
            Type<?> type = ((Attribute) values.keySet().iterator().next()).getDeclaringType();
            qb.keyword(MERGE).keyword(INTO)
                .tableNames(values.keySet())
                .openParenthesis()
                .commaSeparatedExpressions(values.keySet())
                .closeParenthesis().space()
                .keyword(KEY)
                .openParenthesis()
                .commaSeparatedAttributes(type.getKeyAttributes())
                .closeParenthesis().space()
                .keyword(SELECT)
                .commaSeparated(values.keySet(), new QueryBuilder.Appender<Expression<?>>() {
                    @Override
                    public void append(QueryBuilder qb, Expression expression) {
                        qb.append("?");
                        output.parameters().add(expression, values.get(expression));
                    }
                }).space().keyword(FROM).append("DUAL");
        }
    }
}
