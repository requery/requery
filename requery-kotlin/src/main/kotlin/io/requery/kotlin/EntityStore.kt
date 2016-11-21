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

package io.requery.kotlin

import io.requery.Persistable
import io.requery.TransactionIsolation
import io.requery.meta.Attribute
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

interface EntityStore<T : Any, out R> : Queryable<T>, AutoCloseable {

    override fun close()
    infix fun <E : T> insert(entity: E): R
    infix fun <E : T> insert(entities: Iterable<E>): R
    fun <K : Any, E : T> insert(entity: E, keyClass: KClass<K>): R
    fun <K : Any, E : T> insert(entities: Iterable<E>, keyClass: KClass<K>): R
    infix fun <E : T> update(entity: E): R
    infix fun <E : T> update(entities: Iterable<E>): R
    infix fun <E : T> upsert(entity: E): R
    infix fun <E : T> upsert(entities: Iterable<E>): R
    infix fun <E : T> refresh(entity: E): R
    fun <E : T> refresh(entity: E, vararg attributes: Attribute<*, *>): R
    fun <E : T> refresh(entities: Iterable<E>, vararg attributes: Attribute<*, *>): R
    fun <E : T> refreshAll(entity: E): R
    infix fun <E : T> delete(entity: E): R
    infix fun <E : T> delete(entities: Iterable<E>): R
    fun <E : T, K> findByKey(type: KClass<E>, key: K): R?
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
    override fun <E : T> delete(entity: E): Void
    override fun <E : T> delete(entities: Iterable<E>): Void
    override fun <E : T, K> findByKey(type: KClass<E>, key: K): E?

    fun <V> withTransaction(body: BlockingEntityStore<T>.() -> V): V
    fun <V> withTransaction(isolation: TransactionIsolation, body: BlockingEntityStore<T>.() -> V): V

    operator fun <V> invoke(block: BlockingEntityStore<T>.() -> V): V = block()
}

inline fun <T : Persistable, reified E : T> BlockingEntityStore<T>
        .refresh(entity: E, vararg properties: KProperty1<E, *>): E {
    val attributes: MutableSet<QueryableAttribute<E, *>> = LinkedHashSet()
    properties.forEach { property -> attributes.add(findAttribute(property)) }
    return refresh(entity, *attributes.toTypedArray())
}
