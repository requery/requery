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

package io.requery.android.sqlite;

import java.sql.Array;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.NClob;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLClientInfoException;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Savepoint;
import java.sql.Statement;
import java.sql.Struct;
import java.util.Map;
import java.util.Properties;

/**
 * Base {@link Connection} implementation.
 *
 * @author Nikhil Purushe
 */
public abstract class BaseConnection implements Connection {

    protected boolean autoCommit;
    protected int transactionIsolation;
    protected int holdability;
    protected Properties clientInfo;
    protected int savePointId;

    protected BaseConnection() {
        autoCommit = true;
        holdability = ResultSet.HOLD_CURSORS_OVER_COMMIT;
        clientInfo = new Properties();
        transactionIsolation = TRANSACTION_SERIALIZABLE;
    }

    protected abstract void ensureTransaction();

    protected abstract void execSQL(String sql) throws SQLException;

    @Override
    public void clearWarnings() throws SQLException {
    }

    @Override
    public void close() throws SQLException {
        // db must be closed outside of the connection
    }

    @Override
    public void commit() throws SQLException {

    }

    @Override
    public Statement createStatement(int resultSetType, int resultSetConcurrency)
            throws SQLException {
        return createStatement(resultSetType,
                resultSetConcurrency, ResultSet.HOLD_CURSORS_OVER_COMMIT);
    }

    @Override
    public boolean getAutoCommit() throws SQLException {
        return autoCommit;
    }

    @Override
    public String getCatalog() throws SQLException {
        return null;
    }

    @Override
    public int getHoldability() throws SQLException {
        return holdability;
    }

    @Override
    public int getTransactionIsolation() throws SQLException {
        return transactionIsolation;
    }

    @Override
    public Map<String, Class<?>> getTypeMap() throws SQLException {
        return null;
    }

    @Override
    public SQLWarning getWarnings() throws SQLException {
        return null;
    }

    @Override
    public String nativeSQL(String sql) throws SQLException {
        return sql;
    }

    @Override
    public CallableStatement prepareCall(String sql) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency)
            throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency,
                                         int resultSetHoldability) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public PreparedStatement prepareStatement(String sql) throws SQLException {
        return prepareStatement(sql, Statement.NO_GENERATED_KEYS);
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int[] columnIndexes) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int resultSetType,
                                              int resultSetConcurrency) throws SQLException {
        return prepareStatement(sql, resultSetType, resultSetConcurrency,
                ResultSet.HOLD_CURSORS_OVER_COMMIT);
    }

    @Override
    public void releaseSavepoint(Savepoint savepoint) throws SQLException {
        execSQL("release savepoint " + savepoint.getSavepointName());
    }

    @Override
    public void rollback(Savepoint savepoint) throws SQLException {
        execSQL("rollback to savepoint " + savepoint.getSavepointName());
    }

    @Override
    public void setAutoCommit(boolean autoCommit) throws SQLException {
        this.autoCommit = autoCommit;
        ensureTransaction();
    }

    @Override
    public void setCatalog(String catalog) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public void setHoldability(int holdability) throws SQLException {
        this.holdability = holdability;
    }

    @Override
    public void setReadOnly(boolean readOnly) throws SQLException {
        throw new SQLFeatureNotSupportedException("cannot change readonly mode after db opened");
    }

    @Override
    public Savepoint setSavepoint() throws SQLException {
        return setSavepoint(null);
    }

    @Override
    public Savepoint setSavepoint(String name) throws SQLException {
        savePointId++;
        if (name == null) {
            name = "sp" + String.valueOf(savePointId);
        }
        execSQL("savepoint " + name);
        return new DatabaseSavepoint(savePointId, name);
    }

    @Override
    public void setTransactionIsolation(int level) throws SQLException {
        switch (level) {
            case TRANSACTION_NONE:
            case TRANSACTION_SERIALIZABLE:
            case TRANSACTION_READ_COMMITTED:
                execSQL("PRAGMA read_uncommitted = false");
                transactionIsolation = level;
                break;
            case TRANSACTION_READ_UNCOMMITTED:
                execSQL("PRAGMA read_uncommitted = true");
                transactionIsolation = level;
                break;
            case TRANSACTION_REPEATABLE_READ:
                throw new SQLFeatureNotSupportedException();
            default:
                throw new SQLException("invalid isolation " + level);
        }
    }

    @Override
    public void setTypeMap(Map<String, Class<?>> map) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public Clob createClob() throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public Blob createBlob() throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public NClob createNClob() throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public SQLXML createSQLXML() throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isValid(int timeout) throws SQLException {
        return !isClosed();
    }

    @Override
    public void setClientInfo(String name, String value) throws SQLClientInfoException {
        clientInfo.setProperty(name, value);
    }

    @Override
    public void setClientInfo(Properties properties) throws SQLClientInfoException {
        if(properties != null) {
            this.clientInfo = properties;
        }
    }

    @Override
    public String getClientInfo(String name) throws SQLException {
        return clientInfo.getProperty(name);
    }

    @Override
    public Properties getClientInfo() throws SQLException {
        return clientInfo;
    }

    @Override
    public Array createArrayOf(String typeName, Object[] elements) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public Struct createStruct(String typeName, Object[] attributes) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        return null;
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return false;
    }

    private static class DatabaseSavepoint implements Savepoint {

        private final int id;
        private final String name;

        DatabaseSavepoint(int id, String name) {
            this.id = id;
            this.name = name;
        }

        @Override
        public int getSavepointId() throws SQLException {
            return id;
        }

        @Override
        public String getSavepointName() throws SQLException {
            return name;
        }
    }
}
