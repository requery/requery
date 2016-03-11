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
import io.requery.TransactionIsolation;
import io.requery.Transactionable;
import io.requery.meta.Attribute;
import io.requery.query.Result;
import rx.Single;

import java.util.concurrent.Callable;

/**
 * Extends {@link EntityStore} where all return values are single {@link rx.Single} instances
 * representing the outcome of each operation. {@link rx.Observable} query results can be obtained
 * via {@link Result#toObservable()}.
 *
 * @param <T> entity base type. See {@link EntityStore}.
 *
 * @author Nikhil Purushe
 */
public interface SingleEntityStore<T> extends EntityStore<T, Single<?>>,
    Transactionable<Single<?>> {

    @Override
    <E extends T> Single<E> insert(E entity);

    @Override
    <E extends T> Single<Iterable<E>> insert(Iterable<E> entities);

    @Override
    <E extends T> Single<E> update(E entity);

    @Override
    <E extends T> Single<E> refresh(E entity);

    @Override
    <E extends T> Single<E> refresh(E entity, Attribute<?, ?>... attributes);

    @Override
    <E extends T> Single<Iterable<E>> refresh(Iterable<E> entities, Attribute<?, ?>... attributes);

    @Override
    <E extends T> Single<E> refreshAll(E entity);

    @Override
    <E extends T> Single<Void> delete(E entity);

    @Override
    <E extends T> Single<Void> delete(Iterable<E> entities);

    @Override
    <E extends T, K> Single<E> findByKey(Class<E> type, K key);

    @Override
    <V> Single<V> runInTransaction(Callable<V> callable);

    @Override
    <V> Single<V> runInTransaction(Callable<V> callable, TransactionIsolation isolation);
}
