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

package io.requery;

import io.requery.meta.Attribute;

import javax.annotation.CheckReturnValue;
import java.util.concurrent.Callable;

/**
 * Synchronous version of {@link EntityStore} in which the entity objects are returned directly
 * after the successful completion of the operation and all modification operations are blocking.
 *
 * @param <T> base entity type
 */
public interface BlockingEntityStore<T> extends EntityStore<T, Object>, Transactionable<Object> {

    @Override
    <E extends T> E insert(E entity);

    @Override
    <E extends T> Iterable<E> insert(Iterable<E> entities);

    @Override
    <K, E extends T> K insert(E entity, Class<K> keyClass);

    @Override
    <K, E extends T> Iterable<K> insert(Iterable<E> entities, Class<K> keyClass);

    @Override
    <E extends T> E update(E entity);

    @Override
    <E extends T> Iterable<E> update(Iterable<E> entities);

    @Override
    <E extends T> E upsert(E entity);

    @Override
    <E extends T> Iterable<E> upsert(Iterable<E> entities);

    @Override
    <E extends T> E refresh(E entity);

    @Override
    <E extends T> E refresh(E entity, Attribute<?, ?>... attributes);

    @Override
    <E extends T> Iterable<E> refresh(Iterable<E> entities, Attribute<?, ?>... attributes);

    @Override
    <E extends T> E refreshAll(E entity);

    @Override
    <E extends T> Void delete(E entity);

    @Override
    <E extends T> Void delete(Iterable<E> entities);

    @Override
    @CheckReturnValue
    <E extends T, K> E findByKey(Class<E> type, K key);

    @Override
    <V> V runInTransaction(Callable<V> callable);

    @Override
    <V> V runInTransaction(Callable<V> callable, TransactionIsolation isolation);
}
