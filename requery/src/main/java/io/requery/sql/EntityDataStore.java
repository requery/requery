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
import io.requery.sql.gen.StatementGenerator;
import io.requery.sql.platform.PlatformDelegate;
import io.requery.util.ClassMap;
import io.requery.util.Objects;
import io.requery.util.function.Supplier;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
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
@ParametersAreNonnullByDefault
public class EntityDataStore<T> implements BlockingEntityStore<T> {

    private final EntityModel entityModel;
    private final EntityCache entityCache;
    private final ConnectionProvider connectionProvider;
    private final ClassMap<EntityReader<?, ?>> readers;
    private final ClassMap<EntityWriter<?, ?>> writers;
    private final CompositeEntityListener<T> stateListeners;
    private final CompositeStatementListener statementListeners;
    private final UpdateOperation updateOperation;
    private final SelectCountOperation countOperation;
    private final TransactionProvider transactionProvider;
    private final Configuration configuration;
    private final AtomicBoolean closed;
    private TransactionMode transactionMode;
    private PreparedStatementCache statementCache;
    private QueryBuilder.Options queryOptions;
    private Mapping mapping;
    private Platform platform;
    private StatementGenerator statementGenerator;
    private boolean metadataChecked;
    private boolean supportsBatchUpdates;
    private final DataContext context;

    /**
     * Creates a new {@link EntityDataStore} with the given {@link DataSource} and
     * {@link EntityModel}.
     *
     * @param dataSource to use
     * @param model to use
     */
    public EntityDataStore(DataSource dataSource, EntityModel model) {
        this(dataSource, model, null);
    }

    /**
     * Creates a new {@link EntityDataStore} with the given {@link DataSource},{@link EntityModel}
     * and {@link Mapping}.
     *
     * @param dataSource to use
     * @param model to use
     * @param mapping to use
     */
    public EntityDataStore(DataSource dataSource, EntityModel model, @Nullable Mapping mapping) {
        this(new ConfigurationBuilder(dataSource, model)
                .setMapping(mapping)
                .build());
    }

    /**
     * Creates a new {@link EntityDataStore} with the given configuration.
     *
     * @param configuration to use
     */
    public EntityDataStore(Configuration configuration) {
        closed = new AtomicBoolean();
        readers = new ClassMap<>();
        writers = new ClassMap<>();
        entityModel = Objects.requireNotNull(configuration.getModel());
        connectionProvider = Objects.requireNotNull(configuration.getConnectionProvider());
        mapping = configuration.getMapping();
        platform = configuration.getPlatform();
        transactionMode = configuration.getTransactionMode();
        this.configuration = configuration;
        statementListeners = new CompositeStatementListener(configuration.getStatementListeners());
        stateListeners = new CompositeEntityListener<>();

        entityCache = configuration.getCache() == null ?
                new EmptyEntityCache() : configuration.getCache();
        int statementCacheSize = configuration.getStatementCacheSize();
        if (statementCacheSize > 0) {
            statementCache = new PreparedStatementCache(statementCacheSize);
        }
        // set default mapping (otherwise deferred to getConnection()
        if (platform != null && mapping == null) {
            mapping = new GenericMapping(platform);
        }
        context = new DataContext();
        transactionProvider = new TransactionProvider(context);
        updateOperation = new UpdateOperation(context);
        countOperation = new SelectCountOperation(context);
        Set<EntityStateListener<T>> entityListeners = new LinkedHashSet<>();
        if (configuration.getUseDefaultLogging()) {
            LoggingListener<T> logListener = new LoggingListener<>();
            entityListeners.add(logListener);
            statementListeners.add(logListener);
        }
        if (!configuration.getEntityStateListeners().isEmpty()) {
            for (@SuppressWarnings("unchecked")
                 EntityStateListener<T> listener : configuration.getEntityStateListeners()) {
                entityListeners.add(listener);
            }
        }
        if (!entityListeners.isEmpty()){
            stateListeners.enableStateListeners(true);
            for (EntityStateListener<T> listener : entityListeners) {
                stateListeners.addPostLoadListener(listener);
                stateListeners.addPostInsertListener(listener);
                stateListeners.addPostDeleteListener(listener);
                stateListeners.addPostUpdateListener(listener);
                stateListeners.addPreInsertListener(listener);
                stateListeners.addPreDeleteListener(listener);
                stateListeners.addPreUpdateListener(listener);
            }
        }
    }

    @Override
    public <E extends T> E insert(E entity) {
        insert(entity, null);
        return entity;
    }

    @Override
    public <K, E extends T> K insert(E entity, @Nullable Class<K> keyClass) {
        try (TransactionScope transaction = new TransactionScope(transactionProvider)) {
            EntityProxy<E> proxy = context.proxyOf(entity, true);
            synchronized (proxy.syncObject()) {
                EntityWriter<E, T> writer = context.write(proxy.type().getClassType());
                GeneratedKeys<E> key = null;
                if (keyClass != null) {
                    key = new GeneratedKeys<>(proxy.type().isImmutable() ? null : proxy);
                }
                writer.insert(entity, proxy, key);
                transaction.commit();
                if (key != null && key.size() > 0) {
                    return keyClass.cast(key.get(0));
                }
            }
        }
        return null;
    }

    @Override
    public <E extends T> Iterable<E> insert(Iterable<E> entities) {
        insert(entities, null);
        return entities;
    }

    @Override
    public <K, E extends T> Iterable<K> insert(Iterable<E> entities, @Nullable Class<K> keyClass) {
        Iterator<E> iterator = entities.iterator();
        if (iterator.hasNext()) {
            try (TransactionScope transaction = new TransactionScope(transactionProvider)) {
                E entity = iterator.next();
                EntityProxy<E> proxy = context.proxyOf(entity, true);
                EntityWriter<E, T> writer = context.write(proxy.type().getClassType());
                GeneratedKeys<E> keys = writer.batchInsert(entities, keyClass != null);
                transaction.commit();
                @SuppressWarnings("unchecked")
                Iterable<K> result = (Iterable<K>) keys;
                return result;
            }
        }
        return null;
    }

    @Override
    public <E extends T> E update(E entity) {
        try (TransactionScope transaction = new TransactionScope(transactionProvider)) {
            EntityProxy<E> proxy = context.proxyOf(entity, true);
            synchronized (proxy.syncObject()) {
                context.write(proxy.type().getClassType()).update(entity, proxy);
                transaction.commit();
                return entity;
            }
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <E extends T> E update(E entity, Attribute<?, ?>... attributes) {
        try (TransactionScope transaction = new TransactionScope(transactionProvider)) {
            EntityProxy<E> proxy = context.proxyOf(entity, true);
            synchronized (proxy.syncObject()) {
                context.write(proxy.type().getClassType())
                        .update(entity, proxy, (Attribute<E, ?>[]) attributes);
                transaction.commit();
                return entity;
            }
        }
    }

    @Override
    public <E extends T> Iterable<E> update(Iterable<E> entities) {
        try (TransactionScope transaction = new TransactionScope(transactionProvider)) {
            for (E entity : entities) {
                update(entity);
            }
            transaction.commit();
        }
        return entities;
    }

    @Override
    public <E extends T> E upsert(E entity) {
        try (TransactionScope transaction = new TransactionScope(transactionProvider)) {
            EntityProxy<E> proxy = context.proxyOf(entity, true);
            synchronized (proxy.syncObject()) {
                EntityWriter<E, T> writer = context.write(proxy.type().getClassType());
                writer.upsert(entity, proxy);
                transaction.commit();
                return entity;
            }
        }
    }

    @Override
    public <E extends T> Iterable<E> upsert(Iterable<E> entities) {
        try (TransactionScope transaction = new TransactionScope(transactionProvider)) {
            for (E entity : entities) {
                upsert(entity);
            }
            transaction.commit();
        }
        return entities;
    }

    @Override
    public <E extends T> E refresh(E entity) {
        EntityProxy<E> proxy = context.proxyOf(entity, false);
        synchronized (proxy.syncObject()) {
            return context.read(proxy.type().getClassType()).refresh(entity, proxy);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <E extends T> E refresh(E entity, Attribute<?, ?>... attributes) {
        EntityProxy<E> proxy = context.proxyOf(entity, false);
        synchronized (proxy.syncObject()) {
            return context.read(proxy.type().getClassType())
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
            EntityReader<E, T> reader = context.read(proxy.type().getClassType());
            return reader.batchRefresh(entities, (Attribute<E, ?>[])attributes);
        }
        return entities;
    }

    @Override
    public <E extends T> E refreshAll(E entity) {
        EntityProxy<E> proxy = context.proxyOf(entity, false);
        synchronized (proxy.syncObject()) {
            return context.read(proxy.type().getClassType()).refreshAll(entity, proxy);
        }
    }

    @Override
    public <E extends T> Void delete(E entity) {
        try (TransactionScope transaction = new TransactionScope(transactionProvider)) {
            EntityProxy<E> proxy = context.proxyOf(entity, true);
            synchronized (proxy.syncObject()) {
                context.write(proxy.type().getClassType()).delete(entity, proxy);
                transaction.commit();
            }
        }
        return null;
    }

    @Override
    public <E extends T> Void delete(Iterable<E> entities) {
        Iterator<E> iterator = entities.iterator();
        if (iterator.hasNext()) {
            try (TransactionScope transaction = new TransactionScope(transactionProvider)) {
                E entity = iterator.next();
                EntityProxy<E> proxy = context.proxyOf(entity, false);
                EntityWriter<E, T> writer = context.write(proxy.type().getClassType());
                writer.delete(entities);
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
        Set<Attribute<E, ?>> keys = entityType.getKeyAttributes();
        if (keys.isEmpty()) {
            throw new MissingKeyException();
        }
        Selection<? extends Result<E>> selection = select(type);
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
        return selection.get().firstOrNull();
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
    public Selection<? extends Result<Tuple>> select(Expression<?>... expressions) {
        TupleResultReader reader = new TupleResultReader(context);
        SelectOperation<Tuple> select = new SelectOperation<>(context, reader);
        return new QueryElement<>(SELECT, entityModel, select).select(expressions);
    }

    @Override
    public Selection<? extends Result<Tuple>> select(Set<? extends Expression<?>> expressions) {
        TupleResultReader reader = new TupleResultReader(context);
        SelectOperation<Tuple> select = new SelectOperation<>(context, reader);
        return new QueryElement<>(SELECT, entityModel, select).select(expressions);
    }

    @Override
    public Update<? extends Scalar<Integer>> update() {
        checkClosed();
        return new QueryElement<>(UPDATE, entityModel, updateOperation);
    }

    @Override
    public Deletion<? extends Scalar<Integer>> delete() {
        checkClosed();
        return new QueryElement<>(DELETE, entityModel, updateOperation);
    }

    @Override
    public <E extends T> Selection<? extends Result<E>>
    select(Class<E> type, QueryAttribute<?, ?>... attributes) {
        checkClosed();
        EntityReader<E, T> reader = context.read(type);
        Set<Expression<?>> selection;
        ResultReader<E> resultReader;
        if (attributes.length == 0) {
            selection = reader.defaultSelection();
            resultReader = reader.newResultReader(reader.defaultSelectionAttributes());
        } else {
            selection = new LinkedHashSet<>(Arrays.<Expression<?>>asList(attributes));
            resultReader = reader.newResultReader(attributes);
        }
        SelectOperation<E> select = new SelectOperation<>(context, resultReader);
        QueryElement<? extends Result<E>> query = new QueryElement<>(SELECT, entityModel, select);
        return query.select(selection).from(type);
    }

    @Override
    public <E extends T> Selection<? extends Result<E>>
    select(Class<E> type, Set<? extends QueryAttribute<E, ?>> attributes) {
        QueryAttribute<?, ?>[] array = attributes.toArray(new QueryAttribute[attributes.size()]);
        return select(type, array);
    }

    @Override
    public <E extends T> Insertion<? extends Result<Tuple>> insert(Class<E> type) {
        checkClosed();
        Type<E> entityType = context.getModel().typeOf(type);
        Set<Expression<?>> keySelection = new LinkedHashSet<>();
        for (Attribute<E, ?> attribute : entityType.getKeyAttributes()) {
            keySelection.add((Expression<?>) attribute);
        }
        InsertReturningOperation operation = new InsertReturningOperation(context, keySelection);
        return new QueryElement<>(INSERT, entityModel, operation).from(type);
    }

    @Override
    public <E extends T> Update<? extends Scalar<Integer>> update(Class<E> type) {
        checkClosed();
        return new QueryElement<>(UPDATE, entityModel, updateOperation).from(type);
    }

    @Override
    public <E extends T> Deletion<? extends Scalar<Integer>> delete(Class<E> type) {
        checkClosed();
        return new QueryElement<>(DELETE, entityModel, updateOperation).from(type);
    }

    @Override
    public <E extends T> Selection<? extends Scalar<Integer>> count(Class<E> type) {
        checkClosed();
        Objects.requireNotNull(type);
        return new QueryElement<>(SELECT, entityModel, countOperation)
            .select(Count.count(type)).from(type);
    }

    @Override
    public Selection<? extends Scalar<Integer>> count(QueryAttribute<?, ?>... attributes) {
        checkClosed();
        return new QueryElement<>(SELECT, entityModel, countOperation)
            .select(Count.count(attributes));
    }

    @Override
    public Result<Tuple> raw(final String query, final Object... parameters) {
        checkClosed();
        return new RawTupleQuery(context, query, parameters).get();
    }

    @Override
    public <E extends T> Result<E> raw(Class<E> type, String query, Object... parameters) {
        checkClosed();
        return new RawEntityQuery<>(context, type, query, parameters).get();
    }

    @Override
    public <V> V runInTransaction(Callable<V> callable, @Nullable TransactionIsolation isolation) {
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
                    configuration.getTableTransformer(),
                    configuration.getColumnTransformer(),
                    configuration.getQuoteTableNames(),
                    configuration.getQuoteColumnNames());
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

    protected EntityContext<T> context() {
        return context;
    }

    private class DataContext implements EntityContext<T>, ConnectionProvider {

        @Override
        public <E> EntityProxy<E> proxyOf(E entity, boolean forUpdate) {
            checkClosed();
            @SuppressWarnings("unchecked")
            Type<E> type = (Type<E>) entityModel.typeOf(entity.getClass());
            EntityProxy<E> proxy = type.getProxyProvider().apply(entity);
            if (forUpdate && type.isReadOnly()) {
                throw new ReadOnlyException();
            }
            if (forUpdate) {
                EntityTransaction transaction = transactionProvider.get();
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
                checkConnectionMetadata();
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
                checkConnectionMetadata();
                writer = new EntityWriter<>(entityModel.typeOf(type), this, EntityDataStore.this);
                writers.put(type, writer);
            }
            return writer;
        }

        @Override
        public CompositeEntityListener<T> getStateListener() {
            return stateListeners;
        }

        @Override
        public boolean supportsBatchUpdates() {
            checkConnectionMetadata();
            return supportsBatchUpdates && getBatchUpdateSize() > 0;
        }

        @Override
        public int getBatchUpdateSize() {
            return configuration.getBatchUpdateSize();
        }

        @Override
        public QueryBuilder.Options getQueryBuilderOptions() {
            checkConnectionMetadata();
            return queryOptions;
        }

        @Override
        public Mapping getMapping() {
            return mapping;
        }

        @Override
        public EntityModel getModel() {
            return entityModel;
        }

        @Override
        public EntityCache getCache() {
            return entityCache;
        }

        @Override
        public Platform getPlatform() {
            checkConnectionMetadata();
            return platform;
        }

        @Override
        public StatementGenerator getStatementGenerator() {
            if (statementGenerator == null) {
                statementGenerator = StatementGenerator.create(getPlatform());
            }
            return statementGenerator;
        }

        @Override
        public StatementListener getStatementListener() {
            return statementListeners;
        }

        @Override
        public Set<Supplier<TransactionListener>> getTransactionListenerFactories() {
            return configuration.getTransactionListenerFactories();
        }

        @Override
        public TransactionProvider getTransactionProvider() {
            return transactionProvider;
        }

        @Override
        public TransactionMode getTransactionMode() {
            checkConnectionMetadata();
            return transactionMode;
        }

        @Override
        public TransactionIsolation getTransactionIsolation() {
            return configuration.getTransactionIsolation();
        }

        @Override
        public Executor getWriteExecutor() {
            return configuration.getWriteExecutor();
        }
    }
}
