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

import io.requery.proxy.EntityProxy;

import java.util.Set;

/**
 * Provides an interface for listening to {@link Transaction} actions.
 */
public interface TransactionListener {

    /**
     * Invoked before the transaction is begun.
     *
     * @param isolation {@link TransactionIsolation} level of the transaction.
     */
    void beforeBegin(TransactionIsolation isolation);

    /**
     * Invoked after the transaction is begun.
     *
     * @param isolation {@link TransactionIsolation} level of the transaction.
     */
    void afterBegin(TransactionIsolation isolation);

    /**
     * Invoked before the transaction is committed.
     *
     * @param entities collection of entities involved in the transaction
     */
    void beforeCommit(Set<EntityProxy<?>> entities);

    /**
     * Invoked after the transaction is committed successfully.
     *
     * @param entities collection of entities involved in the transaction
     */
    void afterCommit(Set<EntityProxy<?>> entities);

    /**
     * Invoked before the transaction is rolled back.
     *
     * @param entities collection of entities involved in the transaction
     */
    void beforeRollback(Set<EntityProxy<?>> entities);

    /**
     * Invoked after the transaction is rolled back successfully.
     *
     * @param entities collection of entities involved in the transaction
     */
    void afterRollback(Set<EntityProxy<?>> entities);
}
