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
import io.requery.query.element.LimitedElement;
import io.requery.query.element.OrderByElement;
import io.requery.query.element.QueryElement;
import io.requery.query.function.Function;
import io.requery.query.function.Now;
import io.requery.sql.BaseType;
import io.requery.sql.GeneratedColumnDefinition;
import io.requery.sql.Keyword;
import io.requery.sql.Mapping;
import io.requery.sql.QueryBuilder;
import io.requery.sql.gen.Generator;
import io.requery.sql.gen.OffsetFetchGenerator;
import io.requery.sql.gen.OrderByGenerator;
import io.requery.sql.gen.UpsertMergeGenerator;
import io.requery.sql.gen.Output;
import io.requery.sql.type.PrimitiveBooleanType;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Map;
import java.util.Set;

/**
 * Microsoft SQL Server 2012 or later
 */
public class SQLServer extends Generic {

    private final GeneratedColumnDefinition generatedColumnDefinition;

    public SQLServer() {
        generatedColumnDefinition = new IdentityColumnDefinition();
    }

    @Override
    public void addMappings(Mapping mapping) {
        super.addMappings(mapping);
        mapping.replaceType(Types.BOOLEAN, new BitBooleanType());
        mapping.replaceType(Types.TIMESTAMP, new DateTime2TimeStampType());
        mapping.aliasFunction(new Function.Name("getutcdate"), Now.class);
    }

    @Override
    public boolean supportsIfExists() {
        return false;
    }

    @Override
    public GeneratedColumnDefinition generatedColumnDefinition() {
        return generatedColumnDefinition;
    }

    @Override
    public Generator<LimitedElement> limitGenerator() {
        return new OrderByOffsetFetchLimit();
    }

    @Override
    public Generator<Map<Expression<?>, Object>> upsertGenerator() {
        return new MergeGenerator();
    }

    @Override
    public Generator<OrderByElement> orderByGenerator() {
        return new OrderByWithLimitGenerator();
    }

    private static class MergeGenerator extends UpsertMergeGenerator {
        @Override
        public void write(Output output, Map<Expression<?>, Object> values) {
            super.write(output, values);
            // for some reason insists on having a semicolon on a merge statement only
            output.builder().append(";");
        }
    }

    private class OrderByWithLimitGenerator extends OrderByGenerator {

        private void forceOrderBy(QueryElement<?> query) {
            if (query.getLimit() != null &&
                (query.getOrderByExpressions() == null || query.getOrderByExpressions().isEmpty())) {
                Set<Type<?>> types = query.entityTypes();
                if (types != null && !types.isEmpty()) {
                    Type<?> type = types.iterator().next();
                    for (Attribute attribute : type.getAttributes()) {
                        if (attribute.isKey()) {
                            query.orderBy((Expression) attribute);
                            break;
                        }
                    }
                }
            }
        }

        @Override
        public void write(Output output, OrderByElement query) {
            // hack needed to force order by expression if limit present
            if (query instanceof QueryElement) {
                forceOrderBy((QueryElement<?>) query);
            }
            super.write(output, query);
        }
    }

    private static class IdentityColumnDefinition implements GeneratedColumnDefinition {

        @Override
        public boolean skipTypeIdentifier() {
            return false;
        }

        @Override
        public boolean postFixPrimaryKey() {
            return false;
        }

        @Override
        public void appendGeneratedSequence(QueryBuilder qb, Attribute attribute) {
            int start = 1;
            int increment = 1;
            qb.keyword(Keyword.IDENTITY);
            qb.openParenthesis()
                .value(start).comma().value(increment)
                .closeParenthesis();
        }
    }

    private static class OrderByOffsetFetchLimit extends OffsetFetchGenerator {
        @Override
        public void write(QueryBuilder qb, Integer limit, Integer offset) {
            // always include the offset
            super.write(qb, limit, offset == null ? 0 : offset);
        }
    }

    // no boolean support
    private static class BitBooleanType extends BaseType<Boolean> implements PrimitiveBooleanType {

        BitBooleanType() {
            super(Boolean.class, Types.BIT);
        }

        @Override
        public Object getIdentifier() {
            return "bit";
        }

        @Override
        public Boolean read(ResultSet results, int column) throws SQLException {
            Boolean value = results.getBoolean(column);
            return results.wasNull() ? null : value;
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
    
    private static class DateTime2TimeStampType extends BaseType<java.sql.Timestamp> {

        DateTime2TimeStampType() {
            super(java.sql.Timestamp.class, Types.TIMESTAMP);
        }

        @Override
        public Object getIdentifier() {
            return "datetime2";
        }
    }
}
