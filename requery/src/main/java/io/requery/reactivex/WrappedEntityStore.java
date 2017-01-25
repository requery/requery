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
import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.requery.BlockingEntityStore;
import io.requery.meta.Attribute;
import io.requery.meta.QueryAttribute;
import io.requery.query.Deletion;
import io.requery.query.Expression;
import io.requery.query.Insertion;
import io.requery.query.Result;
import io.requery.query.Return;
import io.requery.query.Scalar;
import io.requery.query.Selection;
import io.requery.query.Tuple;
import io.requery.query.Update;
import io.requery.query.element.QueryElement;
import io.requery.util.Objects;
import io.requery.util.function.Function;

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
    public <E extends T, K> Maybe<E> findByKey(final Class<E> type, final K key) {
        return Maybe.fromCallable(new Callable<E>() {
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
    public Selection<ReactiveResult<Tuple>> select(Expression<?>... attributes) {
        return result(delegate.select(attributes));
    }

    @Override
    public Selection<ReactiveResult<Tuple>> select(Set<? extends Expression<?>> expressions) {
        return result(delegate.select(expressions));
    }

    @Override
    public Update<ReactiveScalar<Integer>> update() {
        return scalar(delegate.update());
    }

    @Override
    public Deletion<ReactiveScalar<Integer>> delete() {
        return scalar(delegate.delete());
    }

    @Override
    public <E extends T> Selection<ReactiveResult<E>> select(
        Class<E> type, QueryAttribute<?, ?>... attributes) {
        return result(delegate.select(type, attributes));
    }

    @Override
    public <E extends T> Selection<ReactiveResult<E>> select(
        Class<E> type, Set<? extends QueryAttribute<E, ?>> attributes) {
        return result(delegate.select(type, attributes));
    }

    @Override
    public <E extends T> Insertion<ReactiveResult<Tuple>> insert(Class<E> type) {
        return result(delegate.insert(type));
    }

    @Override
    public <E extends T> Update<ReactiveScalar<Integer>> update(Class<E> type) {
        return scalar(delegate.update(type));
    }

    @Override
    public <E extends T> Deletion<ReactiveScalar<Integer>> delete(Class<E> type) {
        return scalar(delegate.delete(type));
    }

    @Override
    public <E extends T> Selection<ReactiveScalar<Integer>> count(Class<E> type) {
        return scalar(delegate.count(type));
    }

    @Override
    public Selection<ReactiveScalar<Integer>> count(QueryAttribute<?, ?>... attributes) {
        return scalar(delegate.count(attributes));
    }

    @Override
    public ReactiveResult<Tuple> raw(String query, Object... parameters) {
        return new ReactiveResult<>(delegate.raw(query, parameters));
    }

    @Override
    public <E extends T> ReactiveResult<E> raw(Class<E> type, String query, Object... parameters) {
        return new ReactiveResult<>(delegate.raw(type, query, parameters));
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

    private static <E> QueryElement<ReactiveResult<E>> result(Return<? extends Result<E>> query) {
        @SuppressWarnings("unchecked")
        QueryElement<Result<E>> element = (QueryElement<Result<E>>) query;
        return element.extend(new Function<Result<E>, ReactiveResult<E>>() {
            @Override
            public ReactiveResult<E> apply(Result<E> result) {
                return new ReactiveResult<>(result);
            }
        });
    }

    private static <E> QueryElement<ReactiveScalar<E>> scalar(Return<? extends Scalar<E>> query) {
        @SuppressWarnings("unchecked")
        QueryElement<Scalar<E>> element = (QueryElement<Scalar<E>>) query;
        return element.extend(new Function<Scalar<E>, ReactiveScalar<E>>() {
            @Override
            public ReactiveScalar<E> apply(Scalar<E> result) {
                return new ReactiveScalar<>(result);
            }
        });
    }
}
