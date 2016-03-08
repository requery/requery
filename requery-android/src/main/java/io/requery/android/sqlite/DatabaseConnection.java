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

import android.database.sqlite.SQLiteAccessPermException;
import android.database.sqlite.SQLiteCantOpenDatabaseException;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabaseCorruptException;

import java.sql.Array;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.NClob;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLClientInfoException;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.SQLNonTransientException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Savepoint;
import java.sql.Statement;
import java.sql.Struct;
import java.util.Map;
import java.util.Properties;

/**
 * {@link java.sql.Connection} implementation using Android's local SQLite database Java API.
 *
 * @author Nikhil Purushe
 */
public class DatabaseConnection implements Connection {

    private final SQLiteDatabase db;
    private final BasicDatabaseMetaData metaData;
    private boolean autoCommit;
    private int transactionIsolation;
    private int holdability;
    private Properties clientInfo;
    private int savePointId;
    private boolean enteredTransaction;

    public DatabaseConnection(SQLiteDatabase db) {
        if(db == null) {
            throw new IllegalArgumentException("null db");
        }
        this.db = db;
        autoCommit = true;
        holdability = ResultSet.HOLD_CURSORS_OVER_COMMIT;
        clientInfo = new Properties();
        transactionIsolation = TRANSACTION_SERIALIZABLE;
        metaData = new BasicDatabaseMetaData(this);
    }

    static void throwSQLException(android.database.SQLException exception) throws SQLException {
        if(exception instanceof SQLiteConstraintException) {
            throw new SQLIntegrityConstraintViolationException(exception);

        } else if(exception instanceof SQLiteCantOpenDatabaseException ||
                exception instanceof SQLiteDatabaseCorruptException ||
                exception instanceof SQLiteAccessPermException) {

            throw new SQLNonTransientException(exception);
        }
        throw new SQLException(exception);
    }

    private void ensureTransaction() {
        if (!autoCommit) {
            if (!db.inTransaction()) {
                db.beginTransactionNonExclusive();
                enteredTransaction = true;
            }
        }
    }

    SQLiteDatabase getDatabase() {
        return db;
    }

    void execSQL(String sql) throws SQLException {
        try {
            db.execSQL(sql);
        } catch (android.database.SQLException e) {
            throwSQLException(e);
        }
    }

    @Override
    public void clearWarnings() throws SQLException {
    }

    @Override
    public void close() throws SQLException {
        // db must be closed outside of the connection
    }

    @Override
    public void commit() throws SQLException {
        if (autoCommit) {
            throw new SQLException("commit called while in autoCommit mode");
        }
        if (db.inTransaction() && enteredTransaction) {
            try {
                db.setTransactionSuccessful();
            } catch (IllegalStateException e) {
                throw new SQLException(e);
            } finally {
                db.endTransaction();
                enteredTransaction = false;
            }
        }
    }

    @Override
    public Statement createStatement() throws SQLException {
        ensureTransaction();
        return new StatementAdapter(this);
    }

    @Override
    public Statement createStatement(int resultSetType, int resultSetConcurrency)
            throws SQLException {
        ensureTransaction();
        return createStatement(resultSetType,
                resultSetConcurrency, ResultSet.HOLD_CURSORS_OVER_COMMIT);
    }

    @Override
    public Statement createStatement(int resultSetType, int resultSetConcurrency,
                                     int resultSetHoldability) throws SQLException {
        if (resultSetConcurrency == ResultSet.CONCUR_UPDATABLE) {
            throw new SQLFeatureNotSupportedException("CONCUR_UPDATABLE not supported");
        }
        ensureTransaction();
        return new StatementAdapter(this);
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
    public DatabaseMetaData getMetaData() throws SQLException {
        return metaData;
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
    public boolean isClosed() throws SQLException {
        return !db.isOpen();
    }

    @Override
    public boolean isReadOnly() throws SQLException {
        return db.isReadOnly();
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
    public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys) throws SQLException {
        ensureTransaction();
        return new PreparedStatementAdapter(this, sql, autoGeneratedKeys);
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
    public PreparedStatement prepareStatement(String sql,
                                              int resultSetType,
                                              int resultSetConcurrency,
                                              int resultSetHoldability) throws SQLException {
        if (resultSetConcurrency == ResultSet.CONCUR_UPDATABLE) {
            throw new SQLFeatureNotSupportedException("CONCUR_UPDATABLE not supported");
        }
        ensureTransaction();
        return new PreparedStatementAdapter(this, sql, Statement.NO_GENERATED_KEYS);
    }

    @Override
    public PreparedStatement prepareStatement(String sql, String[] columnNames)
            throws SQLException {
        if (columnNames.length != 1) {
            throw new SQLFeatureNotSupportedException();
        }

        ensureTransaction();
        return new PreparedStatementAdapter(this, sql, Statement.RETURN_GENERATED_KEYS);
    }

    @Override
    public void releaseSavepoint(Savepoint savepoint) throws SQLException {
        execSQL("release savepoint " + savepoint.getSavepointName());
    }

    @Override
    public void rollback() throws SQLException {
        if (autoCommit) {
            throw new SQLException("commit called while in autoCommit mode");
        }
        db.endTransaction();
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
        return new ByteArrayBlob(new byte[2048]);
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
        return db.isOpen();
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
        if (iface == SQLiteDatabase.class) {
            return iface.cast(getDatabase());
        }
        return null;
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return iface == SQLiteDatabase.class;
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
