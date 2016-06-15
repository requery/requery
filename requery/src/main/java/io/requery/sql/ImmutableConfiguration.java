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
    private final Set<EntityStateListener> entityStateListeners;
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
                           Set<EntityStateListener> entityStateListeners,
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
        this.entityStateListeners = Collections.unmodifiableSet(entityStateListeners);
        this.statementListeners = Collections.unmodifiableSet(statementListeners);
        this.transactionIsolation = transactionIsolation;
        this.transactionListenerFactories = transactionListenerFactories;
        this.writeExecutor = writeExecutor;
    }

    @Override
    public int getBatchUpdateSize() {
        return batchUpdateSize;
    }

    @Override
    public EntityCache getCache() {
        return cache;
    }

    @Override
    public ConnectionProvider getConnectionProvider() {
        return connectionProvider;
    }

    @Override
    public Set<EntityStateListener> getEntityStateListeners() {
        return entityStateListeners;
    }

    @Override
    public Mapping getMapping() {
        return mapping;
    }

    @Override
    public EntityModel getModel() {
        return model;
    }

    @Override
    public Platform getPlatform() {
        return platform;
    }

    @Override
    public boolean getQuoteTableNames() {
        return quoteTableNames;
    }

    @Override
    public boolean getQuoteColumnNames() {
        return quoteColumnNames;
    }

    @Override
    public int getStatementCacheSize() {
        return statementCacheSize;
    }

    @Override
    public Set<StatementListener> getStatementListeners() {
        return statementListeners;
    }

    @Override
    public TransactionIsolation getTransactionIsolation() {
        return transactionIsolation;
    }

    @Override
    public Set<Supplier<TransactionListener>> getTransactionListenerFactories() {
        return transactionListenerFactories;
    }

    @Override
    public TransactionMode getTransactionMode() {
        return transactionMode;
    }

    @Override
    public boolean getUseDefaultLogging() {
        return useDefaultLogging;
    }

    @Override
    public Executor getWriteExecutor() {
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
