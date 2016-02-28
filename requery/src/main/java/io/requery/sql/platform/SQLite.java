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

import io.requery.sql.AutoIncrementColumnDefinition;
import io.requery.sql.BasicType;
import io.requery.sql.GeneratedColumnDefinition;
import io.requery.sql.Keyword;
import io.requery.sql.LimitDefinition;
import io.requery.sql.LimitOffsetDefinition;
import io.requery.sql.Mapping;
import io.requery.sql.type.PrimitiveLongType;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

/**
 * SQLite. (3.6 and later)
 */
public class SQLite extends Generic {

    private final AutoIncrementColumnDefinition autoIncrementColumn;
    private final LimitDefinition limitDefinition;

    // in SQLite BIGINT can be treated as just an integer, this handles the case when an long is
    // used as generated key
    private static class LongType extends BasicType<Long> implements PrimitiveLongType {

        public LongType(Class<Long> type) {
            super(type, Types.INTEGER);
        }

        @Override
        public Long fromResult(ResultSet results, int column) throws SQLException {
            return results.getLong(column);
        }

        @Override
        public Keyword identifier() {
            return Keyword.INTEGER;
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

    public SQLite() {
        autoIncrementColumn = new AutoIncrementColumnDefinition("autoincrement");
        limitDefinition = new LimitOffsetDefinition();
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
    public void addMappings(Mapping mapping) {
        super.addMappings(mapping);
        mapping.putType(long.class, new LongType(long.class));
        mapping.putType(Long.class, new LongType(Long.class));
    }
}
