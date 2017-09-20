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
import io.requery.meta.Type;
import io.requery.query.Expression;
import io.requery.sql.BaseType;
import io.requery.sql.GeneratedColumnDefinition;
import io.requery.sql.Mapping;
import io.requery.sql.QueryBuilder;
import io.requery.sql.VersionColumnDefinition;
import io.requery.sql.gen.Generator;
import io.requery.sql.gen.LimitGenerator;
import io.requery.sql.gen.Output;
import io.requery.sql.type.VarCharType;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.sql.Blob;
import java.util.Map;
import java.util.UUID;

import static io.requery.sql.Keyword.CONFLICT;
import static io.requery.sql.Keyword.DO;
import static io.requery.sql.Keyword.INSERT;
import static io.requery.sql.Keyword.INTO;
import static io.requery.sql.Keyword.ON;
import static io.requery.sql.Keyword.SET;
import static io.requery.sql.Keyword.UPDATE;
import static io.requery.sql.Keyword.VALUES;

import javax.sql.rowset.serial.SerialBlob;

/**
 * PostgresSQL PL/pgSQL (9+)
 */
public class PostgresSQL extends Generic {

    private final SerialColumnDefinition serialColumnDefinition;
    private final VersionColumnDefinition versionColumnDefinition;

    public PostgresSQL() {
        serialColumnDefinition = new SerialColumnDefinition();
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
        mapping.replaceType(Types.BLOB, new BlobType());
        mapping.putType(UUID.class, new UUIDType());
    }

    @Override
    public LimitGenerator limitGenerator() {
        return new LimitGenerator();
    }

    @Override
    public VersionColumnDefinition versionColumnDefinition() {
        return versionColumnDefinition;
    }
    
    @Override
    public Generator<Map<Expression<?>, Object>> upsertGenerator() {
        return new UpsertOnConflictDoUpdate();
    }
    
    private static class BlobType extends BaseType<Blob> {
        
        BlobType() {
            super(Blob.class, Types.VARBINARY);
        }
        
        @Override
        public String getIdentifier() {
            return "bytea";
        }
        
        @Override
        public Blob read(ResultSet results, int column) throws SQLException {
            byte[] value = results.getBytes(column);
            return results.wasNull() ? null : new SerialBlob(value);
        }
    
        @Override
        public void write(PreparedStatement statement, int index, Blob value) throws SQLException {
            if (value == null) {
                statement.setNull(index, Types.VARBINARY);
            } else {
                statement.setBinaryStream(index, value.getBinaryStream(), value.length());
            }
        }
    }
    
    private static class ByteArrayType extends BaseType<byte[]> {

        ByteArrayType(int jdbcType) {
            super(byte[].class, jdbcType);
        }

        @Override
        public String getIdentifier() {
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
        public String getIdentifier() {
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

    /**
     * Performs an upsert (insert/update) using insert on conflict do update.
     */
    private static class UpsertOnConflictDoUpdate implements Generator<Map<Expression<?>, Object>> {

        @Override
        public void write(final Output output, final Map<Expression<?>, Object> values) {
            QueryBuilder qb = output.builder();
            Type<?> type = ((Attribute)values.keySet().iterator().next()).getDeclaringType();
            // insert into <table> (<columns>) values (<values)
            // on conflict do update (<column>=EXCLUDED.<value>...
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
                .keyword(ON, CONFLICT)
                .openParenthesis()
                .commaSeparatedAttributes(type.getKeyAttributes())
                .closeParenthesis().space()
                .keyword(DO, UPDATE, SET)
                .commaSeparated(values.keySet(), new QueryBuilder.Appender<Expression<?>>() {
                    @Override
                    public void append(QueryBuilder qb, Expression<?> value) {
                        qb.attribute((Attribute) value);
                        qb.append("= EXCLUDED." + value.getName());
                    }
                });
        }
    }
}
