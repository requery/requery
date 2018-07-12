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

package io.requery.android.sqlite

import android.database.sqlite.SQLiteAccessPermException
import android.database.sqlite.SQLiteCantOpenDatabaseException
import android.database.sqlite.SQLiteConstraintException
import android.database.sqlite.SQLiteDatabaseCorruptException

import java.sql.Array
import java.sql.Blob
import java.sql.CallableStatement
import java.sql.Clob
import java.sql.Connection
import java.sql.NClob
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.SQLException
import java.sql.SQLFeatureNotSupportedException
import java.sql.SQLIntegrityConstraintViolationException
import java.sql.SQLNonTransientException
import java.sql.SQLWarning
import java.sql.SQLXML
import java.sql.Savepoint
import java.sql.Statement
import java.sql.Struct
import java.util.Properties

/**
 * Base [Connection] implementation.
 *
 * @author Nikhil Purushe
 */
abstract class BaseConnection protected constructor() : Connection {

    private var _autoCommit: Boolean = false
    private var _transactionIsolation: Int = 0
    private var _holdability: Int = 0
    private var _clientInfo: Properties? = null
    private var _savePointId: Int = 0

    init {
        _autoCommit = true
        _holdability = ResultSet.HOLD_CURSORS_OVER_COMMIT
        _clientInfo = Properties()
        _transactionIsolation = Connection.TRANSACTION_SERIALIZABLE
    }

    protected abstract fun ensureTransaction()

    abstract fun execSQL(sql: String)

    override fun clearWarnings() {}

    override fun close() {
        // db must be closed outside of the connection
    }

    override fun commit() {}

    @Throws(SQLException::class)
    override fun createStatement(resultSetType: Int, resultSetConcurrency: Int): Statement {
        return createStatement(resultSetType,
                resultSetConcurrency, ResultSet.HOLD_CURSORS_OVER_COMMIT)
    }

    override fun getAutoCommit(): Boolean {
        return _autoCommit
    }

    override fun getCatalog(): String? {
        return null
    }

    override fun getHoldability(): Int {
        return _holdability
    }

    override fun getTransactionIsolation(): Int {
        return _transactionIsolation
    }

    override fun getTypeMap(): Map<String, Class<*>>? {
        return null
    }

    override fun getWarnings(): SQLWarning? {
        return null
    }

    override fun nativeSQL(sql: String): String {
        return sql
    }

    @Throws(SQLException::class)
    override fun prepareCall(sql: String): CallableStatement {
        throw SQLFeatureNotSupportedException()
    }

    @Throws(SQLException::class)
    override fun prepareCall(sql: String, resultSetType: Int, resultSetConcurrency: Int): CallableStatement {
        throw SQLFeatureNotSupportedException()
    }

    @Throws(SQLException::class)
    override fun prepareCall(sql: String, resultSetType: Int, resultSetConcurrency: Int,
                             resultSetHoldability: Int): CallableStatement {
        throw SQLFeatureNotSupportedException()
    }

    @Throws(SQLException::class)
    override fun prepareStatement(sql: String): PreparedStatement {
        return prepareStatement(sql, Statement.NO_GENERATED_KEYS)
    }

    @Throws(SQLException::class)
    override fun prepareStatement(sql: String, columnIndexes: IntArray): PreparedStatement {
        throw SQLFeatureNotSupportedException()
    }

    @Throws(SQLException::class)
    override fun prepareStatement(sql: String, resultSetType: Int,
                                  resultSetConcurrency: Int): PreparedStatement {
        return prepareStatement(sql, resultSetType, resultSetConcurrency,
                ResultSet.HOLD_CURSORS_OVER_COMMIT)
    }

    @Throws(SQLException::class)
    override fun releaseSavepoint(savepoint: Savepoint) {
        execSQL("release savepoint " + savepoint.savepointName)
    }

    @Throws(SQLException::class)
    override fun rollback(savepoint: Savepoint) {
        execSQL("rollback to savepoint " + savepoint.savepointName)
    }

    override fun setAutoCommit(autoCommit: Boolean) {
        this._autoCommit = autoCommit
        ensureTransaction()
    }

    @Throws(SQLException::class)
    override fun setCatalog(catalog: String) {
        throw SQLFeatureNotSupportedException()
    }

    override fun setHoldability(holdability: Int) {
        this._holdability = holdability
    }

    @Throws(SQLException::class)
    override fun setReadOnly(readOnly: Boolean) {
        throw SQLFeatureNotSupportedException("cannot change readonly mode after db opened")
    }

    @Throws(SQLException::class)
    override fun setSavepoint(): Savepoint {
        return setSavepoint(null)
    }

    @Throws(SQLException::class)
    override fun setSavepoint(name: String?): Savepoint {
        var name = name
        _savePointId++
        if (name == null) {
            name = "sp" + _savePointId.toString()
        }
        execSQL("savepoint $name")
        return DatabaseSavepoint(_savePointId, name)
    }

    @Throws(SQLException::class)
    override fun setTransactionIsolation(level: Int) {
        _transactionIsolation = when (level) {
            Connection.TRANSACTION_NONE, Connection.TRANSACTION_SERIALIZABLE, Connection.TRANSACTION_READ_COMMITTED -> {
                execSQL("PRAGMA read_uncommitted = false")
                level
            }
            Connection.TRANSACTION_READ_UNCOMMITTED -> {
                execSQL("PRAGMA read_uncommitted = true")
                level
            }
            Connection.TRANSACTION_REPEATABLE_READ -> throw SQLFeatureNotSupportedException()
            else -> throw SQLException("invalid isolation $level")
        }
    }

    @Throws(SQLException::class)
    override fun setTypeMap(map: Map<String, Class<*>>) {
        throw SQLFeatureNotSupportedException()
    }

    @Throws(SQLException::class)
    override fun createClob(): Clob {
        throw SQLFeatureNotSupportedException()
    }

    @Throws(SQLException::class)
    override fun createBlob(): Blob {
        throw SQLFeatureNotSupportedException()
    }

    @Throws(SQLException::class)
    override fun createNClob(): NClob {
        throw SQLFeatureNotSupportedException()
    }

    override fun createSQLXML(): SQLXML {
        throw UnsupportedOperationException()
    }

    @Throws(SQLException::class)
    override fun isValid(timeout: Int): Boolean {
        return !isClosed
    }

    override fun setClientInfo(name: String, value: String) {
        _clientInfo!!.setProperty(name, value)
    }

    override fun setClientInfo(properties: Properties?) {
        if (properties != null) {
            this._clientInfo = properties
        }
    }

    override fun getClientInfo(name: String): String {
        return _clientInfo!!.getProperty(name)
    }

    override fun getClientInfo(): Properties? {
        return _clientInfo
    }

    override fun createArrayOf(typeName: String?, elements: kotlin.Array<out Any>?): Array {
        throw SQLFeatureNotSupportedException()
    }

    override fun createStruct(typeName: String?, attributes: kotlin.Array<out Any>?): Struct {
        throw SQLFeatureNotSupportedException()
    }

    override fun <T> unwrap(iface: Class<T>): T? {
        return null
    }

    override fun isWrapperFor(iface: Class<*>): Boolean {
        return false
    }

    private class DatabaseSavepoint
    internal constructor(private val id: Int, private val name: String) : Savepoint {

        override fun getSavepointId(): Int {
            return id
        }

        override fun getSavepointName(): String {
            return name
        }
    }

    companion object {

        @Throws(SQLException::class)
        fun throwSQLException(exception: android.database.SQLException) {

            if (exception is SQLiteConstraintException) {
                throw SQLIntegrityConstraintViolationException(exception)

            } else if (exception is SQLiteCantOpenDatabaseException ||
                    exception is SQLiteDatabaseCorruptException ||
                    exception is SQLiteAccessPermException) {

                throw SQLNonTransientException(exception)
            }
            throw SQLException(exception)
        }
    }
}
