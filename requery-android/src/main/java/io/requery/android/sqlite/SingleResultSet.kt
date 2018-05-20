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

import java.io.InputStream
import java.io.Reader
import java.math.BigDecimal
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
import java.sql.SQLException
import java.sql.SQLFeatureNotSupportedException
import java.sql.SQLWarning
import java.sql.SQLXML
import java.sql.Statement
import java.sql.Time
import java.sql.Timestamp
import java.util.Calendar

@Suppress("OverridingDeprecatedMember")
class SingleResultSet @Throws(SQLException::class)
constructor(private val statement: Statement, private val value: Long) : NonUpdateableResultSet() {

    init {
        if (value == -1L) {
            throw SQLException("invalid row id")
        }
    }

    override fun absolute(row: Int): Boolean = row == 1

    override fun afterLast() {}

    override fun beforeFirst() {}

    override fun cancelRowUpdates() {}

    override fun clearWarnings() {}

    override fun close() {}

    override fun deleteRow() {}

    override fun findColumn(columnName: String): Int = 1

    override fun first(): Boolean = true

    override fun getArray(columnIndex: Int): Array? = null

    override fun getArray(colName: String): Array? = null

    override fun getAsciiStream(columnIndex: Int): InputStream? = null

    override fun getAsciiStream(columnName: String): InputStream? = null

    override fun getBigDecimal(columnIndex: Int): BigDecimal? = null

    override fun getBigDecimal(columnIndex: Int, scale: Int): BigDecimal? = null

    override fun getBigDecimal(columnName: String): BigDecimal? = null

    override fun getBigDecimal(columnName: String, scale: Int): BigDecimal? = null

    override fun getBinaryStream(columnIndex: Int): InputStream? = null

    override fun getBinaryStream(columnName: String): InputStream? = null

    override fun getBlob(columnIndex: Int): Blob? = null

    override fun getBlob(columnName: String): Blob? = null

    override fun getBoolean(columnIndex: Int): Boolean = false

    override fun getBoolean(columnName: String): Boolean = false

    override fun getByte(columnIndex: Int): Byte = 0

    override fun getByte(columnName: String): Byte = 0

    override fun getBytes(columnIndex: Int): ByteArray? = null

    override fun getBytes(columnName: String): ByteArray? = null

    override fun getCharacterStream(columnIndex: Int): Reader? = null

    override fun getCharacterStream(columnName: String): Reader? = null

    override fun getClob(columnIndex: Int): Clob? = null

    override fun getClob(colName: String): Clob? = null

    override fun getConcurrency(): Int = ResultSet.CONCUR_READ_ONLY

    override fun getCursorName(): String? = null

    override fun getDate(columnIndex: Int): Date? = null

    override fun getDate(columnIndex: Int, cal: Calendar): Date? = null

    override fun getDate(columnName: String): Date? = null

    override fun getDate(columnName: String, cal: Calendar): Date? = null

    override fun getDouble(columnIndex: Int): Double = 0.0

    override fun getDouble(columnName: String): Double = 0.0

    override fun getFetchDirection(): Int = ResultSet.FETCH_FORWARD

    override fun getFetchSize(): Int = 0

    override fun getFloat(columnIndex: Int): Float = 0f

    override fun getFloat(columnName: String): Float = 0f

    override fun getInt(columnIndex: Int): Int = value.toInt()

    override fun getInt(columnName: String): Int = value.toInt()

    override fun getLong(columnIndex: Int): Long = value

    override fun getLong(columnName: String): Long = value

    override fun getMetaData(): ResultSetMetaData? = null

    override fun getObject(columnIndex: Int): Any = value

    override fun getObject(columnIndex: Int, map: Map<String, Class<*>>): Any = value

    override fun getObject(columnName: String): Any = value

    override fun getObject(columnName: String, map: Map<String, Class<*>>): Any = value

    override fun getRef(columnIndex: Int): Ref? = null

    override fun getRef(colName: String): Ref? = null

    override fun getRow(): Int = 0

    override fun getShort(columnIndex: Int): Short = value.toShort()

    override fun getShort(columnName: String): Short = value.toShort()

    override fun getStatement(): Statement = statement

    override fun getString(columnIndex: Int): String = value.toString()

    override fun getString(columnName: String): String = value.toString()

    override fun getTime(columnIndex: Int): Time? = null

    override fun getTime(columnIndex: Int, cal: Calendar): Time? = null

    override fun getTime(columnName: String): Time? = null

    override fun getTime(columnName: String, cal: Calendar): Time? = null

    override fun getTimestamp(columnIndex: Int): Timestamp? = null

    override fun getTimestamp(columnIndex: Int, cal: Calendar): Timestamp? = null

    override fun getTimestamp(columnName: String): Timestamp? = null

    override fun getTimestamp(columnName: String, cal: Calendar): Timestamp? = null

    override fun getType(): Int = ResultSet.TYPE_FORWARD_ONLY

    override fun getUnicodeStream(columnIndex: Int): InputStream? = null

    override fun getUnicodeStream(columnName: String): InputStream? = null

    override fun getURL(columnIndex: Int): URL? = null

    override fun getURL(columnName: String): URL? = null

    override fun getWarnings(): SQLWarning? = null

    override fun insertRow() {}

    override fun isAfterLast(): Boolean = false

    override fun isBeforeFirst(): Boolean = false

    override fun isFirst(): Boolean = false

    override fun isLast(): Boolean = true

    override fun last(): Boolean = false

    @Throws(SQLException::class)
    override fun moveToCurrentRow() {
        throw SQLFeatureNotSupportedException()
    }

    @Throws(SQLException::class)
    override fun moveToInsertRow() {
        throw SQLFeatureNotSupportedException()
    }

    override fun next(): Boolean = true

    override fun previous(): Boolean = false

    override fun refreshRow() {}

    override fun relative(rows: Int): Boolean = false

    override fun rowDeleted(): Boolean = false

    override fun rowInserted(): Boolean = false

    override fun rowUpdated(): Boolean = false

    override fun setFetchDirection(direction: Int) {}

    override fun setFetchSize(rows: Int) {}

    override fun wasNull(): Boolean = false

    override fun getRowId(columnIndex: Int): RowId? = null

    override fun getRowId(columnLabel: String): RowId? = null

    override fun getHoldability(): Int = ResultSet.CLOSE_CURSORS_AT_COMMIT

    override fun isClosed(): Boolean = false

    override fun getNClob(columnIndex: Int): NClob? = null

    override fun getNClob(columnLabel: String): NClob? = null

    override fun getSQLXML(columnIndex: Int): SQLXML? = null

    override fun getSQLXML(columnLabel: String): SQLXML? = null

    override fun getNString(columnIndex: Int): String? = null

    override fun getNString(columnLabel: String): String? = null

    override fun getNCharacterStream(columnIndex: Int): Reader? = null

    override fun getNCharacterStream(columnLabel: String): Reader? = null

    override fun <T> unwrap(iface: Class<T>): T? = null

    override fun isWrapperFor(iface: Class<*>): Boolean = false
}
