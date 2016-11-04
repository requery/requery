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

package io.requery.reactivex;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.requery.BlockingEntityStore;
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
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;

/**
 * Implementation of {@link ReactiveEntityStore} where all operations are passed through a
 * {@link BlockingEntityStore} instance. All observables are 'cold' and must be subscribed to
 * invoke the operation.
 *
 * @param <T> base type of all entities
 *
 * @author Nikhil Purushe
 */
@ParametersAreNonnullByDefault
class WrappedEntityStore<T> extends ReactiveEntityStore<T> {

    private final BlockingEntityStore<T> delegate;

    WrappedEntityStore(BlockingEntityStore<T> delegate) {
        this.delegate = Objects.requireNotNull(delegate);
    }

    @Override
    public <E extends T> Single<E> insert(final E entity) {
        return Single.fromCallable(new Callable<E>() {
            @Override
            public E call() throws Exception {
                return delegate.insert(entity);
            }
        });
    }

    @Override
    public <E extends T> Single<Iterable<E>> insert(final Iterable<E> entities) {
        return Single.fromCallable(new Callable<Iterable<E>>() {
            @Override
            public Iterable<E> call() throws Exception {
                return delegate.insert(entities);
            }
        });
    }

    @Override
    public <K, E extends T> Single<K> insert(final E entity, final Class<K> keyClass) {
        return Single.fromCallable(new Callable<K>() {
            @Override
            public K call() throws Exception {
                return delegate.insert(entity, keyClass);
            }
        });
    }

    @Override
    public <K, E extends T> Single<Iterable<K>> insert(final Iterable<E> entities,
                                                       final Class<K> keyClass) {
        return Single.fromCallable(new Callable<Iterable<K>>() {
            @Override
            public Iterable<K> call() throws Exception {
                return delegate.insert(entities, keyClass);
            }
        });
    }

    @Override
    public <E extends T> Single<E> update(final E entity) {
        return Single.fromCallable(new Callable<E>() {
            @Override
            public E call() throws Exception {
                return delegate.update(entity);
            }
        });
    }

    @Override
    public <E extends T> Single<E> update(final E entity, final Attribute<?, ?>... attributes) {
        return Single.fromCallable(new Callable<E>() {
            @Override
            public E call() throws Exception {
                return delegate.update(entity, attributes);
            }
        });
    }

    @Override
    public <E extends T> Single<Iterable<E>> update(final Iterable<E> entities) {
        return Single.fromCallable(new Callable<Iterable<E>>() {
            @Override
            public Iterable<E> call() throws Exception {
                return delegate.update(entities);
            }
        });
    }

    @Override
    public <E extends T> Single<E> upsert(final E entity) {
        return Single.fromCallable(new Callable<E>() {
            @Override
            public E call() throws Exception {
                return delegate.upsert(entity);
            }
        });
    }

    @Override
    public <E extends T> Single<Iterable<E>> upsert(final Iterable<E> entities) {
        return Single.fromCallable(new Callable<Iterable<E>>() {
            @Override
            public Iterable<E> call() throws Exception {
                return delegate.upsert(entities);
            }
        });
    }

    @Override
    public <E extends T> Single<E> refresh(final E entity) {
        return Single.fromCallable(new Callable<E>() {
            @Override
            public E call() throws Exception {
                return delegate.refresh(entity);
            }
        });
    }

    @Override
    public <E extends T> Single<E> refresh(final E entity,
                                           final Attribute<?, ?>... attributes) {
        return Single.fromCallable(new Callable<E>() {
            @Override
            public E call() throws Exception {
                return delegate.refresh(entity, attributes);
            }
        });
    }

    @Override
    public <E extends T> Single<Iterable<E>> refresh(final Iterable<E> entities,
                                                     final Attribute<?, ?>... attributes) {
        return Single.fromCallable(new Callable<Iterable<E>>() {
            @Override
            public Iterable<E> call() throws Exception {
                return delegate.refresh(entities, attributes);
            }
        });
    }

    @Override
    public <E extends T> Single<E> refreshAll(final E entity) {
        return Single.fromCallable(new Callable<E>() {
            @Override
            public E call() throws Exception {
                return delegate.refreshAll(entity);
            }
        });
    }

    @Override
    public <E extends T> Completable delete(final E entity) {
        return Completable.fromCallable(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                delegate.delete(entity);
                return null;
            }
        });
    }

    @Override
    public <E extends T> Completable delete(final Iterable<E> entities) {
        return Completable.fromCallable(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                delegate.delete(entities);
                return null;
            }
        });
    }

    @Override
    public <E extends T, K> Single<E> findByKey(final Class<E> type, final K key) {
        return Single.fromCallable(new Callable<E>() {
            @Override
            public E call() throws Exception {
                return delegate.findByKey(type, key);
            }
        });
    }

    @Override
    public void close() {
        delegate.close();
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

    @Override
    public final <E> Observable<E> runInTransaction(final List<Single<? extends E>> elements) {
        Objects.requireNotNull(elements);
        Observable<E> startTransaction = Completable.fromCallable(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                if (!delegate.transaction().active()) {
                    delegate.transaction().begin();
                }
                return delegate;
            }
        }).toObservable();

        Observable<E> commitTransaction = Completable.fromCallable(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                try {
                    delegate.transaction().commit();
                } finally {
                    delegate.transaction().close();
                }
                return delegate;
            }
        }).toObservable();

        Observable<E> current = startTransaction;
        for (Single<? extends E> single : elements) {
            current = current.concatWith(single.toObservable());
        }
        return current.concatWith(commitTransaction);
    }
}
