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

package io.requery.kotlin

import io.requery.TransactionIsolation
import io.requery.meta.Attribute
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

/**
 * The primary interface for interacting with [io.requery.Entity] objects. This interface supports the
 * basic insert/update/delete operations.
 */
interface EntityStore<T : Any, out R> : Queryable<T>, AutoCloseable {

    override fun close()

    /**
     * Inserts the given entity. This entity must not have previously been inserted otherwise a
     * [io.requery.PersistenceException] may be thrown.
     */
    infix fun <E : T> insert(entity: E): R

    /**
     * Insert a collection of entities. This method may perform additional optimizations not
     * present in the single element insert method.
     */
    infix fun <E : T> insert(entities: Iterable<E>): R

    /**
     * Inserts the given entity returning the generated key after the entity is inserted. This
     * entity must not have previously been inserted otherwise an [io.requery.PersistenceException]
     * may be thrown.
     */
    fun <K : Any, E : T> insert(entity: E, keyClass: KClass<K>): R

    /**
     * Insert a collection of entities returning the generated keys for the inserted entities in
     * the order they were inserted.
     */
    fun <K : Any, E : T> insert(entities: Iterable<E>, keyClass: KClass<K>): R

    /**
     * Update the given entity. If the given entity has modified properties those changes will be
     * persisted otherwise the method will do nothing. A property is considered modified
     * if its associated setter has been called, modifying the state of a property's content
     * will not cause an update to happen.
     */
    infix fun <E : T> update(entity: E): R

    /**
     * Updates a collection of entities. This method may perform additional optimizations not
     * present in the single element update method.
     */
    infix fun <E : T> update(entities: Iterable<E>): R

    /**
     * Upserts (insert or update) the given entity. Note that upserting may be an expensive
     * operation on some platforms and may not be supported in all cases or platforms.
     */
    infix fun <E : T> upsert(entity: E): R

    /**
     * Upserts (inserts or updates) a collection of entities. This method may perform additional
     * optimizations not present in the single upsert method.
     */
    infix fun <E : T> upsert(entities: Iterable<E>): R

    /**
     * Refresh the given entity. This refreshes the already loaded properties in the entity. If no
     * properties are loaded then the default properties will be loaded.
     */
    infix fun <E : T> refresh(entity: E): R

    /**
     * Refresh the given entity on specific attributes.
     */
    fun <E : T> refresh(entity: E, vararg attributes: Attribute<*, *>): R

    /**
     * Refresh the given entities on specific attributes.
     */
    fun <E : T> refresh(entities: Iterable<E>, vararg attributes: Attribute<*, *>): R

    /**
     * Refresh the given entity on all of its attributes including relational ones.
     */
    fun <E : T> refreshAll(entity: E): R

    /**
     * Deletes the given entity from the store.
     */
    infix fun <E : T> delete(entity: E): R?

    /**
     * Deletes multiple entities.
     */
    infix fun <E : T> delete(entities: Iterable<E>): R?

    /**
     * Find an entity by the given key. This differs from selecting the key using a query in that
     * the [io.requery.EntityCache] if available may be checked first for the object. If an entity is
     * found in the cache it will be returned and potentially no query will be made.
     */
    fun <E : T, K> findByKey(type: KClass<E>, key: K): R?

    /**
     * @return a {@link BlockingEntityStore} version of this entity store. If the implementation
     * is already blocking may return itself.
     */
    fun toBlocking(): BlockingEntityStore<T>
}

interface BlockingEntityStore<T : Any> : EntityStore<T, Any> {

    override fun <E : T> insert(entity: E): E
    override fun <E : T> insert(entities: Iterable<E>): Iterable<E>
    override fun <K : Any, E : T> insert(entity: E, keyClass: KClass<K>): K
    override fun <K : Any, E : T> insert(entities: Iterable<E>, keyClass: KClass<K>): Iterable<K>
    override fun <E : T> update(entity: E): E
    override fun <E : T> update(entities: Iterable<E>): Iterable<E>
    override fun <E : T> upsert(entity: E): E
    override fun <E : T> upsert(entities: Iterable<E>): Iterable<E>
    override fun <E : T> refresh(entity: E): E
    override fun <E : T> refresh(entity: E, vararg attributes: Attribute<*, *>): E
    override fun <E : T> refresh(entities: Iterable<E>, vararg attributes: Attribute<*, *>): Iterable<E>
    override fun <E : T> refreshAll(entity: E): E
    override fun <E : T> delete(entity: E): Void?
    override fun <E : T> delete(entities: Iterable<E>): Void?
    override fun <E : T, K> findByKey(type: KClass<E>, key: K): E?

    fun <V> withTransaction(body: BlockingEntityStore<T>.() -> V): V
    fun <V> withTransaction(isolation: TransactionIsolation, body: BlockingEntityStore<T>.() -> V): V

    operator fun <V> invoke(block: BlockingEntityStore<T>.() -> V): V = block()
}

inline fun <T : Any, reified E : T> BlockingEntityStore<T>
        .refresh(entity: E, vararg properties: KProperty1<E, *>): E {
    val attributes: MutableSet<QueryableAttribute<E, *>> = LinkedHashSet()
    properties.forEach { property -> attributes.add(findAttribute(property)) }
    return refresh(entity, *attributes.toTypedArray())
}
