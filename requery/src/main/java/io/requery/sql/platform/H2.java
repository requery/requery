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

import static io.requery.sql.Keyword.*;

/**
 * H2 Database.
 */
public class H2 extends Generic {

    private final AutoIncrementColumnDefinition autoIncrementColumn;
    private final LimitDefinition limitDefinition;
    private final UpsertDefinition upsertDefinition;

    public H2() {
        autoIncrementColumn = new AutoIncrementColumnDefinition();
        limitDefinition = new LimitOffsetDefinition();
        upsertDefinition = new UpsertMergeDualDefinition();
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

    private static class UpsertMergeDualDefinition implements UpsertDefinition {

        @Override
        public <E> void appendUpsert(QueryBuilder qb,
                                     Iterable<Attribute<E, ?>> attributes,
                                     final Parameterizer<E> parameterizer) {
            Type<E> type = attributes.iterator().next().declaringType();
            qb.keyword(MERGE).keyword(INTO)
                .tableName(type.name())
                .openParenthesis()
                .commaSeparatedAttributes(attributes)
                .closeParenthesis().space()
                .keyword(KEY)
                .openParenthesis()
                .commaSeparatedAttributes(type.keyAttributes())
                .closeParenthesis().space()
                .keyword(SELECT)
                .commaSeparated(attributes, new QueryBuilder.Appender<Attribute<E, ?>>() {
                    @Override
                    public void append(QueryBuilder qb, Attribute<E, ?> value) {
                        qb.append("?");
                        parameterizer.addParameter(value);
                    }
                }).space().keyword(FROM).append("DUAL");
        }
    }
}
