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

import io.requery.meta.Attribute;
import io.requery.query.Expression;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

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
     * @param basicType       non null type to replace
     * @param replacementType non null replacement type
     * @param <T>             the mapped type
     * @return mapping instance
     */
    <T> Mapping replaceType(FieldType<T> basicType, FieldType<T> replacementType);

    /**
     * Get the mapped storage type mapping for a given type {@link Attribute}.
     *
     * @param attribute attribute non null attribute
     * @return the mapped {@link FieldType}
     */
    FieldType mapAttribute(Attribute<?, ?> attribute);

    /**
     * Get the mapped storage type mapping for a given {@link Attribute} that is a collection type.
     *
     * @param attribute attribute
     * @return the mapped {@link FieldType}s for a collection
     */
    List<FieldType> mapCollectionAttribute(Attribute<?, ?> attribute);

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
}
