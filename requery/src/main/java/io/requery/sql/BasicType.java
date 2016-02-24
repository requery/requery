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

package io.requery.sql;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Reads a result using a specific {@link ResultSet} get method rather than
 * {@link ResultSet#getObject(int)}.
 *
 * @param <T> java type
 */
public abstract class BasicType<T> extends BaseType<T> {

    private final boolean checkNull;

    /**
     * Instantiates a new type instance.
     *
     * @param type    java class type being mapped
     * @param sqlType the {@link java.sql.Types} being mapped
     */
    protected BasicType(Class<T> type, int sqlType) {
        super(type, sqlType);
        checkNull = !type.isPrimitive();
    }

    /**
     * Reads a result using a specific {@link ResultSet} get method rather than
     * {@link ResultSet#getObject(int)}.
     *
     * @param results {@link ResultSet} set to the index of the result to be read
     * @param column  column index to read
     * @return the type from the {@link ResultSet}
     * @throws SQLException
     */
    public abstract T fromResult(ResultSet results, int column) throws SQLException;

    @Override
    public T read(ResultSet results, int column) throws SQLException {
        T result = fromResult(results, column);
        if (checkNull && results.wasNull()) {
            return null;
        }
        return result;
    }

    @Override
    public abstract Keyword identifier();
}
