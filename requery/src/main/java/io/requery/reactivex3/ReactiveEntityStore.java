/*
 * Copyright 2017 requery.io
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

package io.requery.reactivex3;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;
import io.requery.BlockingEntityStore;
import io.requery.EntityStore;
import io.requery.meta.Attribute;
import io.requery.util.function.Function;

import javax.annotation.CheckReturnValue;
import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Extends {@link EntityStore} where all return values are either single {@link Single} instances or
 * {@link Completable} representing the outcome of each operation. {@link Observable} query results
 * can be obtained via {@link ReactiveResult#observable()}.
 *
 * @param <T> entity base type. See {@link EntityStore}.
 */
@ParametersAreNonnullByDefault
public abstract class ReactiveEntityStore<T> implements EntityStore<T, Object>, ReactiveQueryable<T> {

    @Override
    @CheckReturnValue
    public abstract <E extends T> Single<E> insert(E entity);

    @Override
    @CheckReturnValue
    public abstract <E extends T> Single<Iterable<E>> insert(Iterable<E> entities);

    @Override
    @CheckReturnValue
    public abstract <K, E extends T> Single<K> insert(E entity, Class<K> keyClass);

    @Override
    @CheckReturnValue
    public abstract <K, E extends T> Single<Iterable<K>> insert(Iterable<E> entities, Class<K> keyClass);

    @Override
    @CheckReturnValue
    public abstract <E extends T> Single<E> update(E entity);

    @Override
    @CheckReturnValue
    public abstract <E extends T> Single<E> update(E entity, Attribute<?, ?>... attributes);

    @Override
    @CheckReturnValue
    public abstract <E extends T> Single<Iterable<E>> update(Iterable<E> entities);

    @Override
    @CheckReturnValue
    public abstract <E extends T> Single<E> upsert(E entity);

    @Override
    @CheckReturnValue
    public abstract <E extends T> Single<Iterable<E>> upsert(Iterable<E> entities);

    @Override
    @CheckReturnValue
    public abstract <E extends T> Single<E> refresh(E entity);

    @Override
    @CheckReturnValue
    public abstract <E extends T> Single<E> refresh(E entity, Attribute<?, ?>... attributes);

    @Override
    @CheckReturnValue
    public abstract <E extends T> Single<Iterable<E>> refresh(Iterable<E> entities, Attribute<?, ?>... attributes);

    @Override
    @CheckReturnValue
    public abstract <E extends T> Single<E> refreshAll(E entity);

    @Override
    @CheckReturnValue
    public abstract <E extends T> Completable delete(E entity);

    @Override
    @CheckReturnValue
    public abstract <E extends T> Completable delete(Iterable<E> entities);

    @Override
    @CheckReturnValue
    public abstract <E extends T, K> Maybe<E> findByKey(Class<E> type, K key);

    @CheckReturnValue
    public abstract <R> Single<R> runInTransaction(Function<BlockingEntityStore<T>, R> function);
} 