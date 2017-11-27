/*
 * Copyright 2017 requery.io
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

import io.requery.meta.Attribute;
import io.requery.query.Expression;
import io.requery.query.function.Function;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Set;

/**
 * Defines the mapping between java class types and {@link FieldType} instances which determines
 * how they are stored in a SQL database.
 *
 * @author Nikhil Purushe
 */
public interface Mapping {

    /**
     * Map a specific java class type to a {@link FieldType} instance.
     *
     * @param type     non null java class type
     * @param fieldType non null storage type
     * @param <T>      the mapped type
     * @return mapping instance
     */
    <T> Mapping putType(Class<? super T> type, FieldType<T> fieldType);

    /**
     * Replace the default mapping from a {@link FieldType} to a another {@link FieldType}. If the
     * basic type is not mapped then a mapping will be added.
     *
     * @param sqlType         {@link java.sql.Types} to replace
     * @param replacementType non null replacement type
     * @param <T>             the mapped type
     * @return mapping instance
     */
    <T> Mapping replaceType(int sqlType, FieldType<T> replacementType);

    /**
     * Adds an alias for a function, either changing it's default name.
     * @param name overriding name of the function
     * @param function function class type
     * @return mapping instance
     */
    Mapping aliasFunction(Function.Name name, Class<? extends Function> function);

    /**
     * Get the mapped storage type mapping for a given type {@link Attribute}.
     *
     * @param attribute attribute non null attribute
     * @return the mapped {@link FieldType}
     */
    FieldType mapAttribute(Attribute<?, ?> attribute);

    /**
     * Get the mapped function name for the given function.
     *
     * @param function to map
     * @return final function name
     */
    Function.Name mapFunctionName(Function<?> function);

    /**
     * Get the mapped storage type mappings for a given sql type.
     *
     * @param sqlType {@link java.sql.Types} type
     * @return the mapped class(es) for the given sql type or empty set if none.
     */
    Set<Class<?>> typesOf(int sqlType);

    /**
     * Given the expression read it from {@link ResultSet} instance.
     *
     * @param expression expression to read
     * @param results    {@link ResultSet} instance
     * @param column     column index
     * @param <A>        type to read
     * @return the read type.
     * @throws SQLException on a failure to read from the {@link ResultSet}
     */
    <A> A read(Expression<A> expression, ResultSet results, int column) throws SQLException;

    /**
     * Reads a boolean value.
     *
     * @param results {@link ResultSet} instance
     * @param column  column index
     * @return read value
     * @throws SQLException on a failure to read from the {@link ResultSet}
     */
    boolean readBoolean(ResultSet results, int column) throws SQLException;

    /**
     * Reads a byte value.
     *
     * @param results {@link ResultSet} instance
     * @param column  column index
     * @return read value
     * @throws SQLException on a failure to read from the {@link ResultSet}
     */
    byte readByte(ResultSet results, int column) throws SQLException;

    /**
     * Reads a boolean value.
     *
     * @param results {@link ResultSet} instance
     * @param column  column index
     * @return read value
     * @throws SQLException on a failure to read from the {@link ResultSet}
     */
    short readShort(ResultSet results, int column) throws SQLException;

    /**
     * Reads a integer value.
     *
     * @param results {@link ResultSet} instance
     * @param column  column index
     * @return read value
     * @throws SQLException on a failure to read from the {@link ResultSet}
     */
    int readInt(ResultSet results, int column) throws SQLException;

    /**
     * Reads a long value.
     *
     * @param results {@link ResultSet} instance
     * @param column  column index
     * @return read value
     * @throws SQLException on a failure to read from the {@link ResultSet}
     */
    long readLong(ResultSet results, int column) throws SQLException;

    /**
     * Reads a float value.
     *
     * @param results {@link ResultSet} instance
     * @param column  column index
     * @return read value
     * @throws SQLException on a failure to read from the {@link ResultSet}
     */
    float readFloat(ResultSet results, int column) throws SQLException;

    /**
     * Reads a double value.
     *
     * @param results {@link ResultSet} instance
     * @param column  column index
     * @return read value
     * @throws SQLException on a failure to read from the {@link ResultSet}
     */
    double readDouble(ResultSet results, int column) throws SQLException;

    /**
     * Given the expression bind it into a JDBC {@link PreparedStatement}.
     *
     * @param expression to bind
     * @param statement  prepared statement instance
     * @param index      statement index
     * @param value      value
     * @param <A>        type to be bound
     * @throws SQLException on a failure to set the expression on the {@link PreparedStatement}
     */
    <A> void write(Expression<A> expression, PreparedStatement statement, int index, A value)
        throws SQLException;

    /**
     * Sets a boolean value
     *
     * @param statement prepared statement
     * @param index     statement index
     * @param value     type to be bound
     * @throws SQLException on a failure to set the expression on the {@link PreparedStatement}
     */
    void writeBoolean(PreparedStatement statement, int index, boolean value) throws SQLException;

    /**
     * Sets a byte value
     *
     * @param statement prepared statement
     * @param index     statement index
     * @param value     type to be bound
     * @throws SQLException on a failure to set the expression on the {@link PreparedStatement}
     */
    void writeByte(PreparedStatement statement, int index, byte value) throws SQLException;

    /**
     * Sets a short value
     *
     * @param statement prepared statement
     * @param index     statement index
     * @param value     type to be bound
     * @throws SQLException on a failure to set the expression on the {@link PreparedStatement}
     */
    void writeShort(PreparedStatement statement, int index, short value) throws SQLException;

    /**
     * Sets a int value
     *
     * @param statement prepared statement
     * @param index     statement index
     * @param value     type to be bound
     * @throws SQLException on a failure to set the expression on the {@link PreparedStatement}
     */
    void writeInt(PreparedStatement statement, int index, int value) throws SQLException;

    /**
     * Sets a long value
     *
     * @param statement prepared statement
     * @param index     statement index
     * @param value     type to be bound
     * @throws SQLException on a failure to set the expression on the {@link PreparedStatement}
     */
    void writeLong(PreparedStatement statement, int index, long value) throws SQLException;

    /**
     * Sets a float value
     *
     * @param statement prepared statement
     * @param index     statement index
     * @param value     type to be bound
     * @throws SQLException on a failure to set the expression on the {@link PreparedStatement}
     */
    void writeFloat(PreparedStatement statement, int index, float value) throws SQLException;

    /**
     * Sets a double value
     *
     * @param statement prepared statement
     * @param index     statement index
     * @param value     type to be bound
     * @throws SQLException on a failure to set the expression on the {@link PreparedStatement}
     */
    void writeDouble(PreparedStatement statement, int index, double value) throws SQLException;

}
