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
import java.sql.Array
import java.sql.Blob
import java.sql.Clob
import java.sql.Date
import java.sql.NClob
import java.sql.Ref
import java.sql.ResultSet
import java.sql.RowId
import java.sql.SQLException
import java.sql.SQLFeatureNotSupportedException
import java.sql.SQLXML
import java.sql.Time
import java.sql.Timestamp

/**
 * Base [ResultSet] that is not updateable.
 */
abstract class NonUpdateableResultSet : ResultSet {

    @Throws(SQLException::class)
    override fun updateArray(columnIndex: Int, x: Array) {
        throw SQLFeatureNotSupportedException()
    }

    @Throws(SQLException::class)
    override fun updateArray(columnName: String, x: Array) {
        throw SQLFeatureNotSupportedException()
    }

    @Throws(SQLException::class)
    override fun updateAsciiStream(columnIndex: Int, x: InputStream, length: Int) {
        throw SQLFeatureNotSupportedException()
    }

    @Throws(SQLException::class)
    override fun updateAsciiStream(columnName: String, x: InputStream, length: Int) {
        throw SQLFeatureNotSupportedException()
    }

    @Throws(SQLException::class)
    override fun updateBigDecimal(columnIndex: Int, x: BigDecimal) {
        throw SQLFeatureNotSupportedException()
    }

    @Throws(SQLException::class)
    override fun updateBigDecimal(columnName: String, x: BigDecimal) {
        throw SQLFeatureNotSupportedException()
    }

    @Throws(SQLException::class)
    override fun updateBinaryStream(columnIndex: Int, x: InputStream, length: Int) {
        throw SQLFeatureNotSupportedException()
    }

    @Throws(SQLException::class)
    override fun updateBinaryStream(columnName: String, x: InputStream, length: Int) {
        throw SQLFeatureNotSupportedException()
    }

    @Throws(SQLException::class)
    override fun updateBlob(columnIndex: Int, x: Blob) {
        throw SQLFeatureNotSupportedException()
    }

    @Throws(SQLException::class)
    override fun updateBlob(columnName: String, x: Blob) {
        throw SQLFeatureNotSupportedException()
    }

    @Throws(SQLException::class)
    override fun updateBoolean(columnIndex: Int, x: Boolean) {
        throw SQLFeatureNotSupportedException()
    }

    @Throws(SQLException::class)
    override fun updateBoolean(columnName: String, x: Boolean) {
        throw SQLFeatureNotSupportedException()
    }

    @Throws(SQLException::class)
    override fun updateByte(columnIndex: Int, x: Byte) {
        throw SQLFeatureNotSupportedException()
    }

    @Throws(SQLException::class)
    override fun updateByte(columnName: String, x: Byte) {
        throw SQLFeatureNotSupportedException()
    }

    @Throws(SQLException::class)
    override fun updateBytes(columnIndex: Int, x: ByteArray) {
        throw SQLFeatureNotSupportedException()
    }

    @Throws(SQLException::class)
    override fun updateBytes(columnName: String, x: ByteArray) {
        throw SQLFeatureNotSupportedException()
    }

    @Throws(SQLException::class)
    override fun updateCharacterStream(columnIndex: Int, x: Reader, length: Int) {
        throw SQLFeatureNotSupportedException()
    }

    @Throws(SQLException::class)
    override fun updateCharacterStream(columnName: String, reader: Reader, length: Int) {
        throw SQLFeatureNotSupportedException()
    }

    @Throws(SQLException::class)
    override fun updateClob(columnIndex: Int, x: Clob) {
        throw SQLFeatureNotSupportedException()
    }

    @Throws(SQLException::class)
    override fun updateClob(columnName: String, x: Clob) {
        throw SQLFeatureNotSupportedException()
    }

    @Throws(SQLException::class)
    override fun updateDate(columnIndex: Int, x: Date) {
        throw SQLFeatureNotSupportedException()
    }

    @Throws(SQLException::class)
    override fun updateDate(columnName: String, x: Date) {
        throw SQLFeatureNotSupportedException()
    }

    @Throws(SQLException::class)
    override fun updateDouble(columnIndex: Int, x: Double) {
        throw SQLFeatureNotSupportedException()
    }

    @Throws(SQLException::class)
    override fun updateDouble(columnName: String, x: Double) {
        throw SQLFeatureNotSupportedException()
    }

    @Throws(SQLException::class)
    override fun updateFloat(columnIndex: Int, x: Float) {
        throw SQLFeatureNotSupportedException()
    }

    @Throws(SQLException::class)
    override fun updateFloat(columnName: String, x: Float) {
        throw SQLFeatureNotSupportedException()
    }

    @Throws(SQLException::class)
    override fun updateInt(columnIndex: Int, x: Int) {
        throw SQLFeatureNotSupportedException()
    }

    @Throws(SQLException::class)
    override fun updateInt(columnName: String, x: Int) {
        throw SQLFeatureNotSupportedException()
    }

    @Throws(SQLException::class)
    override fun updateLong(columnIndex: Int, x: Long) {
        throw SQLFeatureNotSupportedException()
    }

    @Throws(SQLException::class)
    override fun updateLong(columnName: String, x: Long) {
        throw SQLFeatureNotSupportedException()
    }

    @Throws(SQLException::class)
    override fun updateNull(columnIndex: Int) {
        throw SQLFeatureNotSupportedException()
    }

    @Throws(SQLException::class)
    override fun updateNull(columnName: String) {
        throw SQLFeatureNotSupportedException()
    }

    @Throws(SQLException::class)
    override fun updateObject(columnIndex: Int, x: Any) {
        throw SQLFeatureNotSupportedException()
    }

    @Throws(SQLException::class)
    override fun updateObject(columnIndex: Int, x: Any, scale: Int) {
        throw SQLFeatureNotSupportedException()
    }

    @Throws(SQLException::class)
    override fun updateObject(columnName: String, x: Any) {
        throw SQLFeatureNotSupportedException()
    }

    @Throws(SQLException::class)
    override fun updateObject(columnName: String, x: Any, scale: Int) {
        throw SQLFeatureNotSupportedException()
    }

    @Throws(SQLException::class)
    override fun updateRef(columnIndex: Int, x: Ref) {
        throw SQLFeatureNotSupportedException()
    }

    @Throws(SQLException::class)
    override fun updateRef(columnName: String, x: Ref) {
        throw SQLFeatureNotSupportedException()
    }

    @Throws(SQLException::class)
    override fun updateRow() {
        throw SQLFeatureNotSupportedException()
    }

    @Throws(SQLException::class)
    override fun updateShort(columnIndex: Int, x: Short) {
        throw SQLFeatureNotSupportedException()
    }

    @Throws(SQLException::class)
    override fun updateShort(columnName: String, x: Short) {
        throw SQLFeatureNotSupportedException()
    }

    @Throws(SQLException::class)
    override fun updateString(columnIndex: Int, x: String) {
        throw SQLFeatureNotSupportedException()
    }

    @Throws(SQLException::class)
    override fun updateString(columnName: String, x: String) {
        throw SQLFeatureNotSupportedException()
    }

    @Throws(SQLException::class)
    override fun updateTime(columnIndex: Int, x: Time) {
        throw SQLFeatureNotSupportedException()
    }

    @Throws(SQLException::class)
    override fun updateTime(columnName: String, x: Time) {
        throw SQLFeatureNotSupportedException()
    }

    @Throws(SQLException::class)
    override fun updateTimestamp(columnIndex: Int, x: Timestamp) {
        throw SQLFeatureNotSupportedException()
    }

    @Throws(SQLException::class)
    override fun updateTimestamp(columnName: String, x: Timestamp) {
        throw SQLFeatureNotSupportedException()
    }

    @Throws(SQLException::class)
    override fun updateRowId(columnIndex: Int, value: RowId) {
        throw SQLFeatureNotSupportedException()
    }

    @Throws(SQLException::class)
    override fun updateRowId(columnLabel: String, value: RowId) {
        throw SQLFeatureNotSupportedException()
    }

    @Throws(SQLException::class)
    override fun updateNString(columnIndex: Int, nString: String) {
        throw SQLFeatureNotSupportedException()
    }

    @Throws(SQLException::class)
    override fun updateNString(columnLabel: String, nString: String) {
        throw SQLFeatureNotSupportedException()
    }

    @Throws(SQLException::class)
    override fun updateNClob(columnIndex: Int, nClob: NClob) {
        throw SQLFeatureNotSupportedException()
    }

    @Throws(SQLException::class)
    override fun updateNClob(columnLabel: String, nClob: NClob) {
        throw SQLFeatureNotSupportedException()
    }

    @Throws(SQLException::class)
    override fun updateSQLXML(columnIndex: Int, xmlObject: SQLXML) {
        throw SQLFeatureNotSupportedException()
    }

    @Throws(SQLException::class)
    override fun updateSQLXML(columnLabel: String, xmlObject: SQLXML) {
        throw SQLFeatureNotSupportedException()
    }

    @Throws(SQLException::class)
    override fun updateNCharacterStream(columnIndex: Int, x: Reader, length: Long) {
        throw SQLFeatureNotSupportedException()
    }

    @Throws(SQLException::class)
    override fun updateNCharacterStream(columnLabel: String, reader: Reader, length: Long) {
        throw SQLFeatureNotSupportedException()
    }

    @Throws(SQLException::class)
    override fun updateAsciiStream(columnIndex: Int, x: InputStream, length: Long) {
        throw SQLFeatureNotSupportedException()
    }

    @Throws(SQLException::class)
    override fun updateBinaryStream(columnIndex: Int, x: InputStream, length: Long) {
        throw SQLFeatureNotSupportedException()
    }

    @Throws(SQLException::class)
    override fun updateCharacterStream(columnIndex: Int, x: Reader, length: Long) {
        throw SQLFeatureNotSupportedException()
    }

    @Throws(SQLException::class)
    override fun updateAsciiStream(columnLabel: String, x: InputStream, length: Long) {
        throw SQLFeatureNotSupportedException()
    }

    @Throws(SQLException::class)
    override fun updateBinaryStream(columnLabel: String, x: InputStream, length: Long) {
        throw SQLFeatureNotSupportedException()
    }

    @Throws(SQLException::class)
    override fun updateCharacterStream(columnLabel: String, reader: Reader, length: Long) {
        throw SQLFeatureNotSupportedException()
    }

    @Throws(SQLException::class)
    override fun updateBlob(columnIndex: Int, inputStream: InputStream, length: Long) {
        throw SQLFeatureNotSupportedException()
    }

    @Throws(SQLException::class)
    override fun updateBlob(columnLabel: String, inputStream: InputStream, length: Long) {
        throw SQLFeatureNotSupportedException()
    }

    @Throws(SQLException::class)
    override fun updateClob(columnIndex: Int, reader: Reader, length: Long) {
        throw SQLFeatureNotSupportedException()
    }

    @Throws(SQLException::class)
    override fun updateClob(columnLabel: String, reader: Reader, length: Long) {
        throw SQLFeatureNotSupportedException()
    }

    @Throws(SQLException::class)
    override fun updateNClob(columnIndex: Int, reader: Reader, length: Long) {
        throw SQLFeatureNotSupportedException()
    }

    @Throws(SQLException::class)
    override fun updateNClob(columnLabel: String, reader: Reader, length: Long) {
        throw SQLFeatureNotSupportedException()
    }

    @Throws(SQLException::class)
    override fun updateNCharacterStream(columnIndex: Int, x: Reader) {
        throw SQLFeatureNotSupportedException()
    }

    @Throws(SQLException::class)
    override fun updateNCharacterStream(columnLabel: String, reader: Reader) {
        throw SQLFeatureNotSupportedException()
    }

    @Throws(SQLException::class)
    override fun updateAsciiStream(columnIndex: Int, x: InputStream) {
        throw SQLFeatureNotSupportedException()
    }

    @Throws(SQLException::class)
    override fun updateBinaryStream(columnIndex: Int, x: InputStream) {
        throw SQLFeatureNotSupportedException()
    }

    @Throws(SQLException::class)
    override fun updateCharacterStream(columnIndex: Int, x: Reader) {
        throw SQLFeatureNotSupportedException()
    }

    @Throws(SQLException::class)
    override fun updateAsciiStream(columnLabel: String, x: InputStream) {
        throw SQLFeatureNotSupportedException()
    }

    @Throws(SQLException::class)
    override fun updateBinaryStream(columnLabel: String, x: InputStream) {
        throw SQLFeatureNotSupportedException()
    }

    @Throws(SQLException::class)
    override fun updateCharacterStream(columnLabel: String, reader: Reader) {
        throw SQLFeatureNotSupportedException()
    }

    @Throws(SQLException::class)
    override fun updateBlob(columnIndex: Int, inputStream: InputStream) {
        throw SQLFeatureNotSupportedException()
    }

    @Throws(SQLException::class)
    override fun updateBlob(columnLabel: String, inputStream: InputStream) {
        throw SQLFeatureNotSupportedException()
    }

    @Throws(SQLException::class)
    override fun updateClob(columnIndex: Int, reader: Reader) {
        throw SQLFeatureNotSupportedException()
    }

    @Throws(SQLException::class)
    override fun updateClob(columnLabel: String, reader: Reader) {
        throw SQLFeatureNotSupportedException()
    }

    @Throws(SQLException::class)
    override fun updateNClob(columnIndex: Int, reader: Reader) {
        throw SQLFeatureNotSupportedException()
    }

    @Throws(SQLException::class)
    override fun updateNClob(columnLabel: String, reader: Reader) {
        throw SQLFeatureNotSupportedException()
    }
}
