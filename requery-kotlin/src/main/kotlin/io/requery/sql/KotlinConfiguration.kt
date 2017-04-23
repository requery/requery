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

package io.requery.sql

import io.requery.EntityCache
import io.requery.TransactionIsolation
import io.requery.TransactionListener
import io.requery.cache.WeakEntityCache
import io.requery.meta.EntityModel
import io.requery.util.function.Function
import io.requery.util.function.Supplier
import java.util.LinkedHashSet
import java.util.concurrent.Executor
import javax.sql.CommonDataSource
import javax.sql.ConnectionPoolDataSource
import javax.sql.DataSource

/** Simple read-only configuration instance */
class KotlinConfiguration(
    private val model: EntityModel,
    dataSource: CommonDataSource,
    private val mapping: Mapping? = null,
    private val platform: Platform? = null,
    private val cache: EntityCache = WeakEntityCache(),
    private val useDefaultLogging: Boolean = false,
    private val statementCacheSize: Int = 0,
    private val batchUpdateSize: Int = 64,
    private val quoteTableNames: Boolean = false,
    private val quoteColumnNames: Boolean = false,
    private val tableTransformer: Function<String, String>? = null,
    private val columnTransformer: Function<String, String>? = null,
    private val transactionMode: TransactionMode = TransactionMode.AUTO,
    private val transactionIsolation: TransactionIsolation? = null,
    private val statementListeners: Set<StatementListener> = LinkedHashSet(),
    private val entityStateListeners: Set<EntityStateListener<Any>> = LinkedHashSet(),
    private val transactionListeners: Set<Supplier<TransactionListener>> = LinkedHashSet(),
    private val writeExecutor: Executor? = null) : Configuration {

    private val connectionProvider = when (dataSource) {
        is ConnectionPoolDataSource -> PooledConnectionProvider(dataSource);
        is DataSource -> DataSourceConnectionProvider(dataSource)
        else -> throw IllegalArgumentException("unsupported dataSource " + dataSource)
    }

    override fun getBatchUpdateSize(): Int {
        return batchUpdateSize
    }

    override fun getConnectionProvider(): ConnectionProvider? {
        return connectionProvider
    }

    override fun getCache(): EntityCache? {
        return cache
    }

    override fun getEntityStateListeners(): Set<EntityStateListener<Any>> {
        return entityStateListeners
    }

    override fun getMapping(): Mapping? {
        return mapping
    }

    override fun getModel(): EntityModel {
        return model
    }

    override fun getPlatform(): Platform? {
        return platform
    }

    override fun getQuoteTableNames(): Boolean {
        return quoteTableNames
    }

    override fun getQuoteColumnNames(): Boolean {
        return quoteColumnNames
    }

    override fun getTableTransformer(): Function<String, String>? {
        return tableTransformer
    }

    override fun getColumnTransformer(): Function<String, String>? {
        return columnTransformer
    }

    override fun getStatementCacheSize(): Int {
        return statementCacheSize
    }

    override fun getStatementListeners(): Set<StatementListener>? {
        return statementListeners
    }

    override fun getTransactionMode(): TransactionMode? {
        return transactionMode
    }

    override fun getTransactionIsolation(): TransactionIsolation? {
        return transactionIsolation
    }

    override fun getTransactionListenerFactories(): Set<Supplier<TransactionListener>>? {
        return transactionListeners
    }

    override fun getUseDefaultLogging(): Boolean {
        return useDefaultLogging
    }

    override fun getWriteExecutor(): Executor? {
        return writeExecutor
    }
}
