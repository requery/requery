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
import io.requery.sql.AutoIncrementColumnDefinition;
import io.requery.sql.GeneratedColumnDefinition;
import io.requery.sql.LimitDefinition;
import io.requery.sql.LimitOffsetDefinition;
import io.requery.sql.QueryBuilder;
import io.requery.sql.UpsertDefinition;

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
    private final LimitDefinition limitDefinition;
    private final UpsertDefinition upsertDefinition;

    public MySQL() {
        autoIncrementColumn = new AutoIncrementColumnDefinition();
        limitDefinition = new LimitOffsetDefinition();
        upsertDefinition = new UpsertOnDuplicateKeyUpdate();
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
    public LimitDefinition limitDefinition() {
        return limitDefinition;
    }

    @Override
    public UpsertDefinition upsertDefinition() {
        return upsertDefinition;
    }

    /**
     * Performs an upsert (insert/update) using insert on duplicate key update syntax.
     */
    private static class UpsertOnDuplicateKeyUpdate implements UpsertDefinition {

        @Override
        public <E> void appendUpsert(QueryBuilder qb,
                                     Iterable<Attribute<E, ?>> attributes,
                                     final Parameterizer<E> parameterizer) {
            Type<E> type = attributes.iterator().next().declaringType();
            // insert into <table> (<columns>) values (<values)
            // on duplicate key update (<column>=VALUES(<value>...
            // insert fragment
            qb.keyword(INSERT, INTO)
                .tableName(type.name())
                .openParenthesis()
                .commaSeparatedAttributes(attributes)
                .closeParenthesis().space()
                .keyword(VALUES)
                .openParenthesis()
                .commaSeparated(attributes, new QueryBuilder.Appender<Attribute<E, ?>>() {
                    @Override
                    public void append(QueryBuilder qb, Attribute<E, ?> value) {
                        qb.append("?");
                        parameterizer.addParameter(value);
                    }
                })
                .closeParenthesis().space()
                .keyword(ON, DUPLICATE, KEY, UPDATE)
                .commaSeparated(attributes, new QueryBuilder.Appender<Attribute<E, ?>>() {
                    @Override
                    public void append(QueryBuilder qb, Attribute<E, ?> value) {
                        qb.attribute(value)
                            .append("=")
                            .append("values")
                            .openParenthesis()
                            .attribute(value)
                            .closeParenthesis().space();
                    }
                });
        }
    }
}
