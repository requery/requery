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

import io.requery.EntityCache;
import io.requery.TransactionIsolation;
import io.requery.TransactionListener;
import io.requery.meta.EntityModel;
import io.requery.util.function.Supplier;

import java.sql.Connection;
import java.util.Set;
import java.util.concurrent.Executor;

/**
 * Defines the configuration settings for the {@link EntityDataStore}.
 *
 * @author Nikhil Purushe
 */
public interface Configuration {

    /**
     * @return {@link Connection} provider. This provider must return a usable connection.
     */
    ConnectionProvider connectionProvider();

    /**
     * @return {@link Platform} to use, if null the Platform will try to be determined automatically
     * via the Connection metadata.
     */
    Platform platform();

    /**
     * @return {@link EntityModel} defining the model, must not be null.
     */
    EntityModel entityModel();

    /**
     * @return {@link EntityCache} cache to use (if null no caching will be used)
     */
    EntityCache entityCache();

    /**
     * @return the mapping implementation use (if null default mapping will be used)
     */
    Mapping mapping();

    /**
     * @return for asynchronous operations the {@link Executor} that is used to perform the write.
     */
    Executor writeExecutor();

    /**
     * @return true if the default logging should be enabled
     */
    boolean useDefaultLogging();

    /**
     * @return true if the all the table names should be quoted.
     */
    boolean quoteTableNames();

    /**
     * @return true if all column names should be quoted.
     */
    boolean quoteColumnNames();

    /**
     * @return number of statements to cache, 0 to disable caching.
     */
    int statementCacheSize();

    /**
     * @return max number of statements to use in a batch insert or update operation.
     */
    int batchUpdateSize();

    /**
     * @return the mode of transactions enabled {@link TransactionMode}, defaults to
     * {@link TransactionMode#AUTO}
     */
    TransactionMode transactionMode();

    /**
     * @return optional default {@link TransactionIsolation} isolation to use for transactions.
     */
    TransactionIsolation transactionIsolation();

    /**
     * @return get the supplier of transaction listeners. One {@link TransactionListener} will be
     * requested per {@link io.requery.Transaction}
     */
    Set<Supplier<TransactionListener>> transactionListenerFactories();

    /**
     * @return get the set of default statement listeners
     */
    Set<StatementListener> statementListeners();
}
