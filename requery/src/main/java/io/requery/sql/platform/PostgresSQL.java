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
import io.requery.sql.LimitDefinition;
import io.requery.sql.LimitOffsetDefinition;
import io.requery.sql.Mapping;
import io.requery.sql.QueryBuilder;
import io.requery.sql.VersionColumnDefinition;
import io.requery.sql.type.VarCharType;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.UUID;

/**
 * Platform configuration for PostgresSQL PL/pgSQL (9+)
 */
public class PostgresSQL extends Generic {

    private static class ByteArrayType extends BaseType<byte[]> {

        ByteArrayType(int jdbcType) {
            super(byte[].class, jdbcType);
        }

        @Override
        public String identifier() {
            return "bytea";
        }

        @Override
        public byte[] read(ResultSet results, int column) throws SQLException {
            byte[] value = results.getBytes(column);
            return results.wasNull() ? null : value;
        }
    }

    private static class UUIDType extends BaseType<UUID> {

        UUIDType() {
            super(UUID.class, Types.JAVA_OBJECT);
        }

        @Override
        public String identifier() {
            return "uuid";
        }

        @Override
        public void write(PreparedStatement statement, int index, UUID value)
                throws SQLException {
            statement.setObject(index, value);
        }
    }

    private static class SerialColumnDefinition implements GeneratedColumnDefinition {

        @Override
        public boolean skipTypeIdentifier() {
            return true;
        }

        @Override
        public boolean postFixPrimaryKey() {
            return false;
        }

        @Override
        public void appendGeneratedSequence(QueryBuilder qb, Attribute attribute) {
            qb.append("serial");
        }
    }

    private static class SystemVersionColumnDefinition implements VersionColumnDefinition {

        @Override
        public boolean createColumn() {
            return false;
        }

        @Override
        public String columnName() {
            return "xmin";
        }
    }

    private final SerialColumnDefinition serialColumnDefinition;
    private final LimitDefinition limitDefinition;
    private final VersionColumnDefinition versionColumnDefinition;

    public PostgresSQL() {
        serialColumnDefinition = new SerialColumnDefinition();
        limitDefinition = new LimitOffsetDefinition();
        versionColumnDefinition = new SystemVersionColumnDefinition();
    }

    @Override
    public boolean supportsInlineForeignKeyReference() {
        return true;
    }

    @Override
    public boolean supportsGeneratedKeysInBatchUpdate() {
        return true;
    }

    @Override
    public GeneratedColumnDefinition generatedColumnDefinition() {
        return serialColumnDefinition;
    }

    @Override
    public void addMappings(Mapping mapping) {
        super.addMappings(mapping);
        mapping.replaceType(Types.BINARY, new ByteArrayType(Types.BINARY));
        mapping.replaceType(Types.VARBINARY, new ByteArrayType(Types.VARBINARY));
        mapping.replaceType(Types.NVARCHAR, new VarCharType());
        mapping.putType(UUID.class, new UUIDType());
    }

    @Override
    public LimitDefinition limitDefinition() {
        return limitDefinition;
    }

    @Override
    public VersionColumnDefinition versionColumnDefinition() {
        return versionColumnDefinition;
    }
}
