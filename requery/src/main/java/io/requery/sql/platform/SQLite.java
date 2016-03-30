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

import io.requery.ReferentialAction;
import io.requery.meta.Attribute;
import io.requery.meta.Type;
import io.requery.sql.AutoIncrementColumnDefinition;
import io.requery.sql.BasicType;
import io.requery.sql.GeneratedColumnDefinition;
import io.requery.sql.Keyword;
import io.requery.sql.LimitDefinition;
import io.requery.sql.LimitOffsetDefinition;
import io.requery.sql.Mapping;
import io.requery.sql.QueryBuilder;
import io.requery.sql.UpsertDefinition;
import io.requery.sql.type.PrimitiveLongType;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import static io.requery.sql.Keyword.*;

/**
 * SQLite. (3.6 and later)
 */
public class SQLite extends Generic {

    private final AutoIncrementColumnDefinition autoIncrementColumn;
    private final LimitDefinition limitDefinition;
    private final UpsertDefinition upsertDefinition;

    public SQLite() {
        autoIncrementColumn = new AutoIncrementColumnDefinition("autoincrement");
        limitDefinition = new LimitOffsetDefinition();
        upsertDefinition = new InsertOrReplace();
    }

    @Override
    public boolean supportsGeneratedKeysInBatchUpdate() {
        return false;
    }

    @Override
    public boolean supportsAddingConstraint() {
        return false;
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

    @Override
    public boolean supportsUpsert() {
        return false;
    }

    @Override
    public void addMappings(Mapping mapping) {
        super.addMappings(mapping);
        mapping.putType(long.class, new LongType(long.class));
        mapping.putType(Long.class, new LongType(Long.class));
    }

    // in SQLite BIGINT can be treated as just an integer, this handles the case when an long is
    // used as generated key
    private static class LongType extends BasicType<Long> implements PrimitiveLongType {

        LongType(Class<Long> type) {
            super(type, Types.INTEGER);
        }

        @Override
        public Long fromResult(ResultSet results, int column) throws SQLException {
            return results.getLong(column);
        }

        @Override
        public Keyword identifier() {
            return INTEGER;
        }

        @Override
        public long readLong(ResultSet results, int column) throws SQLException {
            return results.getLong(column);
        }

        @Override
        public void writeLong(PreparedStatement statement, int index, long value)
            throws SQLException {
            statement.setLong(index, value);
        }
    }

    // not a real upsert since replace will delete rows however provided as an option
    protected static class InsertOrReplace implements UpsertDefinition {

        @Override
        public<E> void appendUpsert(QueryBuilder qb,
                                    Iterable<Attribute<E, ?>> attributes,
                                    final Parameterizer<E> parameterizer) {
            // insert or replace into <table> select(columns...) from
            // (select "column1" as c1...) as new left join (select ... from <table>)
            //  on prev.key = new.key
            Type<E> type = attributes.iterator().next().declaringType();
            qb.keyword(INSERT, OR, REPLACE, INTO)
                .tableName(type.name())
                .openParenthesis()
                .commaSeparated(attributes, new QueryBuilder.Appender<Attribute<E, ?>>() {
                    @Override
                    public void append(QueryBuilder qb, Attribute<E, ?> value) {
                        if (value.isForeignKey() &&
                            value.deleteAction() == ReferentialAction.CASCADE) {
                            throw new IllegalStateException("replace would cause cascade");
                        }
                        qb.attribute(value);
                    }
                })
                .closeParenthesis().space();
            final String previousAlias = "prev";
            final String newAlias = "next";
            qb.keyword(SELECT)
                .commaSeparated(attributes, new QueryBuilder.Appender<Attribute<E, ?>>() {
                    @Override
                    public void append(QueryBuilder qb, Attribute<E, ?> value) {
                        qb.aliasAttribute(newAlias, value);
                    }
                }).keyword(FROM)
                .openParenthesis()
                .keyword(SELECT)
                .commaSeparated(attributes, new QueryBuilder.Appender<Attribute<E, ?>>() {
                    @Override
                    public void append(QueryBuilder qb, Attribute<E, ?> value) {
                        qb.append("? ").keyword(AS).append(value.name());
                        parameterizer.addParameter(value);
                    }
                })
                .closeParenthesis().space()
                .keyword(AS).append(newAlias).space()
                .keyword(LEFT, JOIN)
                .openParenthesis()
                .keyword(SELECT)
                .commaSeparatedAttributes(attributes)
                .keyword(FROM)
                .tableName(type.name())
                .closeParenthesis().space().keyword(AS).append(previousAlias).space()
                .keyword(ON)
                .aliasAttribute(previousAlias, type.singleKeyAttribute())
                .append(" = ")
                .aliasAttribute(newAlias, type.singleKeyAttribute());
        }
    }
}
