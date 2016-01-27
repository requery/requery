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

/**
 * The generated key column definition type the database supports.
 */
public interface GeneratedColumnDefinition {

    /**
     * @return true if for DDL generation the storage type identifier should be omitted.
     */
    boolean skipTypeIdentifier();

    /**
     * @return true if the sequence should be added after the primary key keywords,
     * false otherwise.
     */
    boolean postFixPrimaryKey();

    /**
     * Append the sequence for the given generated attribute.
     *
     * @param qb        sqlBuilder instance
     * @param attribute to generate
     */
    void appendGeneratedSequence(QueryBuilder qb, Attribute attribute);
}
