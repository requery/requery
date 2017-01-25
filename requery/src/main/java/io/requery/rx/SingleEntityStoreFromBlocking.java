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
import rx.Completable;
import rx.Observable;
import rx.Scheduler;
import rx.Single;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;

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
@ParametersAreNonnullByDefault
class SingleEntityStoreFromBlocking<T> extends SingleEntityStore<T> {

    private final BlockingEntityStore<T> delegate;

    SingleEntityStoreFromBlocking(BlockingEntityStore<T> delegate) {
        this.delegate = Objects.requireNotNull(delegate);
    }

    @Override
    public <E extends T> Single<E> insert(final E entity) {
        return Single.fromCallable(new Callable<E>() {
            @Override
            public E call() {
                return delegate.insert(entity);
            }
        });
    }

    @Override
    public <E extends T> Single<Iterable<E>> insert(final Iterable<E> entities) {
        return Single.fromCallable(new Callable<Iterable<E>>() {
            @Override
            public Iterable<E> call() {
                return delegate.insert(entities);
            }
        });
    }

    @Override
    public <K, E extends T> Single<K> insert(final E entity, final Class<K> keyClass) {
        return Single.fromCallable(new Callable<K>() {
            @Override
            public K call() {
                return delegate.insert(entity, keyClass);
            }
        });
    }

    @Override
    public <K, E extends T> Single<Iterable<K>> insert(final Iterable<E> entities,
                                                       final Class<K> keyClass) {
        return Single.fromCallable(new Callable<Iterable<K>>() {
            @Override
            public Iterable<K> call() {
                return delegate.insert(entities, keyClass);
            }
        });
    }

    @Override
    public <E extends T> Single<E> update(final E entity) {
        return Single.fromCallable(new Callable<E>() {
            @Override
            public E call() {
                return delegate.update(entity);
            }
        });
    }

    @Override
    public <E extends T> Single<E> update(final E entity, final Attribute<?, ?>... attributes) {
        return Single.fromCallable(new Callable<E>() {
            @Override
            public E call() {
                return delegate.update(entity, attributes);
            }
        });
    }

    @Override
    public <E extends T> Single<Iterable<E>> update(final Iterable<E> entities) {
        return Single.fromCallable(new Callable<Iterable<E>>() {
            @Override
            public Iterable<E> call() {
                return delegate.update(entities);
            }
        });
    }

    @Override
    public <E extends T> Single<E> upsert(final E entity) {
        return Single.fromCallable(new Callable<E>() {
            @Override
            public E call() {
                return delegate.upsert(entity);
            }
        });
    }

    @Override
    public <E extends T> Single<Iterable<E>> upsert(final Iterable<E> entities) {
        return Single.fromCallable(new Callable<Iterable<E>>() {
            @Override
            public Iterable<E> call() {
                return delegate.upsert(entities);
            }
        });
    }

    @Override
    public <E extends T> Single<E> refresh(final E entity) {
        return Single.fromCallable(new Callable<E>() {
            @Override
            public E call() {
                return delegate.refresh(entity);
            }
        });
    }

    @Override
    public <E extends T> Single<E> refresh(final E entity, final Attribute<?, ?>... attributes) {
        return Single.fromCallable(new Callable<E>() {
            @Override
            public E call() {
                return delegate.refresh(entity, attributes);
            }
        });
    }

    @Override
    public <E extends T> Single<Iterable<E>> refresh(final Iterable<E> entities,
                                                     final Attribute<?, ?>... attributes) {
        return Single.fromCallable(new Callable<Iterable<E>>() {
            @Override
            public Iterable<E> call() {
                return delegate.refresh(entities, attributes);
            }
        });
    }

    @Override
    public <E extends T> Single<E> refreshAll(final E entity) {
        return Single.fromCallable(new Callable<E>() {
            @Override
            public E call() {
                return delegate.refreshAll(entity);
            }
        });
    }

    @Override
    public <E extends T> Single<Void> delete(final E entity) {
        return Single.fromCallable(new Callable<Void>() {
            @Override
            public Void call() {
                delegate.delete(entity);
                return null;
            }
        });
    }

    @Override
    public <E extends T> Single<Void> delete(final Iterable<E> entities) {
        return Single.fromCallable(new Callable<Void>() {
            @Override
            public Void call() {
                delegate.delete(entities);
                return null;
            }
        });
    }

    @Override
    public <E extends T, K> Single<E> findByKey(final Class<E> type, final K key) {
        return Single.fromCallable(new Callable<E>() {
            @Override
            public E call() {
                return delegate.findByKey(type, key);
            }
        });
    }

    @Override
    public void close() {
        delegate.close();
    }

    @Override
    public Selection<RxResult<Tuple>> select(Expression<?>... attributes) {
        return result(delegate.select(attributes));
    }

    @Override
    public Selection<RxResult<Tuple>> select(Set<? extends Expression<?>> expressions) {
        return result(delegate.select(expressions));
    }

    @Override
    public Update<RxScalar<Integer>> update() {
        return scalar(delegate.update());
    }

    @Override
    public Deletion<RxScalar<Integer>> delete() {
        return scalar(delegate.delete());
    }

    @Override
    public <E extends T> Selection<RxResult<E>> select(
        Class<E> type, QueryAttribute<?, ?>... attributes) {
        return result(delegate.select(type, attributes));
    }

    @Override
    public <E extends T> Selection<RxResult<E>> select(
        Class<E> type, Set<? extends QueryAttribute<E, ?>> attributes) {
        return result(delegate.select(type, attributes));
    }

    @Override
    public <E extends T> Insertion<RxResult<Tuple>> insert(Class<E> type) {
        return result(delegate.insert(type));
    }

    @Override
    public <E extends T> Update<RxScalar<Integer>> update(Class<E> type) {
        return scalar(delegate.update(type));
    }

    @Override
    public <E extends T> Deletion<RxScalar<Integer>> delete(Class<E> type) {
        return scalar(delegate.delete(type));
    }

    @Override
    public <E extends T> Selection<RxScalar<Integer>> count(Class<E> type) {
        return scalar(delegate.count(type));
    }

    @Override
    public Selection<RxScalar<Integer>> count(QueryAttribute<?, ?>... attributes) {
        return scalar(delegate.count(attributes));
    }

    @Override
    public RxResult<Tuple> raw(String query, Object... parameters) {
        return new RxResult<>(delegate.raw(query, parameters));
    }

    @Override
    public <E extends T> RxResult<E> raw(Class<E> type, String query, Object... parameters) {
        return new RxResult<>(delegate.raw(type, query, parameters));
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

    private static <E> QueryElement<RxResult<E>> result(Return<? extends Result<E>> query) {
        @SuppressWarnings("unchecked")
        QueryElement<Result<E>> element = (QueryElement<Result<E>>) query;
        return element.extend(new Function<Result<E>, RxResult<E>>() {
            @Override
            public RxResult<E> apply(Result<E> result) {
                return new RxResult<>(result);
            }
        });
    }

    private static <E> QueryElement<RxScalar<E>> scalar(Return<? extends Scalar<E>> query) {
        @SuppressWarnings("unchecked")
        QueryElement<Scalar<E>> element = (QueryElement<Scalar<E>>) query;
        return element.extend(new Function<Scalar<E>, RxScalar<E>>() {
            @Override
            public RxScalar<E> apply(Scalar<E> result) {
                return new RxScalar<>(result);
            }
        });
    }
}
