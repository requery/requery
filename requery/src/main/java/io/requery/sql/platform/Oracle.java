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
import io.requery.sql.BaseType;
import io.requery.sql.GeneratedColumnDefinition;
import io.requery.sql.IdentityColumnDefinition;
import io.requery.sql.Mapping;
import io.requery.sql.QueryBuilder;
import io.requery.sql.UpsertDefinition;
import io.requery.sql.UpsertMergeDefinition;
import io.requery.sql.type.PrimitiveBooleanType;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import static io.requery.sql.Keyword.ALWAYS;
import static io.requery.sql.Keyword.AS;
import static io.requery.sql.Keyword.BY;
import static io.requery.sql.Keyword.FROM;
import static io.requery.sql.Keyword.GENERATED;
import static io.requery.sql.Keyword.IDENTITY;
import static io.requery.sql.Keyword.INCREMENT;
import static io.requery.sql.Keyword.SELECT;
import static io.requery.sql.Keyword.START;
import static io.requery.sql.Keyword.WITH;

/**
 * Oracle 12c and later PL/SQL.
 */
public class Oracle extends Generic {

    private final OracleIdentityColumnDefinition generatedColumn;
    private final UpsertMergeDefinition upsertMergeDefinition;

    public Oracle() {
        generatedColumn = new OracleIdentityColumnDefinition();
        upsertMergeDefinition = new UpsertMergeDual();
    }

    @Override
    public void addMappings(Mapping mapping) {
        super.addMappings(mapping);
        mapping.replaceType(Types.BINARY, new RawType(Types.BINARY));
        mapping.replaceType(Types.VARBINARY, new RawType(Types.VARBINARY));
        mapping.replaceType(Types.BOOLEAN, new NumericBooleanType());
    }

    @Override
    public boolean supportsIfExists() {
        return false;
    }

    @Override
    public GeneratedColumnDefinition generatedColumnDefinition() {
        return generatedColumn;
    }

    @Override
    public UpsertDefinition upsertDefinition() {
        return upsertMergeDefinition;
    }

    private static class UpsertMergeDual extends UpsertMergeDefinition {
        @Override
        protected <E> void appendUsing(QueryBuilder qb,
                                       Iterable<Attribute<E, ?>> attributes,
                                       final Parameterizer<E> parameterizer) {
            qb.openParenthesis()
                .keyword(SELECT)
                .commaSeparated(attributes, new QueryBuilder.Appender<Attribute<E, ?>>() {
                    @Override
                    public void append(QueryBuilder qb, Attribute<E, ?> value) {
                        qb.append("? ");
                        parameterizer.addParameter(value);
                        qb.append(value.name());
                    }
                }).space()
                .keyword(FROM)
                .append("DUAL ")
                .closeParenthesis()
                .append(" " + alias + " ");
        }
    }
    // binary type
    private static class RawType extends BaseType<byte[]> {

        RawType(int jdbcType) {
            super(byte[].class, jdbcType);
        }

        @Override
        public boolean hasLength() {
            return sqlType() == Types.VARBINARY;
        }

        @Override
        public String identifier() {
            return "raw";
        }

        @Override
        public byte[] read(ResultSet results, int column) throws SQLException {
            byte[] value = results.getBytes(column);
            if (results.wasNull()) {
                return null;
            }
            return value;
        }
    }

    // no boolean support
    private static class NumericBooleanType extends BaseType<Boolean>
        implements PrimitiveBooleanType {

        NumericBooleanType() {
            super(Boolean.class, Types.NUMERIC);
        }

        @Override
        public boolean hasLength() {
            return true;
        }

        @Override
        public Integer defaultLength() {
            return 1;
        }

        @Override
        public String identifier() {
            return "number";
        }

        @Override
        public Boolean read(ResultSet results, int column) throws SQLException {
            Boolean value = results.getBoolean(column);
            if (results.wasNull()) {
                return null;
            }
            return value;
        }

        @Override
        public boolean readBoolean(ResultSet results, int column) throws SQLException {
            return results.getBoolean(column);
        }

        @Override
        public void writeBoolean(PreparedStatement statement, int index, boolean value)
            throws SQLException {
            statement.setBoolean(index, value);
        }
    }

    private static class OracleIdentityColumnDefinition extends
        IdentityColumnDefinition {

        @Override
        public void appendGeneratedSequence(QueryBuilder qb, Attribute attribute) {
            int start = 1;
            int increment = 1;
            qb.keyword(GENERATED, ALWAYS, AS, IDENTITY);
            qb.openParenthesis()
                .keyword(START, WITH).value(start)
                //.comma() just off the standard...
                .keyword(INCREMENT, BY).value(increment)
                .closeParenthesis()
                .space();
        }
    }

}
