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

import io.requery.BlockingEntityStore;
import io.requery.EntityCache;
import io.requery.PersistenceException;
import io.requery.ReadOnlyException;
import io.requery.RollbackException;
import io.requery.Transaction;
import io.requery.TransactionException;
import io.requery.TransactionIsolation;
import io.requery.TransactionListener;
import io.requery.cache.EmptyEntityCache;
import io.requery.meta.Attribute;
import io.requery.meta.EntityModel;
import io.requery.meta.QueryAttribute;
import io.requery.meta.Type;
import io.requery.proxy.CompositeKey;
import io.requery.proxy.EntityProxy;
import io.requery.query.Deletion;
import io.requery.query.Expression;
import io.requery.query.Insertion;
import io.requery.query.Result;
import io.requery.query.Scalar;
import io.requery.query.Selection;
import io.requery.query.Tuple;
import io.requery.query.Update;
import io.requery.query.element.QueryElement;
import io.requery.query.function.Count;
import io.requery.sql.platform.PlatformDelegate;
import io.requery.util.ClassMap;
import io.requery.util.Objects;
import io.requery.util.function.Supplier;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

import static io.requery.query.element.QueryType.DELETE;
import static io.requery.query.element.QueryType.INSERT;
import static io.requery.query.element.QueryType.SELECT;
import static io.requery.query.element.QueryType.UPDATE;

/**
 * Implementation of {@link BlockingEntityStore} that persists and makes queryable
 * {@link io.requery.Entity} instances through standard JDBC database connections.
 *
 * @param <T> base class or interface to restrict all entities that are stored to (e.g.
 * {@link Object} or {@link java.io.Serializable} for instance.
 *
 * @author Nikhil Purushe
 */
public class EntityDataStore<T> implements BlockingEntityStore<T> {

    private final EntityModel entityModel;
    private final EntityCache entityCache;
    private final ConnectionProvider connectionProvider;
    private final ClassMap<EntityReader<?, ?>> readers;
    private final ClassMap<EntityWriter<?, ?>> writers;
    private final CompositeEntityListener<T> stateListeners;
    private final CompositeStatementListener statementListeners;
    private final UpdateOperation updateExecutor;
    private final SelectCountOperation countExecutor;
    private final Executor writeExecutor;
    private final Supplier<EntityProxyTransaction> transactionProvider;
    private final TransactionIsolation defaultIsolation;
    private final Set<Supplier<TransactionListener>> transactionListenerFactories;
    private final int batchUpdateSize;
    private final boolean quoteTableNames;
    private final boolean quoteColumnNames;
    private final AtomicBoolean closed;
    private TransactionMode transactionMode;
    private PreparedStatementCache statementCache;
    private QueryBuilder.Options queryOptions;
    private Mapping mapping;
    private Platform platform;
    private boolean metadataChecked;
    private boolean supportsBatchUpdates;
    private DataContext context;

    /**
     * Create new {@link EntityDataStore} with the given {@link DataSource} and {@link EntityModel}.
     *
     * @param dataSource to use
     * @param model to use
     */
    public EntityDataStore(DataSource dataSource, EntityModel model) {
        this(dataSource, model, null);
    }

    /**
     * Create new {@link EntityDataStore} with the given {@link DataSource},{@link EntityModel} and
     * {@link Mapping}.
     *
     * @param dataSource to use
     * @param model to use
     * @param mapping to use
     */
    public EntityDataStore(DataSource dataSource, EntityModel model, Mapping mapping) {
        this(new ConfigurationBuilder(dataSource, model)
                .setMapping(mapping)
                .build());
    }

    /**
     * Create new {@link EntityDataStore} with the given configuration.
     *
     * @param configuration to use
     */
    public EntityDataStore(Configuration configuration) {
        closed = new AtomicBoolean();
        readers = new ClassMap<>();
        writers = new ClassMap<>();
        entityModel = Objects.requireNotNull(configuration.entityModel());
        connectionProvider = Objects.requireNotNull(configuration.connectionProvider());
        mapping = configuration.mapping();
        platform = configuration.platform();
        transactionMode = configuration.transactionMode();
        writeExecutor = configuration.writeExecutor();
        quoteColumnNames = configuration.quoteColumnNames();
        quoteTableNames = configuration.quoteTableNames();
        batchUpdateSize = configuration.batchUpdateSize();
        defaultIsolation = configuration.transactionIsolation();
        transactionListenerFactories = configuration.transactionListenerFactories();
        statementListeners = new CompositeStatementListener(configuration.statementListeners());
        stateListeners = new CompositeEntityListener<>();

        entityCache = configuration.entityCache() == null ?
                new EmptyEntityCache() : configuration.entityCache();
        int statementCacheSize = configuration.statementCacheSize();
        if (statementCacheSize > 0) {
            statementCache = new PreparedStatementCache(statementCacheSize);
        }
        // set default mapping (otherwise deferred to getConnection()
        if (platform != null && mapping == null) {
            mapping = new GenericMapping(platform);
        }
        context = new DataContext();
        transactionProvider = new TransactionProvider(context);
        updateExecutor = new UpdateOperation(context);
        countExecutor = new SelectCountOperation(context);
        if (configuration.useDefaultLogging()) {
            LoggingListener<T> logListener = new LoggingListener<>();
            stateListeners.addPostLoadListener(logListener);
            stateListeners.addPostInsertListener(logListener);
            stateListeners.addPostDeleteListener(logListener);
            stateListeners.addPostUpdateListener(logListener);
            stateListeners.addPreInsertListener(logListener);
            stateListeners.addPreDeleteListener(logListener);
            stateListeners.addPreUpdateListener(logListener);
            stateListeners.enableStateListeners(true);
            statementListeners.add(logListener);
        } else {
            // disable the listener since it's used only for logging right now
            stateListeners.enableStateListeners(false);
        }
    }

    @Override
    public <E extends T> E insert(E entity) {
        try (TransactionScope transaction = new TransactionScope(transactionProvider)) {
            EntityProxy<E> proxy = context.proxyOf(entity, true);
            synchronized (proxy.syncObject()) {
                context.write(proxy.type().classType()).insert(entity, proxy);
                transaction.commit();
                return entity;
            }
        }
    }

    @Override
    public <E extends T> Iterable<E> insert(Iterable<E> entities) {
        Iterator<E> iterator = entities.iterator();
        if (iterator.hasNext()) {
            try (TransactionScope transaction = new TransactionScope(transactionProvider)) {
                EntityWriter<E, T> writer;
                E entity = iterator.next();
                EntityProxy<E> proxy = context.proxyOf(entity, true);
                writer = context.write(proxy.type().classType());
                writer.batchInsert(entities);
                transaction.commit();
            }
        }
        return entities;
    }

    @Override
    public <E extends T> E update(E entity) {
        try (TransactionScope transaction = new TransactionScope(transactionProvider)) {
            EntityProxy<E> proxy = context.proxyOf(entity, true);
            synchronized (proxy.syncObject()) {
                context.write(proxy.type().classType()).update(entity, proxy);
                transaction.commit();
                return entity;
            }
        }
    }

    @Override
    public <E extends T> E refresh(E entity) {
        EntityProxy<E> proxy = context.proxyOf(entity, false);
        synchronized (proxy.syncObject()) {
            return context.read(proxy.type().classType()).refresh(entity, proxy);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <E extends T> E refresh(E entity, Attribute<?, ?>... attributes) {
        EntityProxy<E> proxy = context.proxyOf(entity, false);
        synchronized (proxy.syncObject()) {
            return context.read(proxy.type().classType())
                    .refresh(entity, proxy, (Attribute<E, ?>[]) attributes);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <E extends T> Iterable<E> refresh(Iterable<E> entities, Attribute<?, ?>... attributes) {
        Iterator<E> iterator = entities.iterator();
        if (iterator.hasNext()) {
            E entity = iterator.next();
            EntityProxy<E> proxy = context.proxyOf(entity, false);
            EntityReader<E, T> reader = context.read(proxy.type().classType());
            reader.batchRefresh(entities, (Attribute<E, ?>[])attributes);
        }
        return entities;
    }

    @Override
    public <E extends T> E refreshAll(E entity) {
        EntityProxy<E> proxy = context.proxyOf(entity, false);
        synchronized (proxy.syncObject()) {
            return context.read(proxy.type().classType()).refreshAll(entity, proxy);
        }
    }

    @Override
    public <E extends T> Void delete(E entity) {
        EntityProxy<E> proxy = context.proxyOf(entity, true);
        synchronized (proxy.syncObject()) {
            context.write(proxy.type().classType()).delete(entity, proxy);
        }
        return null;
    }

    @Override
    public <E extends T> Void delete(Iterable<E> entities) {
        Iterator<E> iterator = entities.iterator();
        if (iterator.hasNext()) {
            try (TransactionScope transaction = new TransactionScope(transactionProvider)) {
                EntityWriter<E, T> writer;
                E entity = iterator.next();
                EntityProxy<E> proxy = context.proxyOf(entity, false);
                writer = context.write(proxy.type().classType());
                writer.batchDelete(entities);
                transaction.commit();
            }
        }
        return null;
    }

    @Override
    public <E extends T, K> E findByKey(Class<E> type, K key) {
        Type<E> entityType = entityModel.typeOf(type);
        if (entityType.isCacheable() && entityCache != null) {
            E entity = entityCache.get(type, key);
            if (entity != null) {
                return entity;
            }
        }
        Set<Attribute<E, ?>> keys = entityType.keyAttributes();
        if (keys.isEmpty()) {
            throw new MissingKeyException();
        }
        Selection<Result<E>> selection = select(type);
        if (keys.size() == 1) {
            QueryAttribute<E, Object> attribute = Attributes.query(keys.iterator().next());
            selection.where(attribute.equal(key));
        } else {
            if (key instanceof CompositeKey) {
                CompositeKey compositeKey = (CompositeKey) key;
                for (Attribute<E, ?> attribute : keys) {
                    QueryAttribute<E, Object> keyAttribute = Attributes.query(attribute);
                    Object value = compositeKey.get(keyAttribute);
                    selection.where(keyAttribute.equal(value));
                }
            } else {
                throw new IllegalArgumentException("CompositeKey required");
            }
        }
        return selection.get().first();
    }

    @Override
    public Transaction transaction() {
        checkClosed();
        return transactionProvider.get();
    }

    @Override
    public void close() {
        if (closed.compareAndSet(false, true)) {
            entityCache.clear();
            if (statementCache != null) {
                statementCache.close();
            }
        }
    }

    @Override
    public Selection<Result<Tuple>> select(Expression<?>... expressions) {
        TupleResultReader reader = new TupleResultReader(context);
        SelectOperation<Tuple> select = new SelectOperation<>(context, reader);
        return new QueryElement<>(SELECT, entityModel, select).select(expressions);
    }

    @Override
    public Selection<Result<Tuple>> select(Set<? extends Expression<?>> expressions) {
        TupleResultReader reader = new TupleResultReader(context);
        SelectOperation<Tuple> select = new SelectOperation<>(context, reader);
        return new QueryElement<>(SELECT, entityModel, select).select(expressions);
    }

    @Override
    public Update<Scalar<Integer>> update() {
        checkClosed();
        return new QueryElement<>(UPDATE, entityModel, updateExecutor);
    }

    @Override
    public Deletion<Scalar<Integer>> delete() {
        checkClosed();
        return new QueryElement<>(DELETE, entityModel, updateExecutor);
    }

    @Override
    public <E extends T> Selection<Result<E>> select(Class<E> type,
                                                     QueryAttribute<?, ?>... attributes) {
        checkClosed();
        EntityResultReader<E, T> reader = new EntityResultReader<>(context.read(type));
        SelectOperation<E> select = new SelectOperation<>(context, reader);
        QueryElement<Result<E>> query = new QueryElement<>(SELECT, entityModel, select);
        Set<Expression<?>> selection;
        if (attributes == null || attributes.length == 0) {
            selection = context.read(type).defaultSelection();
        } else {
            selection = new LinkedHashSet<>(Arrays.<Expression<?>>asList(attributes));
        }
        return query.select(selection).from(type);
    }

    @Override
    public <E extends T> Selection<Result<E>> select(
                        Class<E> type, Set<? extends QueryAttribute<E, ?>> attributes) {
        checkClosed();
        EntityResultReader<E, T> reader = new EntityResultReader<>(context.read(type));
        SelectOperation<E> select = new SelectOperation<>(context, reader);
        QueryElement<Result<E>> query = new QueryElement<>(SELECT, entityModel, select);
        Set<Expression<?>> selection;
        if (attributes == null || attributes.isEmpty()) {
            selection = context.read(type).defaultSelection();
        } else {
            selection = new LinkedHashSet<>();
            selection.addAll(attributes);
        }
        return query.select(selection).from(type);
    }

    @Override
    public <E extends T> Insertion<Scalar<Integer>> insert(Class<E> type) {
        checkClosed();
        return new QueryElement<>(INSERT, entityModel, updateExecutor).from(type);
    }

    @Override
    public <E extends T> Update<Scalar<Integer>> update(Class<E> type) {
        checkClosed();
        return new QueryElement<>(UPDATE, entityModel, updateExecutor).from(type);
    }

    @Override
    public <E extends T> Deletion<Scalar<Integer>> delete(Class<E> type) {
        checkClosed();
        return new QueryElement<>(DELETE, entityModel, updateExecutor).from(type);
    }

    @Override
    public <E extends T> Selection<Scalar<Integer>> count(Class<E> type) {
        checkClosed();
        Objects.requireNotNull(type);
        return new QueryElement<>(SELECT, entityModel, countExecutor)
            .select(Count.count(type)).from(type);
    }

    @Override
    public Selection<Scalar<Integer>> count(QueryAttribute<?, ?>... attributes) {
        checkClosed();
        return new QueryElement<>(SELECT, entityModel, countExecutor)
            .select(Count.count(attributes));
    }

    @Override
    public <V> V runInTransaction(Callable<V> callable, TransactionIsolation isolation) {
        Objects.requireNotNull(callable);
        checkClosed();
        Transaction transaction = transactionProvider.get();
        if (transaction == null) {
            throw new TransactionException("no transaction");
        }
        try {
            transaction.begin(isolation);
            V result = callable.call();
            transaction.commit();
            return result;
        } catch (Exception e) {
            throw new RollbackException(e);
        }
    }

    @Override
    public <V> V runInTransaction(Callable<V> callable) {
        return runInTransaction(callable, null);
    }

    @Override
    public BlockingEntityStore<T> toBlocking() {
        return this;
    }

    protected synchronized void checkConnectionMetadata() {
        // only done once metadata assumed to be the same for every connection
        if (!metadataChecked) {
            try (Connection connection = context.getConnection()) {
                DatabaseMetaData metadata = connection.getMetaData();
                if (!metadata.supportsTransactions()) {
                    transactionMode = TransactionMode.NONE;
                }
                supportsBatchUpdates = metadata.supportsBatchUpdates();
                String quoteIdentifier = metadata.getIdentifierQuoteString();
                queryOptions = new QueryBuilder.Options(quoteIdentifier, true,
                    quoteTableNames, quoteColumnNames);
                metadataChecked = true;
            } catch (SQLException e) {
                throw new PersistenceException(e);
            }
        }
    }

    protected void checkClosed() {
        if (closed.get()) {
            throw new PersistenceException("closed");
        }
    }

    private class DataContext implements EntityContext<T>, ConnectionProvider {

        @Override
        public <E> EntityProxy<E> proxyOf(E entity, boolean forUpdate) {
            if (entity == null) {
                return null;
            }
            checkClosed();
            @SuppressWarnings("unchecked")
            Type<E> type = (Type<E>) entityModel.typeOf(entity.getClass());
            EntityProxy<E> proxy = type.proxyProvider().apply(entity);
            if (forUpdate && type.isReadOnly()) {
                throw new ReadOnlyException();
            }
            if (forUpdate) {
                EntityProxyTransaction transaction = transactionProvider.get();
                if (transaction != null && transaction.active()) {
                    transaction.addToTransaction(proxy);
                }
            }
            return proxy;
        }

        @Override
        public synchronized Connection getConnection() throws SQLException {
            Connection connection = null;
            Transaction transaction = transactionProvider.get();
            // if the transaction holds a connection use that
            if (transaction != null && transaction.active()) {
                if (transaction instanceof ConnectionProvider) {
                    ConnectionProvider connectionProvider = (ConnectionProvider) transaction;
                    connection = connectionProvider.getConnection();
                }
            }
            if (connection == null) {
                connection = connectionProvider.getConnection();
                if (statementCache != null) {
                    connection = new StatementCachingConnection(statementCache, connection);
                }
            }
            // lazily create things that depend on a connection
            if (platform == null) {
                platform = new PlatformDelegate(connection);
            }
            if (mapping == null) {
                mapping = new GenericMapping(platform);
            }
            return connection;
        }

        @Override
        public synchronized <E extends T> EntityReader<E, T> read(Class<? extends E> type) {
            @SuppressWarnings("unchecked")
            EntityReader<E, T> reader = (EntityReader<E, T>) readers.get(type);
            if (reader == null) {
                reader = new EntityReader<>(entityModel.typeOf(type), this, EntityDataStore.this);
                readers.put(type, reader);
            }
            return reader;
        }

        @Override
        public synchronized <E extends T> EntityWriter<E, T> write(Class<? extends E> type) {
            @SuppressWarnings("unchecked")
            EntityWriter<E, T> writer = (EntityWriter<E, T>) writers.get(type);
            if (writer == null) {
                writer = new EntityWriter<>(entityModel.typeOf(type), this, EntityDataStore.this);
                writers.put(type, writer);
            }
            return writer;
        }

        @Override
        public CompositeEntityListener<T> stateListener() {
            return stateListeners;
        }

        @Override
        public ConnectionProvider connectionProvider() {
            return this;
        }

        @Override
        public boolean supportsBatchUpdates() {
            checkConnectionMetadata();
            return supportsBatchUpdates && batchUpdateSize() > 0;
        }

        @Override
        public int batchUpdateSize() {
            return batchUpdateSize;
        }

        @Override
        public QueryBuilder.Options queryBuilderOptions() {
            checkConnectionMetadata();
            return queryOptions;
        }

        @Override
        public Mapping mapping() {
            return mapping;
        }

        @Override
        public EntityModel model() {
            return entityModel;
        }

        @Override
        public EntityCache cache() {
            return entityCache;
        }

        @Override
        public Platform platform() {
            return platform;
        }

        @Override
        public StatementListener statementListener() {
            return statementListeners;
        }

        @Override
        public Set<Supplier<TransactionListener>> transactionListenerFactories() {
            return transactionListenerFactories;
        }

        @Override
        public TransactionMode transactionMode() {
            checkConnectionMetadata();
            return transactionMode;
        }

        @Override
        public TransactionIsolation transactionIsolation() {
            return defaultIsolation;
        }

        @Override
        public Executor writeExecutor() {
            return writeExecutor;
        }
    }
}
