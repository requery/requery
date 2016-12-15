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

package io.requery.async;

import io.requery.BlockingEntityStore;
import io.requery.EntityStore;
import io.requery.meta.Attribute;
import io.requery.meta.QueryAttribute;
import io.requery.query.Deletion;
import io.requery.query.Expression;
import io.requery.query.Insertion;
import io.requery.query.Result;
import io.requery.query.Scalar;
import io.requery.query.Selection;
import io.requery.query.Tuple;
import io.requery.query.Update;
import io.requery.util.Objects;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

/**
 * Implementation of {@link CompletionStageEntityStore} where all operations are passed through a
 * {@link BlockingEntityStore} instance. All operations are supplied via the given {@link Executor}.
 *
 * @param <T> entity base type. See {@link EntityStore}.
 *
 * @author Nikhil Purushe
 */
@ParametersAreNonnullByDefault
public class CompletableEntityStore<T> implements CompletionStageEntityStore<T> {

    private final BlockingEntityStore<T> delegate;
    private final Executor executor;
    private final boolean createdExecutor;

    public CompletableEntityStore(BlockingEntityStore<T> delegate) {
        this.delegate = Objects.requireNotNull(delegate);
        this.executor = Executors.newSingleThreadExecutor();
        createdExecutor = true;
    }

    public CompletableEntityStore(BlockingEntityStore<T> delegate, Executor executor) {
        this.delegate = Objects.requireNotNull(delegate);
        this.executor = Objects.requireNotNull(executor);
        createdExecutor = false;
    }

    @Override
    public <E extends T> CompletableFuture<E> insert(final E entity) {
        return CompletableFuture.supplyAsync(new Supplier<E>() {
            @Override
            public E get() {
                return delegate.insert(entity);
            }
        }, executor);
    }

    @Override
    public <E extends T> CompletableFuture<Iterable<E>> insert(final Iterable<E> entities) {
        return CompletableFuture.supplyAsync(new Supplier<Iterable<E>>() {
            @Override
            public Iterable<E> get() {
                return delegate.insert(entities);
            }
        }, executor);
    }

    @Override
    public <K, E extends T> CompletionStage<K> insert(final E entity, final Class<K> keyClass) {
        return CompletableFuture.supplyAsync(new Supplier<K>() {
            @Override
            public K get() {
                return delegate.insert(entity, keyClass);
            }
        }, executor);
    }

    @Override
    public <K, E extends T> CompletionStage<Iterable<K>> insert(final Iterable<E> entities,
                                                                final Class<K> keyClass) {
        return CompletableFuture.supplyAsync(new Supplier<Iterable<K>>() {
            @Override
            public Iterable<K> get() {
                return delegate.insert(entities, keyClass);
            }
        }, executor);
    }

    @Override
    public <E extends T> CompletableFuture<E> update(final E entity) {
        return CompletableFuture.supplyAsync(new Supplier<E>() {
            @Override
            public E get() {
                return delegate.update(entity);
            }
        }, executor);
    }

    @Override
    public <E extends T> CompletionStage<E> update(final E entity,
                                                   final Attribute<?, ?>... attributes) {
        return CompletableFuture.supplyAsync(new Supplier<E>() {
            @Override
            public E get() {
                return delegate.update(entity, attributes);
            }
        }, executor);
    }

    @Override
    public <E extends T> CompletableFuture<Iterable<E>> update(final Iterable<E> entities) {
        return CompletableFuture.supplyAsync(new Supplier<Iterable<E>>() {
            @Override
            public Iterable<E> get() {
                return delegate.update(entities);
            }
        }, executor);
    }

    @Override
    public <E extends T> CompletionStage<E> upsert(final E entity) {
        return CompletableFuture.supplyAsync(new Supplier<E>() {
            @Override
            public E get() {
                return delegate.upsert(entity);
            }
        }, executor);
    }

    @Override
    public <E extends T> CompletableFuture<Iterable<E>> upsert(final Iterable<E> entities) {
        return CompletableFuture.supplyAsync(new Supplier<Iterable<E>>() {
            @Override
            public Iterable<E> get() {
                return delegate.upsert(entities);
            }
        }, executor);
    }

    @Override
    public <E extends T> CompletableFuture<E> refresh(final E entity) {
        return CompletableFuture.supplyAsync(new Supplier<E>() {
            @Override
            public E get() {
                return delegate.refresh(entity);
            }
        }, executor);
    }

    @Override
    public <E extends T> CompletableFuture<E> refresh(final E entity,
                                                      final Attribute<?, ?>... attributes) {
        return CompletableFuture.supplyAsync(new Supplier<E>() {
            @Override
            public E get() {
                return delegate.refresh(entity, attributes);
            }
        }, executor);
    }

    @Override
    public <E extends T> CompletionStage<Iterable<E>> refresh(final Iterable<E> entities,
                                                              final Attribute<?, ?>... attributes) {
        return CompletableFuture.supplyAsync(new Supplier<Iterable<E>>() {
            @Override
            public Iterable<E> get() {
                return delegate.refresh(entities, attributes);
            }
        }, executor);
    }

    @Override
    public <E extends T> CompletableFuture<E> refreshAll(final E entity) {
        return CompletableFuture.supplyAsync(new Supplier<E>() {
            @Override
            public E get() {
                return delegate.refreshAll(entity);
            }
        }, executor);
    }

    @Override
    public <E extends T> CompletableFuture<Void> delete(final E entity) {
        return CompletableFuture.runAsync(new Runnable() {
            @Override
            public void run() {
                delegate.delete(entity);
            }
        }, executor);
    }

    @Override
    public <E extends T> CompletableFuture<Void> delete(final Iterable<E> entities) {
        return CompletableFuture.runAsync(new Runnable() {
            @Override
            public void run() {
                delegate.delete(entities);
            }
        }, executor);
    }

    @Override
    public <E extends T, K> CompletableFuture<E> findByKey(final Class<E> type, final K key) {
        return CompletableFuture.supplyAsync(new Supplier<E>() {
            @Override
            public E get() {
                return delegate.findByKey(type, key);
            }
        }, executor);
    }

    @Override
    public void close() {
        try {
            if (createdExecutor) {
                ExecutorService executorService = (ExecutorService) executor;
                executorService.shutdown();
            }
        } finally {
            delegate.close();
        }
    }

    @Override
    public Selection<? extends Result<Tuple>> select(Expression<?>... expressions) {
        return delegate.select(expressions);
    }

    @Override
    public Selection<? extends Result<Tuple>> select(Set<? extends Expression<?>> expressions) {
        return delegate.select(expressions);
    }

    @Override
    public Update<? extends Scalar<Integer>> update() {
        return delegate.update();
    }

    @Override
    public Deletion<? extends Scalar<Integer>> delete() {
        return delegate.delete();
    }

    @Override
    public <E extends T> Selection<? extends Result<E>> select(Class<E> type,
                                                     QueryAttribute<?, ?>... attributes) {
        return delegate.select(type, attributes);
    }

    @Override
    public <E extends T> Selection<? extends Result<E>> select(
        Class<E> type, Set<? extends QueryAttribute<E, ?>> attributes) {
        return delegate.select(type, attributes);
    }

    @Override
    public <E extends T> Insertion<? extends Result<Tuple>> insert(Class<E> type) {
        return delegate.insert(type);
    }

    @Override
    public <E extends T> Update<? extends Scalar<Integer>> update(Class<E> type) {
        return delegate.update(type);
    }

    @Override
    public <E extends T> Deletion<? extends Scalar<Integer>> delete(Class<E> type) {
        return delegate.delete(type);
    }

    @Override
    public <E extends T> Selection<? extends Scalar<Integer>> count(Class<E> type) {
        return delegate.count(type);
    }

    @Override
    public Selection<? extends Scalar<Integer>> count(QueryAttribute<?, ?>... attributes) {
        return delegate.count(attributes);
    }

    @Override
    public Result<Tuple> raw(String query, Object... parameters) {
        return delegate.raw(query, parameters);
    }

    @Override
    public <E extends T> Result<E> raw(Class<E> type, String query, Object... parameters) {
        return delegate.raw(type, query, parameters);
    }

    @Override
    public BlockingEntityStore<T> toBlocking() {
        return delegate;
    }
}
