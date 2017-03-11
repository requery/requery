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

package io.requery.sql.platform;

import io.requery.meta.Attribute;
import io.requery.query.Expression;
import io.requery.query.function.Function;
import io.requery.query.function.Random;
import io.requery.sql.AutoIncrementColumnDefinition;
import io.requery.sql.GeneratedColumnDefinition;
import io.requery.sql.Mapping;
import io.requery.sql.QueryBuilder;
import io.requery.sql.gen.Generator;
import io.requery.sql.gen.LimitGenerator;
import io.requery.sql.gen.Output;

import java.util.Map;

import static io.requery.sql.Keyword.DUPLICATE;
import static io.requery.sql.Keyword.INSERT;
import static io.requery.sql.Keyword.INTO;
import static io.requery.sql.Keyword.KEY;
import static io.requery.sql.Keyword.ON;
import static io.requery.sql.Keyword.UPDATE;
import static io.requery.sql.Keyword.VALUES;

/**
 * MySQL SQL/PSM (5+).
 */
public class MySQL extends Generic {

    private final AutoIncrementColumnDefinition autoIncrementColumn;

    public MySQL() {
        autoIncrementColumn = new AutoIncrementColumnDefinition();
    }

    @Override
    public void addMappings(Mapping mapping) {
        mapping.aliasFunction(new Function.Name("rand"), Random.class);
    }

    @Override
    public boolean supportsGeneratedKeysInBatchUpdate() {
        return true;
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
        return new UpsertOnDuplicateKeyUpdate();
    }

    /**
     * Performs an upsert (insert/update) using insert on duplicate key update syntax.
     */
    private static class UpsertOnDuplicateKeyUpdate implements
        Generator<Map<Expression<?>, Object>> {

        @Override
        public void write(final Output output, final Map<Expression<?>, Object> values) {
            QueryBuilder qb = output.builder();
            // insert into <table> (<columns>) values (<values)
            // on duplicate key update (<column>=VALUES(<value>...
            // insert fragment
            qb.keyword(INSERT, INTO)
                .tableNames(values.keySet())
                .openParenthesis()
                .commaSeparatedExpressions(values.keySet())
                .closeParenthesis().space()
                .keyword(VALUES)
                .openParenthesis()
                .commaSeparated(values.keySet(), new QueryBuilder.Appender<Expression<?>>() {
                    @Override
                    public void append(QueryBuilder qb, Expression expression) {
                        qb.append("?");
                        output.parameters().add(expression, values.get(expression));
                    }
                })
                .closeParenthesis().space()
                .keyword(ON, DUPLICATE, KEY, UPDATE)
                .commaSeparated(values.keySet(), new QueryBuilder.Appender<Expression<?>>() {
                    @Override
                    public void append(QueryBuilder qb, Expression<?> value) {
                        qb.attribute((Attribute) value)
                            .append("=")
                            .append("values")
                            .openParenthesis()
                            .attribute((Attribute) value)
                            .closeParenthesis().space();
                    }
                });
        }
    }
}
