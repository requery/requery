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
import io.requery.cache.WeakEntityCache;
import io.requery.TransactionListener;
import io.requery.meta.EntityModel;
import io.requery.util.Objects;
import io.requery.util.function.Supplier;

import javax.sql.CommonDataSource;
import javax.sql.ConnectionPoolDataSource;
import javax.sql.DataSource;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.Executor;

/**
 * Assists in creating a {@link Configuration} instance for use with the {@link EntityDataStore}.
 *
 * @see Configuration
 *
 * @author Nikhil Purushe
 */
public class ConfigurationBuilder {

    private final EntityModel model;
    private final ConnectionProvider connectionProvider;
    private final Set<StatementListener> statementListeners;
    private final Set<Supplier<TransactionListener>> transactionListenerFactory;
    private Platform platform;
    private EntityCache cache;
    private Mapping mapping;
    private TransactionMode transactionMode;
    private TransactionIsolation transactionIsolation;
    private boolean useDefaultLogging;
    private int statementCacheSize;
    private int batchUpdateSize;
    private boolean quoteTableNames;
    private boolean quoteColumnNames;
    private Executor writeExecutor;

    public ConfigurationBuilder(ConnectionProvider connectionProvider, EntityModel model) {
        this.connectionProvider = Objects.requireNotNull(connectionProvider);
        this.model = Objects.requireNotNull(model);
        this.statementListeners = new LinkedHashSet<>();
        this.transactionListenerFactory = new LinkedHashSet<>();
        setQuoteTableNames(false);
        setQuoteColumnNames(false);
        setEntityCache(new WeakEntityCache());
        setStatementCacheSize(0);
        setBatchUpdateSize(64);
        setTransactionMode(TransactionMode.AUTO);
        setTransactionIsolation(null);
    }

    public ConfigurationBuilder(CommonDataSource dataSource, EntityModel model) {
        this(createConnectionProvider(dataSource), model);
    }

    private static ConnectionProvider createConnectionProvider(CommonDataSource dataSource) {
        if (dataSource instanceof DataSource) {
            return new DataSourceConnectionProvider((DataSource)dataSource);
        } else if(dataSource instanceof ConnectionPoolDataSource) {
            return new PooledConnectionProvider((ConnectionPoolDataSource)dataSource);
        } else {
            throw new IllegalArgumentException("unsupported dataSource " + dataSource);
        }
    }

    public ConfigurationBuilder setMapping(Mapping mapping) {
        this.mapping = mapping;
        return this;
    }

    public ConfigurationBuilder setPlatform(Platform platform) {
        this.platform = platform;
        return this;
    }

    public ConfigurationBuilder setEntityCache(EntityCache cache) {
        this.cache = cache;
        return this;
    }

    public ConfigurationBuilder setStatementCacheSize(int size) {
        if (size < 0) {
            throw new IllegalArgumentException();
        }
        this.statementCacheSize = size;
        return this;
    }

    public ConfigurationBuilder setBatchUpdateSize(int size) {
        if (size < 0) {
            throw new IllegalArgumentException();
        }
        this.batchUpdateSize = size;
        return this;
    }

    public ConfigurationBuilder setQuoteTableNames(boolean quote) {
        this.quoteTableNames = quote;
        return this;
    }

    public ConfigurationBuilder setQuoteColumnNames(boolean quote) {
        this.quoteColumnNames = quote;
        return this;
    }

    public ConfigurationBuilder setWriteExecutor(Executor writeExecutor) {
        this.writeExecutor = writeExecutor;
        return this;
    }

    public ConfigurationBuilder addStatementListener(StatementListener listener) {
        this.statementListeners.add(Objects.requireNotNull(listener));
        return this;
    }

    public ConfigurationBuilder addTransactionListenerFactory(
        Supplier<TransactionListener> supplier) {

        this.transactionListenerFactory.add(Objects.requireNotNull(supplier));
        return this;
    }

    public ConfigurationBuilder useDefaultLogging() {
        this.useDefaultLogging = true;
        return this;
    }

    public ConfigurationBuilder setTransactionIsolation(TransactionIsolation isolation) {
        this.transactionIsolation = isolation;
        return this;
    }

    public ConfigurationBuilder setTransactionMode(TransactionMode mode) {
        this.transactionMode = mode;
        return this;
    }

    public Configuration build() {
        return new ImmutableConfiguration(
            connectionProvider,
            platform,
            model,
            cache,
            mapping,
            useDefaultLogging,
            statementCacheSize,
            batchUpdateSize,
            quoteTableNames,
            quoteColumnNames,
            statementListeners,
            transactionMode,
            transactionIsolation,
            transactionListenerFactory,
            writeExecutor);
    }

}
