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

import android.database.Cursor

import java.io.ByteArrayInputStream
import java.io.InputStream
import java.io.Reader
import java.io.StringReader
import java.math.BigDecimal
import java.net.MalformedURLException
import java.net.URL
import java.sql.Array
import java.sql.Blob
import java.sql.Clob
import java.sql.Date
import java.sql.NClob
import java.sql.Ref
import java.sql.ResultSet
import java.sql.ResultSetMetaData
import java.sql.RowId
import java.sql.SQLDataException
import java.sql.SQLException
import java.sql.SQLFeatureNotSupportedException
import java.sql.SQLWarning
import java.sql.SQLXML
import java.sql.Statement
import java.sql.Time
import java.sql.Timestamp
import java.sql.Types
import java.text.ParseException
import java.util.Calendar

/**
 * [ResultSet] implementation using Android's [Cursor] interface.
 *
 * @author Nikhil Purushe
 */
@Suppress("OverridingDeprecatedMember")
class CursorResultSet(private val statement: Statement?, private val cursor: Cursor, private val closeCursor: Boolean)
    : NonUpdateableResultSet(), ResultSetMetaData {

    private var lastColumnIndex: Int = 0

    init {
        cursor.moveToPosition(-1)
    }

    override fun absolute(row: Int): Boolean {
        return cursor.moveToPosition(row - 1)
    }

    override fun afterLast() {
        cursor.moveToLast()
        cursor.moveToNext()
    }

    override fun beforeFirst() {
        cursor.moveToPosition(-1)
    }

    override fun cancelRowUpdates() {
        throw SQLFeatureNotSupportedException()
    }

    override fun clearWarnings() {}

    override fun close() {
        if (closeCursor) {
            cursor.close()
        }
    }

    override fun deleteRow() {
        throw SQLFeatureNotSupportedException()
    }

    override fun findColumn(columnName: String): Int {
        val index = cursor.getColumnIndex(columnName)
        if (index == -1) {
            throw SQLDataException("no column $columnName")
        }
        return index + 1
    }

    override fun first(): Boolean {
        return cursor.moveToFirst()
    }

    override fun getArray(columnIndex: Int): Array {
        throw SQLFeatureNotSupportedException()
    }

    override fun getArray(colName: String): Array {
        throw SQLFeatureNotSupportedException()
    }

    override fun getAsciiStream(columnIndex: Int): InputStream {
        throw SQLFeatureNotSupportedException()
    }

    override fun getAsciiStream(columnName: String): InputStream {
        throw SQLFeatureNotSupportedException()
    }

    override fun getBigDecimal(columnIndex: Int): BigDecimal? {
        val value = getString(columnIndex)
        return if (value == null) null else BigDecimal(value)
    }

    override fun getBigDecimal(columnIndex: Int, scale: Int): BigDecimal? {
        val value = getString(columnIndex)
        var result: BigDecimal? = if (value == null) null else BigDecimal(value)
        if (result != null) {
            result = result.setScale(scale, BigDecimal.ROUND_DOWN)
        }
        return result
    }

    override fun getBigDecimal(columnName: String): BigDecimal? {
        return getBigDecimal(findColumn(columnName))
    }

    override fun getBigDecimal(columnName: String, scale: Int): BigDecimal? {
        return getBigDecimal(findColumn(columnName))
    }

    override fun getBinaryStream(columnIndex: Int): InputStream {
        return ByteArrayInputStream(getBytes(columnIndex))
    }

    override fun getBinaryStream(columnName: String): InputStream {
        return getBinaryStream(findColumn(columnName))
    }

    override fun getBlob(columnIndex: Int): Blob {
        throw SQLFeatureNotSupportedException()
    }

    override fun getBlob(columnName: String): Blob {
        return getBlob(findColumn(columnName))
    }

    override fun getBoolean(columnIndex: Int): Boolean {
        return getInt(columnIndex) > 0
    }

    override fun getBoolean(columnName: String): Boolean {
        return getBoolean(findColumn(columnName))
    }

    override fun getByte(columnIndex: Int): Byte {
        return getShort(columnIndex).toByte()
    }

    override fun getByte(columnName: String): Byte {
        return getByte(findColumn(columnName))
    }

    override fun getBytes(columnIndex: Int): ByteArray {
        lastColumnIndex = columnIndex
        return cursor.getBlob(columnIndex - 1)
    }

    override fun getBytes(columnName: String): ByteArray {
        return getBytes(findColumn(columnName))
    }

    override fun getCharacterStream(columnIndex: Int): Reader {
        return StringReader(getString(columnIndex)!!)
    }

    override fun getCharacterStream(columnName: String): Reader {
        return StringReader(getString(columnName)!!)
    }

    override fun getClob(columnIndex: Int): Clob {
        throw SQLFeatureNotSupportedException()
    }

    override fun getClob(colName: String): Clob {
        throw SQLFeatureNotSupportedException()
    }

    override fun getConcurrency(): Int {
        return ResultSet.CONCUR_READ_ONLY
    }

    override fun getCursorName(): String? = null

    override fun getDate(columnIndex: Int): Date? {
        lastColumnIndex = columnIndex
        if (cursor.isNull(columnIndex - 1)) {
            return null
        }
        return if (cursor.getType(columnIndex - 1) == Cursor.FIELD_TYPE_INTEGER) {
            Date(cursor.getLong(columnIndex - 1))
        } else {
            try {
                val value = cursor.getString(columnIndex - 1)
                Date(BasePreparedStatement.ISO8601_FORMAT.get().parse(value).time)
            } catch (e: ParseException) {
                throw SQLException(e)
            }

        }
    }

    override fun getDate(columnIndex: Int, cal: Calendar): Date {
        throw SQLFeatureNotSupportedException()
    }

    override fun getDate(columnName: String): Date? {
        return getDate(findColumn(columnName))
    }

    override fun getDate(columnName: String, cal: Calendar): Date {
        throw SQLFeatureNotSupportedException()
    }

    override fun getDouble(columnIndex: Int): Double {
        lastColumnIndex = columnIndex
        return cursor.getDouble(columnIndex - 1)
    }

    override fun getDouble(columnName: String): Double {
        return getDouble(findColumn(columnName))
    }

    override fun getFetchDirection(): Int {
        return ResultSet.FETCH_FORWARD
    }

    override fun getFetchSize(): Int = 0

    override fun getFloat(columnIndex: Int): Float {
        lastColumnIndex = columnIndex
        return cursor.getFloat(columnIndex - 1)
    }

    override fun getFloat(columnName: String): Float {
        return getFloat(findColumn(columnName))
    }

    override fun getInt(columnIndex: Int): Int {
        lastColumnIndex = columnIndex
        return cursor.getInt(columnIndex - 1)
    }

    override fun getInt(columnName: String): Int {
        return getInt(findColumn(columnName))
    }

    override fun getLong(columnIndex: Int): Long {
        lastColumnIndex = columnIndex
        return cursor.getLong(columnIndex - 1)
    }

    override fun getLong(columnName: String): Long {
        return getLong(findColumn(columnName))
    }

    override fun getMetaData(): ResultSetMetaData {
        return this
    }

    override fun getObject(columnIndex: Int): Any? {
        lastColumnIndex = columnIndex
        val index = columnIndex - 1
        val type = cursor.getType(index)
        if (!cursor.isNull(index)) {
            when (type) {
                Cursor.FIELD_TYPE_BLOB -> return cursor.getBlob(index)
                Cursor.FIELD_TYPE_FLOAT -> return cursor.getFloat(index)
                Cursor.FIELD_TYPE_INTEGER -> return cursor.getInt(index)
                Cursor.FIELD_TYPE_STRING -> return cursor.getString(index)
                Cursor.FIELD_TYPE_NULL -> return null
            }
        }
        return null
    }

    override fun getObject(columnIndex: Int, map: Map<String, Class<*>>): Any {
        throw SQLFeatureNotSupportedException()
    }

    override fun getObject(columnName: String): Any? {
        return getObject(findColumn(columnName))
    }

    override fun getObject(columnName: String, map: Map<String, Class<*>>): Any {
        return getObject(findColumn(columnName), map)
    }

    override fun getRef(columnIndex: Int): Ref {
        throw SQLFeatureNotSupportedException()
    }

    override fun getRef(colName: String): Ref {
        return getRef(findColumn(colName))
    }

    override fun getRow(): Int {
        return cursor.position + 1
    }

    override fun getShort(columnIndex: Int): Short {
        lastColumnIndex = columnIndex
        return cursor.getShort(columnIndex - 1)
    }

    override fun getShort(columnName: String): Short {
        return getShort(findColumn(columnName))
    }

    override fun getStatement(): Statement? {
        return statement
    }

    override fun getString(columnIndex: Int): String? {
        lastColumnIndex = columnIndex
        return if (cursor.isNull(columnIndex - 1)) {
            null
        } else cursor.getString(columnIndex - 1)
    }

    override fun getString(columnName: String): String? {
        return getString(findColumn(columnName))
    }

    override fun getTime(columnIndex: Int): Time? {
        lastColumnIndex = columnIndex
        return if (cursor.isNull(columnIndex - 1)) {
            null
        } else Time(getLong(columnIndex - 1))
    }

    override fun getTime(columnIndex: Int, cal: Calendar): Time? {
        lastColumnIndex = columnIndex
        return if (cursor.isNull(columnIndex - 1)) {
            null
        } else Time(getLong(columnIndex - 1))
    }

    override fun getTime(columnName: String): Time? {
        return getTime(findColumn(columnName))
    }

    override fun getTime(columnName: String, cal: Calendar): Time {
        throw SQLFeatureNotSupportedException()
    }

    override fun getTimestamp(columnIndex: Int): Timestamp? {
        lastColumnIndex = columnIndex
        return if (cursor.isNull(columnIndex - 1)) {
            null
        } else Timestamp(cursor.getLong(columnIndex - 1))
    }

    override fun getTimestamp(columnIndex: Int, cal: Calendar): Timestamp {
        throw SQLFeatureNotSupportedException()
    }

    override fun getTimestamp(columnName: String): Timestamp? {
        return getTimestamp(findColumn(columnName))
    }

    override fun getTimestamp(columnName: String, cal: Calendar): Timestamp {
        throw SQLFeatureNotSupportedException()
    }

    override fun getType(): Int {
        return ResultSet.TYPE_SCROLL_SENSITIVE // allows random access from ResultSetIterator
    }

    override fun getUnicodeStream(columnIndex: Int): InputStream {
        throw SQLFeatureNotSupportedException()
    }

    override fun getUnicodeStream(columnName: String): InputStream {
        throw SQLFeatureNotSupportedException()
    }

    override fun getURL(columnIndex: Int): URL? {
        val value = getString(columnIndex) ?: return null
        try {
            return URL(value)
        } catch (e: MalformedURLException) {
            throw SQLException(e)
        }

    }

    override fun getURL(columnName: String): URL? {
        return getURL(findColumn(columnName))
    }

    override fun getWarnings(): SQLWarning? = null

    override fun insertRow() {
        throw SQLFeatureNotSupportedException()
    }

    override fun isAfterLast(): Boolean {
        return cursor.isAfterLast
    }

    override fun isBeforeFirst(): Boolean {
        return cursor.count != 0 && cursor.isBeforeFirst
    }

    override fun isFirst(): Boolean {
        return cursor.isFirst
    }

    override fun isLast(): Boolean {
        return cursor.isLast || cursor.count == 0
    }

    override fun last(): Boolean {
        return cursor.moveToLast()
    }

    override fun moveToCurrentRow() {}

    override fun moveToInsertRow() {}

    override fun next(): Boolean {
        return cursor.moveToNext()
    }

    override fun previous(): Boolean {
        return cursor.moveToPrevious()
    }

    override fun refreshRow() {}

    override fun relative(rows: Int): Boolean {
        return cursor.move(rows)
    }

    override fun rowDeleted(): Boolean = false

    override fun rowInserted(): Boolean = false

    override fun rowUpdated(): Boolean = false

    override fun setFetchDirection(direction: Int) {
        if (direction != ResultSet.FETCH_FORWARD) {
            throw SQLException("Only FETCH_FORWARD is supported")
        }
    }

    override fun setFetchSize(rows: Int) {}

    override fun wasNull(): Boolean {
        return cursor.isNull(lastColumnIndex - 1)
    }

    override fun getRowId(columnIndex: Int): RowId {
        throw SQLFeatureNotSupportedException()
    }

    override fun getRowId(columnLabel: String): RowId {
        return getRowId(findColumn(columnLabel))
    }

    override fun getHoldability(): Int {
        return ResultSet.HOLD_CURSORS_OVER_COMMIT
    }

    override fun isClosed(): Boolean {
        return cursor.isClosed
    }

    override fun getNClob(columnIndex: Int): NClob {
        throw SQLFeatureNotSupportedException()
    }

    override fun getNClob(columnLabel: String): NClob {
        return getNClob(findColumn(columnLabel))
    }

    override fun getSQLXML(columnIndex: Int): SQLXML {
        throw SQLFeatureNotSupportedException()
    }

    override fun getSQLXML(columnLabel: String): SQLXML {
        return getSQLXML(findColumn(columnLabel))
    }

    override fun getNString(columnIndex: Int): String? {
        return getString(columnIndex)
    }

    override fun getNString(columnLabel: String): String? {
        return getNString(findColumn(columnLabel))
    }

    override fun getNCharacterStream(columnIndex: Int): Reader {
        return getCharacterStream(columnIndex)
    }

    override fun getNCharacterStream(columnLabel: String): Reader {
        return getNCharacterStream(findColumn(columnLabel))
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T> unwrap(iface: Class<T>): T {
        if (iface == Cursor::class.java) {
            return iface.cast(cursor) as T
        }
        throw SQLFeatureNotSupportedException()
    }

    override fun isWrapperFor(iface: Class<*>): Boolean {
        return iface == Cursor::class.java
    }

    override fun getCatalogName(column: Int): String? = null

    override fun getColumnClassName(column: Int): String? = null

    override fun getColumnCount(): Int {
        return cursor.columnCount
    }

    override fun getColumnDisplaySize(column: Int): Int = 0

    override fun getColumnLabel(column: Int): String {
        return ""
    }

    override fun getColumnName(column: Int): String {
        return cursor.getColumnName(column - 1)
    }

    override fun getColumnType(column: Int): Int {
        val type = cursor.getType(column - 1)
        when (type) {
            Cursor.FIELD_TYPE_BLOB -> return Types.VARBINARY
            Cursor.FIELD_TYPE_FLOAT -> return Types.FLOAT
            Cursor.FIELD_TYPE_INTEGER -> return Types.INTEGER
            Cursor.FIELD_TYPE_NULL -> return Types.NULL
            Cursor.FIELD_TYPE_STRING -> return Types.VARCHAR
        }
        return 0
    }

    override fun getColumnTypeName(column: Int): String? = null

    override fun getPrecision(column: Int): Int = 0

    override fun getScale(column: Int): Int = 0

    override fun getSchemaName(column: Int): String? = null

    override fun getTableName(column: Int): String? = null

    override fun isAutoIncrement(column: Int): Boolean = false

    override fun isCaseSensitive(column: Int): Boolean = false

    override fun isCurrency(column: Int): Boolean = false

    override fun isDefinitelyWritable(column: Int): Boolean = false

    override fun isNullable(column: Int): Int = ResultSetMetaData.columnNullableUnknown

    override fun isReadOnly(column: Int): Boolean = false

    override fun isSearchable(column: Int): Boolean = false

    override fun isSigned(column: Int): Boolean = false

    override fun isWritable(column: Int): Boolean = false
}
