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

package io.requery.sql;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Provides a cache of PreparedStatement instances, normally not required since the driver or
 * database will provide this functionality already.
 *
 * @author Nikhil Purushe
 */
class PreparedStatementCache implements AutoCloseable {

    private static class CachedStatement extends PreparedStatementDelegate {

        private final String sql;
        private final PreparedStatementCache cache;
        private final PreparedStatement statement;

        CachedStatement(PreparedStatementCache cache, String sql, PreparedStatement statement) {
            super(statement);
            this.cache = cache;
            this.sql = sql;
            this.statement = statement;
        }

        void closeDelegate() throws SQLException {
            statement.close();
        }

        @Override
        public void close() throws SQLException {
            cache.put(sql, this);
        }
    }

    private final LinkedHashMap<String, PreparedStatement> elements;
    private boolean closed;

    public PreparedStatementCache(final int count) {
        elements = new LinkedHashMap<String, PreparedStatement>(count, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry eldest) {
                synchronized (elements) {
                    if (elements.size() > count) {
                        PreparedStatement statement = (PreparedStatement) eldest.getValue();
                        closeStatement(statement);
                        return true;
                    }
                }
                return false;
            }
        };
    }

    public PreparedStatement get(String sql) throws SQLException {
        synchronized (elements) {
            if (closed) {
                return null;
            }
            PreparedStatement statement = elements.remove(sql);
            if (statement != null && statement.isClosed()) {
                return null;
            }
            return statement;
        }
    }

    public PreparedStatement put(String sql, PreparedStatement statement) {
        if (!(statement instanceof CachedStatement)) {
            statement = new CachedStatement(this, sql, statement);
        }
        synchronized (elements) {
            if (closed) {
                return null;
            }
            elements.put(sql, statement);
        }
        return statement;
    }

    @Override
    public void close() {
        synchronized (elements) {
            if (closed) {
                return;
            }
            closed = true;
            for (PreparedStatement statement : elements.values()) {
                closeStatement(statement);
            }
            elements.clear();
        }
    }

    private void closeStatement(PreparedStatement statement) {
        try {
            if (!statement.isClosed()) {
                if (statement instanceof CachedStatement) {
                    CachedStatement delegate = (CachedStatement) statement;
                    delegate.closeDelegate();
                }
            }
        } catch (SQLException ignored) {
            ignored.printStackTrace();
        }
    }
}
