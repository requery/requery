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

import io.requery.query.Expression;
import io.requery.query.element.LimitedElement;
import io.requery.query.element.OrderByElement;
import io.requery.query.element.QueryElement;
import io.requery.sql.gen.Generator;

import java.util.Map;

/**
 * Defines platform/vendor specific SQL features.
 */
public interface Platform {

    /**
     * Given the attribute and {@link FieldType} allows the platform to replace the storage type
     * with a platform specific one. Substitution occurs both for table generation and entity
     * property conversion.
     *
     * @param mapping mapping containing type information of what is being read or written.
     */
    void addMappings(Mapping mapping);

    /**
     * @return false if the database requires foreign keys to be defined as columns in the create
     * table statement. true if they can be inlined in one statement.
     */
    boolean supportsInlineForeignKeyReference();

    /**
     * @return true if the database supports adding a constraint (e.g. foreign key) after the
     * column is created (or if it requires it). In most DDL statements the column is created first
     * and then the constraint is added.
     */
    boolean supportsAddingConstraint();

    /**
     * @return true if the database supports the 'if exists' syntax in the create table/index,
     * drop DDL statements.
     */
    boolean supportsIfExists();

    /**
     * @return true if the JDBC {@link java.sql.Connection#prepareStatement(String, String[])}
     * method should be used to get the generated key set when inserting records.
     */
    boolean supportsGeneratedColumnsInPrepareStatement();

    /**
     * @return true if the JDBC driver supports reading generated keys via
     * {@link java.sql.Statement#getGeneratedKeys}.
     */
    boolean supportsGeneratedKeysInBatchUpdate();

    /**
     * @return true if the platform supports the 'on update cascade' clause in a constraint,
     * false otherwise
     */
    boolean supportsOnUpdateCascade();

    /**
     * @return true if the platform supports an upsert (insert or update) operation either via
     * merge or an alternate syntax that is defined in {@link #upsertGenerator()}
     */
    boolean supportsUpsert();

    /**
     * @return the type of generated key type DDL this database supports.
     */
    GeneratedColumnDefinition generatedColumnDefinition();

    /**
     * @return the limit generator for this database
     */
    Generator<LimitedElement> limitGenerator();

    /**
     * @return the insert generator for this database
     */
    Generator<QueryElement<?>> insertGenerator();

    /**
     * @return the update generator for this database
     */
    Generator<Map<Expression<?>, Object>> updateGenerator();

    /**
     * @return the upsert generator for this database
     */
    Generator<Map<Expression<?>, Object>> upsertGenerator();

    /**
     * @return the order by generator for this database
     */
    Generator<OrderByElement> orderByGenerator();

    /**
     * @return the type of version column this database supports.
     */
    VersionColumnDefinition versionColumnDefinition();
}
