/*
 * Copyright 2018 requery.io
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

package io.requery.android.sqlite;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.SQLWarning;
import java.sql.Statement;

/**
 * {@link Statement} implementation using Android's local SQLite database.
 *
 * @author Nikhil Purushe
 */
public abstract class BaseStatement implements Statement {

    protected final BaseConnection connection;
    protected ResultSet queryResult;
    protected ResultSet insertResult;
    protected int updateCount;
    private boolean closed;
    private int timeout;
    private int maxRows;
    private int maxFieldSize;
    private int fetchSize;

    protected BaseStatement(BaseConnection connection) {
        if (connection == null) {
            throw new IllegalArgumentException("null connection");
        }
        this.connection = connection;
    }

    protected void throwIfClosed() throws SQLException {
        if (isClosed()) {
            throw new SQLException("closed");
        }
    }

    @Override
    public void addBatch(String sql) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public void cancel() throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public void clearBatch() throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public void clearWarnings() {

    }

    @Override
    public void close() throws SQLException {
        if (queryResult != null) {
            queryResult.close();
        }
        closed = true;
    }

    @Override
    public boolean execute(String sql) throws SQLException {
        throwIfClosed();
        connection.execSQL(sql);
        return false;
    }

    @Override
    public boolean execute(String sql, int[] columnIndexes) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public boolean execute(String sql, String[] columnNames) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public int[] executeBatch() throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public int executeUpdate(String sql) throws SQLException {
        return executeUpdate(sql, NO_GENERATED_KEYS);
    }

    @Override
    public int executeUpdate(String sql, int[] columnIndexes) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public int executeUpdate(String sql, String[] columnNames) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public Connection getConnection() throws SQLException {
        throwIfClosed();
        return connection;
    }

    @Override
    public int getFetchDirection() {
        return ResultSet.FETCH_FORWARD;
    }

    @Override
    public int getFetchSize() {
        return fetchSize;
    }

    @Override
    public ResultSet getGeneratedKeys() {
        return insertResult;
    }

    @Override
    public int getMaxFieldSize() {
        return maxFieldSize;
    }

    @Override
    public int getMaxRows() {
        return maxRows;
    }

    @Override
    public boolean getMoreResults() throws SQLException {
        return getMoreResults(CLOSE_CURRENT_RESULT);
    }

    @Override
    public boolean getMoreResults(int current) {
        return false;
    }

    @Override
    public int getQueryTimeout() {
        return timeout;
    }

    @Override
    public ResultSet getResultSet() throws SQLException {
        throwIfClosed();
        return queryResult;
    }

    @Override
    public int getResultSetConcurrency() {
        return ResultSet.CONCUR_READ_ONLY;
    }

    @Override
    public int getResultSetHoldability() {
        return connection.getHoldability();
    }

    @Override
    public int getResultSetType() {
        return ResultSet.TYPE_SCROLL_INSENSITIVE;
    }

    @Override
    public int getUpdateCount() {
        return updateCount;
    }

    @Override
    public SQLWarning getWarnings() {
        return null;
    }

    @Override
    public void setCursorName(String name) {
    }

    @Override
    public void setEscapeProcessing(boolean enable) {

    }

    @Override
    public void setFetchDirection(int direction) throws SQLException {
        if (direction != ResultSet.FETCH_FORWARD) {
            throw new SQLFeatureNotSupportedException("only FETCH_FORWARD is supported");
        }
    }

    @Override
    public void setFetchSize(int rows) {
        this.fetchSize = rows;
    }

    @Override
    public void setMaxFieldSize(int max) {
        this.maxFieldSize = max;
    }

    @Override
    public void setMaxRows(int max) {
        this.maxRows = max;
    }

    @Override
    public void setQueryTimeout(int seconds) {
        this.timeout = seconds;
    }

    @Override
    public boolean isClosed() {
        return closed;
    }

    @Override
    public void setPoolable(boolean poolable) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public boolean isPoolable() {
        return false;
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) {
        return false;
    }
}
