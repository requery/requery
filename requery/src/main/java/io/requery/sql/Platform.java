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
     * @return the type of generated key type DDL this database supports.
     */
    GeneratedColumnDefinition generatedColumnDefinition();

    /**
     * @return the type of limit support this database supports.
     */
    LimitDefinition limitDefinition();

    /**
     * @return the type of version column this database supports.
     */
    VersionColumnDefinition versionColumnDefinition();
}
