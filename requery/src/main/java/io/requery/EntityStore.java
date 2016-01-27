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
import io.requery.query.Result;

/**
 * The primary interface for interacting with {@link Entity} objects. This interface supports the
 * basic insert/update/delete operations.
 *
 * @param <T> the base class or interface to restrict all entities that are stored to (e.g.
 *            {@link Persistable} or {@link Object} or {@link java.io.Serializable} for instance.
 *            The purpose is to aid in the prevention of runtime errors by restricting the types at
 *            compile time.
 * @param <R> the operation return type. For an asynchronous implementation may return a
 *            {@link java.util.concurrent.Future} or other type of promise representing the result
 *            of the operation.
 * @author Nikhil Purushe
 */
public interface EntityStore<T, R> extends Queryable<T>, AutoCloseable {

    /**
     * Close the entity store. After close has been called all methods and any open {@link Result}
     * instances may throw a {@link PersistenceException}.
     */
    @Override
    void close();

    /**
     * Inserts the given entity. This entity must not have previously been inserted otherwise an
     * {@link PersistenceException} may be thrown.
     *
     * @param entity non null entity to insert
     * @param <E>    entity type
     * @return the operation result.
     */
    <E extends T> R insert(E entity);

    /**
     * Insert a collection of entities.
     *
     * @param entities to insert
     * @param <E>      entity type
     * @return the operation result.
     */
    <E extends T> R insert(Iterable<E> entities);

    /**
     * Update the given entity. If the given entity has modified properties those changes will be
     * persisted otherwise the method will do nothing.
     *
     * @param entity to update
     * @param <E>    entity type
     * @return the operation result.
     */
    <E extends T> R update(E entity);

    /**
     * Refresh the given entity. This refreshes the already loaded properties in the entity. If no
     * properties are loaded then the default properties will be loaded.
     *
     * @param entity to insert
     * @param <E>    entity type
     * @return the operation result.
     */
    <E extends T> R refresh(E entity);

    /**
     * Refresh the given entity on specific attributes.
     *
     * @param entity     to refresh
     * @param attributes attributes to refresh, attributes should be of type E
     *                   (not enforced due to erasure)
     * @param <E>        entity type
     * @return the operation result.
     */
    <E extends T> R refresh(E entity, Attribute<?, ?>... attributes);

    /**
     * Refresh the given entities on specific attributes.
     *
     * @param entities   to refresh
     * @param attributes attributes to refresh, attributes should be of type E
     *                   (not enforced due to erasure)
     * @param <E>        entity type
     * @return the operation result.
     */
    <E extends T> R refresh(Iterable<E> entities, Attribute<?, ?>... attributes);

    /**
     * Refresh the given entity on all of its attributes including relational ones.
     *
     * @param entity to refresh
     * @param <E>    entity type
     * @return the operation result.
     */
    <E extends T> R refreshAll(E entity);

    /**
     * Deletes the given entity from the store.
     *
     * @param entity to delete
     * @param <E>    entity type.
     * @return the operation result.
     */
    <E extends T> R delete(E entity);

    /**
     * Deletes multiple entities.
     *
     * @param entities to delete
     * @param <E>      entity type
     * @return the operation result.
     */
    <E extends T> R delete(Iterable<E> entities);

    /**
     * Find an entity by the given key. This differs from selecting the key using a query in that
     * the {@link EntityCache} if available may be checked first for the object. If an entity is
     * found in the cache it will be returned and potentially no query will be made.
     *
     * @param type non null entity class type
     * @param key  non null key value
     * @param <E>  entity type
     * @param <K>  key type
     * @return an operation returning the entity if found.
     */
    <E extends T, K> R findByKey(Class<E> type, K key);

    /**
     * @return a {@link BlockingEntityStore} version of this entity store. If the implementation
     * is already blocking may return itself.
     */
    BlockingEntityStore<T> toBlocking();
}
