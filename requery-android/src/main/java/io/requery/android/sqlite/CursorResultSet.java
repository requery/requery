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

import android.database.Cursor;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Date;
import java.sql.NClob;
import java.sql.Ref;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.RowId;
import java.sql.SQLDataException;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.Calendar;
import java.util.Map;

/**
 * {@link ResultSet} implementation using Android's {@link Cursor} interface.
 *
 * @author Nikhil Purushe
 */
public class CursorResultSet extends NonUpdateableResultSet implements ResultSetMetaData {

    private final Statement statement;
    private final Cursor cursor;
    private final boolean closeCursor;
    private int lastColumnIndex;

    public CursorResultSet(Statement statement, Cursor cursor, boolean closeCursor) {
        if(cursor == null) {
            throw new IllegalArgumentException("null cursor");
        }
        this.statement = statement;
        this.cursor = cursor;
        this.closeCursor = closeCursor;
        cursor.moveToPosition(-1);
    }

    @Override
    public boolean absolute(int row) throws SQLException {
        return cursor.moveToPosition(row - 1);
    }

    @Override
    public void afterLast() throws SQLException {
        cursor.moveToLast();
        cursor.moveToNext();
    }

    @Override
    public void beforeFirst() throws SQLException {
        cursor.moveToPosition(-1);
    }

    @Override
    public void cancelRowUpdates() throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public void clearWarnings() throws SQLException {

    }

    @Override
    public void close() throws SQLException {
        if (closeCursor) {
            cursor.close();
        }
    }

    @Override
    public void deleteRow() throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public int findColumn(String columnName) throws SQLException {
        int index = cursor.getColumnIndex(columnName);
        if (index == -1) {
            throw new SQLDataException("no column " + columnName);
        }
        return index + 1;
    }

    @Override
    public boolean first() throws SQLException {
        return cursor.moveToFirst();
    }

    @Override
    public Array getArray(int columnIndex) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public Array getArray(String colName) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public InputStream getAsciiStream(int columnIndex) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public InputStream getAsciiStream(String columnName) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public BigDecimal getBigDecimal(int columnIndex) throws SQLException {
        String value = getString(columnIndex);
        return value == null ? null : new BigDecimal(value);
    }

    @Override
    public BigDecimal getBigDecimal(int columnIndex, int scale) throws SQLException {
        String value = getString(columnIndex);
        BigDecimal result = value == null ? null : new BigDecimal(value);
        if(result != null) {
            result = result.setScale(scale, BigDecimal.ROUND_DOWN);
        }
        return result;
    }

    @Override
    public BigDecimal getBigDecimal(String columnName) throws SQLException {
        return getBigDecimal(findColumn(columnName));
    }

    @Override
    public BigDecimal getBigDecimal(String columnName, int scale) throws SQLException {
        return getBigDecimal(findColumn(columnName));
    }

    @Override
    public InputStream getBinaryStream(int columnIndex) throws SQLException {
        return new ByteArrayInputStream(getBytes(columnIndex));
    }

    @Override
    public InputStream getBinaryStream(String columnName) throws SQLException {
        return getBinaryStream(findColumn(columnName));
    }

    @Override
    public Blob getBlob(int columnIndex) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public Blob getBlob(String columnName) throws SQLException {
        return getBlob(findColumn(columnName));
    }

    @Override
    public boolean getBoolean(int columnIndex) throws SQLException {
        return getInt(columnIndex) > 0;
    }

    @Override
    public boolean getBoolean(String columnName) throws SQLException {
        return getBoolean(findColumn(columnName));
    }

    @Override
    public byte getByte(int columnIndex) throws SQLException {
        return (byte) getShort(columnIndex);
    }

    @Override
    public byte getByte(String columnName) throws SQLException {
        return getByte(findColumn(columnName));
    }

    @Override
    public byte[] getBytes(int columnIndex) throws SQLException {
        lastColumnIndex = columnIndex;
        return cursor.getBlob(columnIndex - 1);
    }

    @Override
    public byte[] getBytes(String columnName) throws SQLException {
        return getBytes(findColumn(columnName));
    }

    @Override
    public Reader getCharacterStream(int columnIndex) throws SQLException {
        return new StringReader(getString(columnIndex));
    }

    @Override
    public Reader getCharacterStream(String columnName) throws SQLException {
        return new StringReader(getString(columnName));
    }

    @Override
    public Clob getClob(int columnIndex) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public Clob getClob(String colName) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public int getConcurrency() throws SQLException {
        return CONCUR_READ_ONLY;
    }

    @Override
    public String getCursorName() throws SQLException {
        return null;
    }

    @Override
    public Date getDate(int columnIndex) throws SQLException {
        lastColumnIndex = columnIndex;
        if(cursor.isNull(columnIndex - 1)) {
            return null;
        }
        return new Date(cursor.getLong(columnIndex - 1));
    }

    @Override
    public Date getDate(int columnIndex, Calendar cal) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public Date getDate(String columnName) throws SQLException {
        return getDate(findColumn(columnName));
    }

    @Override
    public Date getDate(String columnName, Calendar cal) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public double getDouble(int columnIndex) throws SQLException {
        lastColumnIndex = columnIndex;
        return cursor.getDouble(columnIndex - 1);
    }

    @Override
    public double getDouble(String columnName) throws SQLException {
        return getDouble(findColumn(columnName));
    }

    @Override
    public int getFetchDirection() throws SQLException {
        return FETCH_FORWARD;
    }

    @Override
    public int getFetchSize() throws SQLException {
        return 0;
    }

    @Override
    public float getFloat(int columnIndex) throws SQLException {
        lastColumnIndex = columnIndex;
        return cursor.getFloat(columnIndex - 1);
    }

    @Override
    public float getFloat(String columnName) throws SQLException {
        return getFloat(findColumn(columnName));
    }

    @Override
    public int getInt(int columnIndex) throws SQLException {
        lastColumnIndex = columnIndex;
        return cursor.getInt(columnIndex - 1);
    }

    @Override
    public int getInt(String columnName) throws SQLException {
        return getInt(findColumn(columnName));
    }

    @Override
    public long getLong(int columnIndex) throws SQLException {
        lastColumnIndex = columnIndex;
        return cursor.getLong(columnIndex - 1);
    }

    @Override
    public long getLong(String columnName) throws SQLException {
        return getLong(findColumn(columnName));
    }

    @Override
    public ResultSetMetaData getMetaData() throws SQLException {
        return this;
    }

    @Override
    public Object getObject(int columnIndex) throws SQLException {
        lastColumnIndex = columnIndex;
        columnIndex = columnIndex - 1;
        int type = cursor.getType(columnIndex);
        if(!cursor.isNull(columnIndex)) {
            switch (type) {
                case Cursor.FIELD_TYPE_BLOB:
                    return cursor.getBlob(columnIndex);
                case Cursor.FIELD_TYPE_FLOAT:
                    return cursor.getFloat(columnIndex);
                case Cursor.FIELD_TYPE_INTEGER:
                    return cursor.getInt(columnIndex);
                case Cursor.FIELD_TYPE_NULL:
                    return null;
                case Cursor.FIELD_TYPE_STRING:
                    return cursor.getString(columnIndex);
            }
        }
        return null;
    }

    @Override
    public Object getObject(int columnIndex, Map<String, Class<?>> map) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public Object getObject(String columnName) throws SQLException {
        return getObject(findColumn(columnName));
    }

    @Override
    public Object getObject(String columnName, Map<String, Class<?>> map) throws SQLException {
        return getObject(findColumn(columnName), map);
    }

    @Override
    public Ref getRef(int columnIndex) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public Ref getRef(String colName) throws SQLException {
        return getRef(findColumn(colName));
    }

    @Override
    public int getRow() throws SQLException {
        return cursor.getPosition() + 1;
    }

    @Override
    public short getShort(int columnIndex) throws SQLException {
        lastColumnIndex = columnIndex;
        return cursor.getShort(columnIndex - 1);
    }

    @Override
    public short getShort(String columnName) throws SQLException {
        return getShort(findColumn(columnName));
    }

    @Override
    public Statement getStatement() throws SQLException {
        return statement;
    }

    @Override
    public String getString(int columnIndex) throws SQLException {
        lastColumnIndex = columnIndex;
        if(cursor.isNull(columnIndex - 1)) {
            return null;
        }
        return cursor.getString(columnIndex - 1);
    }

    @Override
    public String getString(String columnName) throws SQLException {
        return getString(findColumn(columnName));
    }

    @Override
    public Time getTime(int columnIndex) throws SQLException {
        lastColumnIndex = columnIndex;
        if(cursor.isNull(columnIndex - 1)) {
            return null;
        }
        return new Time(getLong(columnIndex - 1));
    }

    @Override
    public Time getTime(int columnIndex, Calendar cal) throws SQLException {
        lastColumnIndex = columnIndex;
        if(cursor.isNull(columnIndex - 1)) {
            return null;
        }
        return new Time(getLong(columnIndex - 1));
    }

    @Override
    public Time getTime(String columnName) throws SQLException {
        return getTime(findColumn(columnName));
    }

    @Override
    public Time getTime(String columnName, Calendar cal) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public Timestamp getTimestamp(int columnIndex) throws SQLException {
        lastColumnIndex = columnIndex;
        if(cursor.isNull(columnIndex - 1)) {
            return null;
        }
        return new Timestamp(cursor.getLong(columnIndex - 1));
    }

    @Override
    public Timestamp getTimestamp(int columnIndex, Calendar cal) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public Timestamp getTimestamp(String columnName) throws SQLException {
        return getTimestamp(findColumn(columnName));
    }

    @Override
    public Timestamp getTimestamp(String columnName, Calendar cal) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public int getType() throws SQLException {
        return TYPE_SCROLL_SENSITIVE; // allows random access from ResultSetIterator
    }

    @Override
    public InputStream getUnicodeStream(int columnIndex) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public InputStream getUnicodeStream(String columnName) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public URL getURL(int columnIndex) throws SQLException {
        String value = getString(columnIndex);
        if(value == null) {
            return null;
        }
        try {
            return new URL(value);
        } catch (MalformedURLException e) {
            throw new SQLException(e);
        }
    }

    @Override
    public URL getURL(String columnName) throws SQLException {
        return getURL(findColumn(columnName));
    }

    @Override
    public SQLWarning getWarnings() throws SQLException {
        return null;
    }

    @Override
    public void insertRow() throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public boolean isAfterLast() throws SQLException {
        return cursor.isAfterLast();
    }

    @Override
    public boolean isBeforeFirst() throws SQLException {
        return cursor.getCount() != 0 && cursor.isBeforeFirst();
    }

    @Override
    public boolean isFirst() throws SQLException {
        return cursor.isFirst();
    }

    @Override
    public boolean isLast() throws SQLException {
        return cursor.isLast() || cursor.getCount() == 0;
    }

    @Override
    public boolean last() throws SQLException {
        return cursor.moveToLast();
    }

    @Override
    public void moveToCurrentRow() throws SQLException {

    }

    @Override
    public void moveToInsertRow() throws SQLException {

    }

    @Override
    public boolean next() throws SQLException {
        return cursor.moveToNext();
    }

    @Override
    public boolean previous() throws SQLException {
        return cursor.moveToPrevious();
    }

    @Override
    public void refreshRow() throws SQLException {

    }

    @Override
    public boolean relative(int rows) throws SQLException {
        return cursor.move(rows);
    }

    @Override
    public boolean rowDeleted() throws SQLException {
        return false;
    }

    @Override
    public boolean rowInserted() throws SQLException {
        return false;
    }

    @Override
    public boolean rowUpdated() throws SQLException {
        return false;
    }

    @Override
    public void setFetchDirection(int direction) throws SQLException {
        if(direction != ResultSet.FETCH_FORWARD) {
            throw new SQLException("Only FETCH_FORWARD is supported");
        }
    }

    @Override
    public void setFetchSize(int rows) throws SQLException {

    }

    @Override
    public boolean wasNull() throws SQLException {
        return cursor.isNull(lastColumnIndex - 1);
    }

    @Override
    public RowId getRowId(int columnIndex) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public RowId getRowId(String columnLabel) throws SQLException {
        return getRowId(findColumn(columnLabel));
    }

    @Override
    public int getHoldability() throws SQLException {
        return HOLD_CURSORS_OVER_COMMIT;
    }

    @Override
    public boolean isClosed() throws SQLException {
        return cursor.isClosed();
    }

    @Override
    public NClob getNClob(int columnIndex) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public NClob getNClob(String columnLabel) throws SQLException {
        return getNClob(findColumn(columnLabel));
    }

    @Override
    public SQLXML getSQLXML(int columnIndex) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public SQLXML getSQLXML(String columnLabel) throws SQLException {
        return getSQLXML(findColumn(columnLabel));
    }

    @Override
    public String getNString(int columnIndex) throws SQLException {
        return getString(columnIndex);
    }

    @Override
    public String getNString(String columnLabel) throws SQLException {
        return getNString(findColumn(columnLabel));
    }

    @Override
    public Reader getNCharacterStream(int columnIndex) throws SQLException {
        return getCharacterStream(columnIndex);
    }

    @Override
    public Reader getNCharacterStream(String columnLabel) throws SQLException {
        return getNCharacterStream(findColumn(columnLabel));
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        if(iface == Cursor.class) {
            return iface.cast(cursor);
        }
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return iface == Cursor.class;
    }

    @Override
    public String getCatalogName(int column) throws SQLException {
        return null;
    }

    @Override
    public String getColumnClassName(int column) throws SQLException {
        return null;
    }

    @Override
    public int getColumnCount() throws SQLException {
        return cursor.getColumnCount();
    }

    @Override
    public int getColumnDisplaySize(int column) throws SQLException {
        return 0;
    }

    @Override
    public String getColumnLabel(int column) throws SQLException {
        return "";
    }

    @Override
    public String getColumnName(int column) throws SQLException {
        return cursor.getColumnName(column - 1);
    }

    @Override
    public int getColumnType(int column) throws SQLException {
        int type = cursor.getType(column - 1);
        switch (type) {
            case Cursor.FIELD_TYPE_BLOB:
                return Types.VARBINARY;
            case Cursor.FIELD_TYPE_FLOAT:
                return Types.FLOAT;
            case Cursor.FIELD_TYPE_INTEGER:
                return Types.INTEGER;
            case Cursor.FIELD_TYPE_NULL:
                return Types.NULL;
            case Cursor.FIELD_TYPE_STRING:
                return Types.VARCHAR;
        }
        return 0;
    }

    @Override
    public String getColumnTypeName(int column) throws SQLException {
        return null;
    }

    @Override
    public int getPrecision(int column) throws SQLException {
        return 0;
    }

    @Override
    public int getScale(int column) throws SQLException {
        return 0;
    }

    @Override
    public String getSchemaName(int column) throws SQLException {
        return null;
    }

    @Override
    public String getTableName(int column) throws SQLException {
        return null;
    }

    @Override
    public boolean isAutoIncrement(int column) throws SQLException {
        return false;
    }

    @Override
    public boolean isCaseSensitive(int column) throws SQLException {
        return false;
    }

    @Override
    public boolean isCurrency(int column) throws SQLException {
        return false;
    }

    @Override
    public boolean isDefinitelyWritable(int column) throws SQLException {
        return false;
    }

    @Override
    public int isNullable(int column) throws SQLException {
        return columnNullableUnknown;
    }

    @Override
    public boolean isReadOnly(int column) throws SQLException {
        return false;
    }

    @Override
    public boolean isSearchable(int column) throws SQLException {
        return false;
    }

    @Override
    public boolean isSigned(int column) throws SQLException {
        return false;
    }

    @Override
    public boolean isWritable(int column) throws SQLException {
        return false;
    }
}
