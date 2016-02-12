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

import java.util.concurrent.Callable;

/**
 * Defines an interface for objects that provide a transaction.
 *
 * @param <R> optional return type for {@link
 *            #runInTransaction(Callable, TransactionIsolation)}
 */
public interface Transactionable<R> {

    /**
     * @return the current transaction
     */
    Transaction transaction();

    /**
     * Runs the given callable in a transaction. The transaction will be automatically committed
     * after successful completion of the callable. If the callable throws an exception the
     * transaction will be rolled back.
     *
     * @param callable to run
     * @param <V>      call result type
     * @return callable result
     */
    <V> R runInTransaction(Callable<V> callable);

    /**
     * Runs the given callable in a transaction. The transaction will be automatically committed
     * after successful completion of the callable. If the callable throws an exception the
     * transaction will be rolled back.
     *
     * @param callable  to run
     * @param isolation isolation level for the transaction
     * @param <V>       call result type
     * @return callable result
     */
    <V> R runInTransaction(Callable<V> callable, TransactionIsolation isolation);
}
