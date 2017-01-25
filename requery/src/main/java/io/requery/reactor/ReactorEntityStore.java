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

package io.requery.reactor;

import io.requery.BlockingEntityStore;
import io.requery.EntityStore;
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
import reactor.core.publisher.Mono;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Set;
import java.util.concurrent.Callable;

/**
 * Extends {@link EntityStore} where all return values are {@link Mono} instances.
 *
 * @param <T> entity base type. See {@link EntityStore}.
 */
@ParametersAreNonnullByDefault
public class ReactorEntityStore<T> implements EntityStore<T, Object>, ReactorQueryable<T> {

    private final BlockingEntityStore<T> delegate;

    public ReactorEntityStore(BlockingEntityStore<T> delegate) {
        this.delegate = Objects.requireNotNull(delegate);
    }

    @Override
    public <E extends T> Mono<E> insert(final E entity) {
        return Mono.fromCallable(new Callable<E>() {
            @Override
            public E call() throws Exception {
                return delegate.insert(entity);
            }
        });
    }

    @Override
    public <E extends T> Mono<Iterable<E>> insert(final Iterable<E> entities) {
        return Mono.fromCallable(new Callable<Iterable<E>>() {
            @Override
            public Iterable<E> call() throws Exception {
                return delegate.insert(entities);
            }
        });
    }

    @Override
    public <K, E extends T> Mono<K> insert(final E entity, final Class<K> keyClass) {
        return Mono.fromCallable(new Callable<K>() {
            @Override
            public K call() throws Exception {
                return delegate.insert(entity, keyClass);
            }
        });
    }

    @Override
    public <K, E extends T> Mono<Iterable<K>> insert(final Iterable<E> entities, final Class<K> keyClass) {
        return Mono.fromCallable(new Callable<Iterable<K>>() {
            @Override
            public Iterable<K> call() throws Exception {
                return delegate.insert(entities, keyClass);
            }
        });
    }

    @Override
    public <E extends T> Mono<E> update(final E entity) {
        return Mono.fromCallable(new Callable<E>() {
            @Override
            public E call() throws Exception {
                return delegate.update(entity);
            }
        });
    }

    @Override
    public <E extends T> Mono<E> update(final E entity, final Attribute<?, ?>... attributes) {
        return Mono.fromCallable(new Callable<E>() {
            @Override
            public E call() throws Exception {
                return delegate.update(entity, attributes);
            }
        });
    }

    @Override
    public <E extends T> Mono<Iterable<E>> update(final Iterable<E> entities) {
        return Mono.fromCallable(new Callable<Iterable<E>>() {
            @Override
            public Iterable<E> call() throws Exception {
                return delegate.update(entities);
            }
        });
    }

    @Override
    public <E extends T> Mono<E> upsert(final E entity) {
        return Mono.fromCallable(new Callable<E>() {
            @Override
            public E call() throws Exception {
                return delegate.upsert(entity);
            }
        });
    }

    @Override
    public <E extends T> Mono<Iterable<E>> upsert(final Iterable<E> entities) {
        return Mono.fromCallable(new Callable<Iterable<E>>() {
            @Override
            public Iterable<E> call() throws Exception {
                return delegate.upsert(entities);
            }
        });
    }

    @Override
    public <E extends T> Mono<E> refresh(final E entity) {
        return Mono.fromCallable(new Callable<E>() {
            @Override
            public E call() throws Exception {
                return delegate.refresh(entity);
            }
        });
    }

    @Override
    public <E extends T> Mono<E> refresh(final E entity, final Attribute<?, ?>... attributes) {
        return Mono.fromCallable(new Callable<E>() {
            @Override
            public E call() throws Exception {
                return delegate.refresh(entity, attributes);
            }
        });
    }

    @Override
    public <E extends T> Mono<Iterable<E>> refresh(final Iterable<E> entities, final Attribute<?, ?>... attributes) {
        return Mono.fromCallable(new Callable<Iterable<E>>() {
            @Override
            public Iterable<E> call() throws Exception {
                return delegate.refresh(entities, attributes);
            }
        });
    }

    @Override
    public <E extends T> Mono<E> refreshAll(final E entity) {
        return Mono.fromCallable(new Callable<E>() {
            @Override
            public E call() throws Exception {
                return delegate.refreshAll(entity);
            }
        });
    }

    @Override
    public <E extends T> Mono<Void> delete(final E entity) {
        return Mono.fromRunnable(new Runnable() {
            @Override
            public void run() {
                delegate.delete(entity);
            }
        });
    }

    @Override
    public <E extends T> Mono<Void> delete(final Iterable<E> entities) {
        return Mono.fromRunnable(new Runnable() {
            @Override
            public void run() {
                delegate.delete(entities);
            }
        });
    }

    @Override
    public <E extends T, K> Mono<E> findByKey(final Class<E> type, final K key) {
        return Mono.fromCallable(new Callable<E>() {
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
    public Selection<ReactorResult<Tuple>> select(Expression<?>... attributes) {
        return result(delegate.select(attributes));
    }

    @Override
    public Selection<ReactorResult<Tuple>> select(Set<? extends Expression<?>> expressions) {
        return result(delegate.select(expressions));
    }

    @Override
    public Update<ReactorScalar<Integer>> update() {
        return scalar(delegate.update());
    }

    @Override
    public Deletion<ReactorScalar<Integer>> delete() {
        return scalar(delegate.delete());
    }

    @Override
    public <E extends T> Selection<ReactorResult<E>> select(
        Class<E> type, QueryAttribute<?, ?>... attributes) {
        return result(delegate.select(type, attributes));
    }

    @Override
    public <E extends T> Selection<ReactorResult<E>> select(
        Class<E> type, Set<? extends QueryAttribute<E, ?>> attributes) {
        return result(delegate.select(type, attributes));
    }

    @Override
    public <E extends T> Insertion<ReactorResult<Tuple>> insert(Class<E> type) {
        return result(delegate.insert(type));
    }

    @Override
    public <E extends T> Update<ReactorScalar<Integer>> update(Class<E> type) {
        return scalar(delegate.update(type));
    }

    @Override
    public <E extends T> Deletion<ReactorScalar<Integer>> delete(Class<E> type) {
        return scalar(delegate.delete(type));
    }

    @Override
    public <E extends T> Selection<ReactorScalar<Integer>> count(Class<E> type) {
        return scalar(delegate.count(type));
    }

    @Override
    public Selection<ReactorScalar<Integer>> count(QueryAttribute<?, ?>... attributes) {
        return scalar(delegate.count(attributes));
    }

    @Override
    public ReactorResult<Tuple> raw(String query, Object... parameters) {
        return new ReactorResult<>(delegate.raw(query, parameters));
    }

    @Override
    public <E extends T> ReactorResult<E> raw(Class<E> type, String query, Object... parameters) {
        return new ReactorResult<>(delegate.raw(type, query, parameters));
    }

    @Override
    public BlockingEntityStore<T> toBlocking() {
        return delegate;
    }

    private static <E> QueryElement<ReactorResult<E>> result(Return<? extends Result<E>> query) {
        @SuppressWarnings("unchecked")
        QueryElement<Result<E>> element = (QueryElement<Result<E>>) query;
        return element.extend(new Function<Result<E>, ReactorResult<E>>() {
            @Override
            public ReactorResult<E> apply(Result<E> result) {
                return new ReactorResult<>(result);
            }
        });
    }

    private static <E> QueryElement<ReactorScalar<E>> scalar(Return<? extends Scalar<E>> query) {
        @SuppressWarnings("unchecked")
        QueryElement<Scalar<E>> element = (QueryElement<Scalar<E>>) query;
        return element.extend(new Function<Scalar<E>, ReactorScalar<E>>() {
            @Override
            public ReactorScalar<E> apply(Scalar<E> result) {
                return new ReactorScalar<>(result);
            }
        });
    }
}
