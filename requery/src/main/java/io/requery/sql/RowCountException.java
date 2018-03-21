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

import io.requery.PersistenceException;

import javax.annotation.Nonnull;

/**
 * Exception thrown when the affected row count of a executed statement doesn't match the value
 * expected.
 */
public class RowCountException extends PersistenceException {

    @Nonnull
    private final Class<?> entityClass;
    private final long expected;
    private final long actual;

    RowCountException(@Nonnull Class<?> entityClass, long expected, long actual) {
        super(entityClass.getSimpleName() + ": expected " + expected + " row affected actual " + actual);
        this.entityClass = entityClass;
        this.expected = expected;
        this.actual = actual;
    }

    /**
     * @return the expected affected value count
     */
    public long getExpected() {
        return expected;
    }

    /**
     * @return the actual affected value count
     */
    public long getActual() {
        return actual;
    }

    /**
     * @return entity class affected
     */
    @Nonnull
    public Class<?> getEntityClass() {
        return entityClass;
    }
}
