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

package io.requery;

/**
 * Represents a transaction. Obtained from {@link Transactionable#transaction()}.
 *
 * @author Nikhil Purushe
 */
public interface Transaction extends AutoCloseable {

    /**
     * Begins the transaction. This method can only be called if a transaction is not already
     * {#active} otherwise an {@link IllegalStateException} will be thrown.
     *
     * @return the current transaction instance. The return value can be used in a try-with block.
     */
    Transaction begin();

    /**
     * Begins the transaction. This method can only be called if a transaction is not already
     * {#active} otherwise an {@link IllegalStateException} will be thrown.
     *
     * @param isolation desired isolation level for the transaction (null for the default).
     *
     * @return the current transaction instance. The return value can be used in a try-with block.
     */
    Transaction begin(TransactionIsolation isolation);

    /**
     * Commit the current transaction.
     */
    void commit();

    /**
     * Rollback the transaction.
     */
    void rollback();

    /**
     * @return true if the transaction is currently active false otherwise.
     */
    boolean active();

    /**
     * Closes the transaction. In rollback mode if commit was not called successfully then a
     * rollback operation will be performed automatically.
     */
    @Override
    void close();
}
