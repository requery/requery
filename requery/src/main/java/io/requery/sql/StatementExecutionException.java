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

package io.requery.sql;

import io.requery.PersistenceException;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Exception indicating an error executing or preparing a {@link Statement}.
 */
public class StatementExecutionException extends PersistenceException {

    private static boolean useSuppressed;

    static {
        // only used suppressed exceptions if not Android since only Android 4.4 has Java 7
        String vendor = System.getProperty("java.vendor");
        useSuppressed = !vendor.contains("Android");
    }

    static StatementExecutionException closing(Statement statement, Throwable cause, String sql) {
        StatementExecutionException e = new StatementExecutionException(cause, sql);
        if (statement != null) {
            Connection connection = null;
            try {
                connection = statement.getConnection();
            } catch (SQLException suppressed) {
                if (useSuppressed) {
                    e.addSuppressed(suppressed);
                }
            }
            e.closeSuppressed(statement);
            e.closeSuppressed(connection);
        }
        return e;
    }

    private final String sql;

    StatementExecutionException(Throwable throwable, String sql) {
        super("Exception executing statement: " + sql, throwable);
        this.sql = sql;
    }

    public String getSql() {
        return sql;
    }

    private void closeSuppressed(AutoCloseable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (Exception suppressed) {
                if (useSuppressed) {
                    addSuppressed(suppressed);
                } else {
                    suppressed.printStackTrace();
                }
            }
        }
    }
}
