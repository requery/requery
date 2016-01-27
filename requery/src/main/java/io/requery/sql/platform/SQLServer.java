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
import io.requery.sql.BasicTypes;
import io.requery.sql.GeneratedColumnDefinition;
import io.requery.sql.Keyword;
import io.requery.sql.LimitDefinition;
import io.requery.sql.Mapping;
import io.requery.sql.OffsetFetchLimitDefinition;
import io.requery.sql.QueryBuilder;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

/**
 * Platform for Microsoft SQL Server 2012 or later T-SQL.
 */
public class SQLServer extends Generic {

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

    private static class OrderByOffsetFetchLimit extends OffsetFetchLimitDefinition {

        @Override
        public boolean requireOrderBy() {
            return true;
        }

        @Override
        public void appendLimit(QueryBuilder qb, Integer limit, Integer offset) {
            // always include the offset
            super.appendLimit(qb, limit, offset == null ? 0 : offset);
        }
    }

    // no boolean support
    private static class BitBooleanType extends BaseType<Boolean> {

        BitBooleanType() {
            super(Boolean.class, Types.BIT);
        }

        @Override
        public Object identifier() {
            return "bit";
        }

        @Override
        public Boolean read(ResultSet results, int column) throws SQLException {
            Boolean value = results.getBoolean(column);
            return results.wasNull() ? null : value;
        }
    }

    private final GeneratedColumnDefinition generatedColumnDefinition;
    private final LimitDefinition limitDefinition;

    public SQLServer() {
        generatedColumnDefinition = new IdentityColumnDefinition();
        limitDefinition = new OrderByOffsetFetchLimit();
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
    public LimitDefinition limitDefinition() {
        return limitDefinition;
    }

    @Override
    public void addMappings(Mapping mapping) {
        super.addMappings(mapping);
        mapping.replaceType(BasicTypes.BOOLEAN, new BitBooleanType());
    }
}
