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

import io.requery.meta.Attribute
import io.requery.query.*
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

/**
 * Interface for querying both [Entity] objects and selectable attributes of those objects.
 *
 * @param <T> the base class or interface to restrict all entities that are queried to.
 */
interface Queryable<T : Any> {

    infix fun <E : T> select(type: KClass<E>): Selection<out Result<E>>
    fun <E : T> select(type: KClass<E>, vararg attributes: QueryableAttribute<E, *>): Selection<out Result<E>>
    infix fun <E : T> insert(type: KClass<E>): Insertion<out Result<Tuple>>
    fun <E : T> insert(type: KClass<E>, vararg attributes: QueryableAttribute<E, *>): InsertInto<out Result<Tuple>>
    infix fun <E : T> update(type: KClass<E>): Update<out Scalar<Int>>
    infix fun <E : T> delete(type: KClass<E>): Deletion<out Scalar<Int>>
    infix fun <E : T> count(type: KClass<E>): Selection<out Scalar<Int>>
    fun count(vararg attributes: QueryableAttribute<T, *>): Selection<out Scalar<Int>>
    fun select(vararg expressions: Expression<*>): Selection<out Result<Tuple>>
    fun update(): Update<out Scalar<Int>>
    fun delete(): Deletion<out Scalar<Int>>
    fun raw(query: String, vararg parameters: Any): Result<Tuple>
    fun <E : T> raw(type: KClass<E>, query: String, vararg parameters: Any): Result<E>
}

// property selection support
inline fun <T : Any, reified E : T> Queryable<T>
        .select(type: KClass<E>, vararg properties: KProperty1<E, *>): Selection<out Result<E>> {
    val attributes: MutableSet<QueryableAttribute<E, *>> = LinkedHashSet()
    properties.forEach { property -> attributes.add(findAttribute(property)) }
    return select(type, *attributes.toTypedArray())
}

inline fun <T : Any, reified E : T> Queryable<T>
        .select(vararg properties: KProperty1<E, *>): Selection<out Result<Tuple>> {
    val attributes: MutableSet<Expression<*>> = LinkedHashSet()
    properties.forEach { property -> attributes.add(findAttribute(property)) }
    return select(*attributes.toTypedArray())
}

inline operator fun <T : Any, reified E : T> Queryable<T>
        .get(vararg properties: KProperty1<E, *>): Selection<out Result<E>> {
    val attributes: MutableSet<QueryableAttribute<E, *>> = LinkedHashSet()
    properties.forEach { property -> attributes.add(findAttribute(property)) }
    return select(E::class, *attributes.toTypedArray())
}

inline fun <T : Any, reified E : T> Queryable<T>
        .insert(type: KClass<E>, vararg properties: KProperty1<E, *>): InsertInto<out Result<Tuple>> {
    val attributes: MutableSet<QueryableAttribute<E, *>> = LinkedHashSet()
    properties.forEach { property -> attributes.add(findAttribute(property)) }
    return insert(type, *attributes.toTypedArray())
}

interface QueryableAttribute<T, V> : Attribute<T, V>,
        Expression<V>,
        Functional<V>,
        Aliasable<Expression<V>>,
        Conditional<Logical<out Expression<V>, *>, V>
