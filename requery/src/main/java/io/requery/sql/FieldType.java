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

import javax.annotation.Nullable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Represents a stored SQL type which can be read from a JDBC {@link ResultSet} and written to a
 * JDBC {@link PreparedStatement}.
 *
 * @author Nikhil Purushe
 */
public interface FieldType<T> {

    /**
     * Read the type from a JDBC {@link ResultSet}.
     *
     * @param results result set
     * @param column  column index
     * @return the read value
     * @throws SQLException when failing to read a value from the results
     */
    @Nullable T read(ResultSet results, int column) throws SQLException;

    /**
     * Write the value into a JDBC {@link PreparedStatement}.
     *
     * @param statement statement to use
     * @param index     parameter index
     * @param value     object value
     * @throws SQLException when failing to set the value in the statement
     */
    void write(PreparedStatement statement, int index, @Nullable T value) throws SQLException;

    /**
     * @return One of the {@link java.sql.Types} constants indicating the JDBC type
     */
    int sqlType();

    /**
     * @return true if this data type has a length constraint, false otherwise.
     */
    boolean hasLength();

    /**
     * @return the default length for the type (optional), null if not present.
     */
    @Nullable Integer defaultLength();

    /**
     * @return the identifier for the type can be {@link String} or {@link Keyword}
     */
    Object identifier();

    /**
     * @return the identifier suffix appears after the identifier and any length size e.g.
     * id(255) suffix
     */
    @Nullable String identifierSuffix();
}
