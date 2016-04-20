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

import io.requery.EntityStore;
import io.requery.meta.Attribute;
import io.requery.query.Result;
import rx.Single;

import javax.annotation.CheckReturnValue;
import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Extends {@link EntityStore} where all return values are single {@link rx.Single} instances
 * representing the outcome of each operation. {@link rx.Observable} query results can be obtained
 * via {@link Result#toObservable()}.
 *
 * @param <T> entity base type. See {@link EntityStore}.
 *
 * @author Nikhil Purushe
 */
@ParametersAreNonnullByDefault
public interface SingleEntityStore<T> extends EntityStore<T, Single<?>> {

    @Override
    @CheckReturnValue
    <E extends T> Single<E> insert(E entity);

    @Override
    @CheckReturnValue
    <E extends T> Single<Iterable<E>> insert(Iterable<E> entities);

    @Override
    @CheckReturnValue
    <K, E extends T> Single<K> insert(E entity, Class<K> keyClass);

    @Override
    @CheckReturnValue
    <K, E extends T> Single<Iterable<K>> insert(Iterable<E> entities, Class<K> keyClass);

    @Override
    @CheckReturnValue
    <E extends T> Single<E> update(E entity);

    @Override
    @CheckReturnValue
    <E extends T> Single<Iterable<E>> update(Iterable<E> entities);

    @Override
    @CheckReturnValue
    <E extends T> Single<E> upsert(E entity);

    @Override
    @CheckReturnValue
    <E extends T> Single<Iterable<E>> upsert(Iterable<E> entities);

    @Override
    @CheckReturnValue
    <E extends T> Single<E> refresh(E entity);

    @Override
    @CheckReturnValue
    <E extends T> Single<E> refresh(E entity, Attribute<?, ?>... attributes);

    @Override
    @CheckReturnValue
    <E extends T> Single<Iterable<E>> refresh(Iterable<E> entities, Attribute<?, ?>... attributes);

    @Override
    @CheckReturnValue
    <E extends T> Single<E> refreshAll(E entity);

    @Override
    @CheckReturnValue
    <E extends T> Single<Void> delete(E entity);

    @Override
    @CheckReturnValue
    <E extends T> Single<Void> delete(Iterable<E> entities);

    @Override
    @CheckReturnValue
    <E extends T, K> Single<E> findByKey(Class<E> type, K key);
}
