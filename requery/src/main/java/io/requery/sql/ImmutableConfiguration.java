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
import io.requery.util.Objects;
import io.requery.util.function.Supplier;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.Executor;

final class ImmutableConfiguration implements Configuration {

    private final Platform platform;
    private final EntityModel model;
    private final EntityCache cache;
    private final Mapping mapping;
    private final boolean useDefaultLogging;
    private final int statementCacheSize;
    private final int batchUpdateSize;
    private final boolean quoteTableNames;
    private final boolean quoteColumnNames;
    private final TransactionMode transactionMode;
    private final TransactionIsolation transactionIsolation;
    private final ConnectionProvider connectionProvider;
    private final Set<StatementListener> statementListeners;
    private final Set<Supplier<TransactionListener>> transactionListenerFactories;
    private final Executor writeExecutor;

    ImmutableConfiguration(ConnectionProvider connectionProvider,
                           Platform platform,
                           EntityModel model,
                           EntityCache cache,
                           Mapping mapping,
                           boolean useDefaultLogging,
                           int statementCacheSize,
                           int batchUpdateSize,
                           boolean quoteTableNames,
                           boolean quoteColumnNames,
                           Set<StatementListener> statementListeners,
                           TransactionMode transactionMode,
                           TransactionIsolation transactionIsolation,
                           Set<Supplier<TransactionListener>> transactionListenerFactories,
                           Executor writeExecutor) {
        this.connectionProvider = connectionProvider;
        this.platform = platform;
        this.model = model;
        this.cache = cache;
        this.mapping = mapping;
        this.useDefaultLogging = useDefaultLogging;
        this.statementCacheSize = statementCacheSize;
        this.batchUpdateSize = batchUpdateSize;
        this.quoteTableNames = quoteTableNames;
        this.quoteColumnNames = quoteColumnNames;
        this.transactionMode = transactionMode;
        this.statementListeners = Collections.unmodifiableSet(statementListeners);
        this.transactionIsolation = transactionIsolation;
        this.transactionListenerFactories = transactionListenerFactories;
        this.writeExecutor = writeExecutor;
    }

    @Override
    public ConnectionProvider connectionProvider() {
        return connectionProvider;
    }

    @Override
    public Platform platform() {
        return platform;
    }

    @Override
    public EntityModel entityModel() {
        return model;
    }

    @Override
    public EntityCache entityCache() {
        return cache;
    }

    @Override
    public Mapping mapping() {
        return mapping;
    }

    @Override
    public boolean useDefaultLogging() {
        return useDefaultLogging;
    }

    @Override
    public int statementCacheSize() {
        return statementCacheSize;
    }

    @Override
    public int batchUpdateSize() {
        return batchUpdateSize;
    }

    @Override
    public boolean quoteTableNames() {
        return quoteTableNames;
    }

    @Override
    public boolean quoteColumnNames() {
        return quoteColumnNames;
    }

    @Override
    public Set<StatementListener> statementListeners() {
        return statementListeners;
    }

    @Override
    public TransactionIsolation transactionIsolation() {
        return transactionIsolation;
    }

    @Override
    public Set<Supplier<TransactionListener>> transactionListenerFactories() {
        return transactionListenerFactories;
    }

    @Override
    public TransactionMode transactionMode() {
        return transactionMode;
    }

    @Override
    public Executor writeExecutor() {
        return writeExecutor;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Configuration) {
            Configuration other = (Configuration) obj;
            return hashCode() == other.hashCode();
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(platform,
            connectionProvider,
            model,
            mapping,
            quoteColumnNames,
            quoteTableNames,
            transactionIsolation,
            transactionMode,
            statementCacheSize,
            transactionListenerFactories,
            useDefaultLogging);
    }

    @Override
    public String toString() {
        return ("platform: " + platform) +
            "connectionProvider: " + connectionProvider +
            "model: " + model +
            "quoteColumnNames: " + quoteColumnNames +
            "quoteTableNames: " + quoteTableNames +
            "transactionMode" + transactionMode +
            "transactionIsolation" + transactionIsolation +
            "statementCacheSize: " + statementCacheSize +
            "useDefaultLogging: " + useDefaultLogging;
    }
}
