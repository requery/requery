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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Blob;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;

class ByteArrayBlob implements Blob {

    private byte[] blob;
    private boolean freed;

    ByteArrayBlob(byte[] blob) {
        this.blob = blob;
    }

    private void throwIfFreed() throws SQLException {
        if(freed) {
            throw new SQLException("blob freed");
        }
    }

    @Override
    public InputStream getBinaryStream() throws SQLException {
        throwIfFreed();
        return new ByteArrayInputStream(blob);
    }

    @Override
    public InputStream getBinaryStream(long pos, long length) throws SQLException {
        if(pos > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("invalid position");
        }
        if(length > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("invalid position");
        }
        throwIfFreed();
        return new ByteArrayInputStream(blob, (int)pos, (int)length);
    }

    @Override
    public byte[] getBytes(long pos, int length) throws SQLException {
        if(pos > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("invalid position");
        }
        if(pos == 0 && length == blob.length) {
            return blob;
        }
        throwIfFreed();
        byte[] bytes = new byte[length];
        System.arraycopy(blob, (int)pos, bytes, 0, length);
        return bytes;
    }

    @Override
    public long length() throws SQLException {
        throwIfFreed();
        return blob.length;
    }

    @Override
    public long position(Blob pattern, long start) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public long position(byte[] pattern, long start) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public OutputStream setBinaryStream(long pos) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public int setBytes(long pos, byte[] theBytes) throws SQLException {
        if(pos > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("invalid position");
        }
        throwIfFreed();
        System.arraycopy(theBytes, 0, blob, (int)pos, theBytes.length);
        return theBytes.length;
    }

    @Override
    public int setBytes(long pos, byte[] theBytes, int offset, int len) throws SQLException {
        if(pos > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("invalid position");
        }
        throwIfFreed();
        System.arraycopy(theBytes, offset, blob, (int)pos, len);
        return len;
    }

    @Override
    public void truncate(long len) throws SQLException {
        throwIfFreed();
        byte[] truncated = new byte[(int)len];
        System.arraycopy(blob, 0, truncated, 0, truncated.length);
        blob = truncated;
    }

    @Override
    public void free() throws SQLException {
        blob = null;
        freed = true;
    }
}
