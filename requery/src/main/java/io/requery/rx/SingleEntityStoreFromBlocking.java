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

package io.requery.rx;

import io.requery.BlockingEntityStore;
import io.requery.Transaction;
import io.requery.TransactionIsolation;
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
import io.requery.util.function.Supplier;
import rx.Scheduler;
import rx.Single;
import rx.schedulers.Schedulers;

import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Implementation of {@link SingleEntityStore} where all operations are passed through a
 * {@link BlockingEntityStore} instance. All observables are 'cold' and must be subscribed to
 * invoke the operation. All operations are subscribed on the given {@link Scheduler}, by default
 * to prevent inconsistent state its usually best if this a single threaded scheduler.
 *
 * @param <T> base type of all entities
 *
 * @author Nikhil Purushe
 */
class SingleEntityStoreFromBlocking<T> implements SingleEntityStore<T> {

    private final BlockingEntityStore<T> delegate;
    private final Scheduler subscribeOn;
    private final ExecutorService executor;
    private final boolean createdExecutor;

    SingleEntityStoreFromBlocking(BlockingEntityStore<T> delegate) {
        this(delegate, null);
    }

    SingleEntityStoreFromBlocking(BlockingEntityStore<T> delegate, Scheduler subscribeOn) {
        this.delegate = Objects.requireNotNull(delegate);
        if (subscribeOn == null) {
            createdExecutor = true;
            executor = Executors.newSingleThreadExecutor();
            this.subscribeOn = Schedulers.from(executor);
        } else {
            this.subscribeOn = subscribeOn;
            executor = null;
            createdExecutor = false;
        }
    }

    @Override
    public <E extends T> Single<E> insert(final E entity) {
        return RxSupport.toSingle(new Supplier<E>() {
            @Override
            public E get() {
                return delegate.insert(entity);
            }
        }, subscribeOn);
    }

    @Override
    public <E extends T> Single<Iterable<E>> insert(final Iterable<E> entities) {
        return RxSupport.toSingle(new Supplier<Iterable<E>>() {
            @Override
            public Iterable<E> get() {
                return delegate.insert(entities);
            }
        }, subscribeOn);
    }

    @Override
    public <E extends T> Single<E> update(final E entity) {
        return RxSupport.toSingle(new Supplier<E>() {
            @Override
            public E get() {
                return delegate.update(entity);
            }
        }, subscribeOn);
    }

    @Override
    public <E extends T> Single<E> refresh(final E entity) {
        return RxSupport.toSingle(new Supplier<E>() {
            @Override
            public E get() {
                return delegate.refresh(entity);
            }
        }, subscribeOn);
    }

    @Override
    public <E extends T> Single<E> refresh(final E entity,
                                           final Attribute<?, ?>... attributes) {
        return RxSupport.toSingle(new Supplier<E>() {
            @Override
            public E get() {
                return delegate.refresh(entity, attributes);
            }
        }, subscribeOn);
    }

    @Override
    public <E extends T> Single<Iterable<E>> refresh(final Iterable<E> entities,
                                                     final Attribute<?, ?>... attributes) {
        return RxSupport.toSingle(new Supplier<Iterable<E>>() {
            @Override
            public Iterable<E> get() {
                return delegate.refresh(entities, attributes);
            }
        }, subscribeOn);
    }

    @Override
    public <E extends T> Single<E> refreshAll(final E entity) {
        return RxSupport.toSingle(new Supplier<E>() {
            @Override
            public E get() {
                return delegate.refreshAll(entity);
            }
        }, subscribeOn);
    }

    @Override
    public <E extends T> Single<Void> delete(final E entity) {
        return RxSupport.toSingle(new Supplier<Void>() {
            @Override
            public Void get() {
                delegate.delete(entity);
                return null;
            }
        }, subscribeOn);
    }

    @Override
    public <E extends T> Single<Void> delete(final Iterable<E> entities) {
        return RxSupport.toSingle(new Supplier<Void>() {
            @Override
            public Void get() {
                delegate.delete(entities);
                return null;
            }
        }, subscribeOn);
    }

    @Override
    public <E extends T, K> Single<E> findByKey(final Class<E> type, final K key) {
        return RxSupport.toSingle(new Supplier<E>() {
            @Override
            public E get() {
                return delegate.findByKey(type, key);
            }
        }, subscribeOn);
    }

    @Override
    public Transaction transaction() {
        return null;
    }

    @Override
    public <V> Single<V> runInTransaction(Callable<V> callable) {
        return runInTransaction(callable, null);
    }

    @Override
    public <V> Single<V> runInTransaction(final Callable<V> callable,
                                          final TransactionIsolation isolation) {
        return Single.fromCallable(new Callable<V>() {
            @Override
            public V call() {
                return delegate.runInTransaction(callable, isolation);
            }
        }).compose(new Single.Transformer<V, V>() {
            @Override
            public Single<V> call(Single<V> single) {
                return subscribeOn == null ? single : single.subscribeOn(subscribeOn);
            }
        });
    }

    @Override
    public void close() {
        delegate.close();
        if (executor != null && createdExecutor) {
            executor.shutdown();
        }
    }

    @Override
    public Selection<Result<Tuple>> select(Expression<?>... attributes) {
        return delegate.select(attributes);
    }

    @Override
    public Selection<Result<Tuple>> select(Set<? extends Expression<?>> expressions) {
        return delegate.select(expressions);
    }

    @Override
    public Update<Scalar<Integer>> update() {
        return delegate.update();
    }

    @Override
    public Deletion<Scalar<Integer>> delete() {
        return delegate.delete();
    }

    @Override
    public <E extends T> Selection<Result<E>> select(
        Class<E> type, QueryAttribute<?, ?>... attributes) {
        return delegate.select(type, attributes);
    }

    @Override
    public <E extends T> Selection<Result<E>> select(
        Class<E> type, Set<? extends QueryAttribute<E, ?>> attributes) {
        return delegate.select(type, attributes);
    }

    @Override
    public <E extends T> Insertion<Result<Tuple>> insert(Class<E> type) {
        return delegate.insert(type);
    }

    @Override
    public <E extends T> Update<Scalar<Integer>> update(Class<E> type) {
        return delegate.update(type);
    }

    @Override
    public <E extends T> Deletion<Scalar<Integer>> delete(Class<E> type) {
        return delegate.delete(type);
    }

    @Override
    public <E extends T> Selection<Scalar<Integer>> count(Class<E> type) {
        return delegate.count(type);
    }

    @Override
    public Selection<Scalar<Integer>> count(QueryAttribute<?, ?>... attributes) {
        return delegate.count(attributes);
    }

    @Override
    public BlockingEntityStore<T> toBlocking() {
        return delegate;
    }
}
