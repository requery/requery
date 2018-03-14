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

/**
 * Exception thrown when the affected row count of a executed statement doesn't match the value
 * expected.
 */
public class RowCountException extends PersistenceException {

    private final String entityName;
    private final long expected;
    private final long actual;

    RowCountException(String entityName, long expected, long actual) {
        super(entityName + ": expected " + expected + " row affected actual " + actual);
        this.entityName = entityName;
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
     * @return entity name affected
     */
    public String getEntityName() {
        return entityName;
    }
}
