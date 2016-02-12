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

import android.database.AbstractCursor;

/**
 * Simple cursor implementation wrapping a single long result.
 *
 * @author Nikhil Purushe
 */
class SingleResultCursor extends AbstractCursor {

    private final String columnName;
    private final long value;

    SingleResultCursor(String columnName, long value) {
        this.columnName = columnName;
        this.value = value;
    }

    @Override
    public int getCount() {
        return 1;
    }

    @Override
    public String[] getColumnNames() {
        if (columnName == null) {
            return new String[0];
        } else {
            return new String[]{columnName};
        }
    }

    @Override
    public String getString(int column) {
        return String.valueOf(getLong(column));
    }

    @Override
    public short getShort(int column) {
        return (short) getLong(column);
    }

    @Override
    public int getInt(int column) {
        return (int) getLong(column);
    }

    @Override
    public long getLong(int column) {
        return value;
    }

    @Override
    public float getFloat(int column) {
        return getLong(column);
    }

    @Override
    public double getDouble(int column) {
        return getLong(column);
    }

    @Override
    public boolean isNull(int column) {
        return false;
    }
}
