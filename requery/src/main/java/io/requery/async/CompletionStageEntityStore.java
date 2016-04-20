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

import io.requery.EntityStore;
import io.requery.meta.Attribute;

import javax.annotation.CheckReturnValue;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.concurrent.CompletionStage;

/**
 * Extends {@link EntityStore} where all return values are {@link CompletionStage} instances
 * representing the outcome of each operation.
 *
 * @param <T> base type of all entities
 *
 * @author Nikhil Purushe
 */
@ParametersAreNonnullByDefault
public interface CompletionStageEntityStore<T> extends EntityStore<T, CompletionStage<?>> {

    @Override
    @CheckReturnValue
    <E extends T> CompletionStage<E> insert(E entity);

    @Override
    @CheckReturnValue
    <E extends T> CompletionStage<Iterable<E>> insert(Iterable<E> entities);

    @Override
    @CheckReturnValue
    <K, E extends T> CompletionStage<K> insert(E entity, Class<K> keyClass);

    @Override
    @CheckReturnValue
    <K, E extends T> CompletionStage<Iterable<K>> insert(Iterable<E> entities, Class<K> keyClass);

    @Override
    @CheckReturnValue
    <E extends T> CompletionStage<E> update(E entity);

    @Override
    @CheckReturnValue
    <E extends T> CompletionStage<Iterable<E>> update(Iterable<E> entities);

    @Override
    @CheckReturnValue
    <E extends T> CompletionStage<E> upsert(E entity);

    @Override
    @CheckReturnValue
    <E extends T> CompletionStage<Iterable<E>> upsert(Iterable<E> entities);

    @Override
    @CheckReturnValue
    <E extends T> CompletionStage<E> refresh(E entity);

    @Override
    @CheckReturnValue
    <E extends T> CompletionStage<E> refresh(E entity, Attribute<?, ?>... attributes);

    @Override
    @CheckReturnValue
    <E extends T> CompletionStage<Iterable<E>> refresh(Iterable<E> entities,
                                                       Attribute<?, ?>... attributes);
    @Override
    @CheckReturnValue
    <E extends T> CompletionStage<E> refreshAll(E entity);

    @Override
    @CheckReturnValue
    <E extends T> CompletionStage<Void> delete(E entity);

    @Override
    @CheckReturnValue
    <E extends T> CompletionStage<Void> delete(Iterable<E> entities);

    @Override
    @CheckReturnValue
    <E extends T, K> CompletionStage<E> findByKey(Class<E> type, K key);
}
