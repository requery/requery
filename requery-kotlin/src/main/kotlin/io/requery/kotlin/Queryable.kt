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

    infix fun <E : T> select(type: KClass<E>): Selection<Result<E>>
    fun <E : T> select(vararg attributes: QueryableAttribute<E, *>): Selection<Result<E>>
    infix fun <E : T> insert(type: KClass<E>): Insertion<Result<Tuple>>
    infix fun <E : T> update(type: KClass<E>): Update<Scalar<Int>>
    infix fun <E : T> delete(type: KClass<E>): Deletion<Scalar<Int>>
    infix fun <E : T> count(type: KClass<E>): Selection<Scalar<Int>>
    fun count(vararg attributes: QueryableAttribute<T, *>): Selection<Scalar<Int>>
    fun select(vararg expressions: Expression<*>): Selection<Result<Tuple>>
    fun update(): Update<Scalar<Int>>
    fun delete(): Deletion<Scalar<Int>>
}

// property selection support
inline fun <T : Persistable, reified E : T> Queryable<T>
        .select(vararg properties: KProperty1<E, *>): Selection<Result<E>> {
    val attributes: MutableSet<QueryableAttribute<E, *>> = LinkedHashSet()
    properties.forEach { property -> attributes.add(findAttribute(property)) }
    return select(*attributes.toTypedArray())
}

inline operator fun <T : Persistable, reified E : T> Queryable<T>
        .get(vararg properties: KProperty1<E, *>): Selection<Result<E>> {
    val attributes: MutableSet<QueryableAttribute<E, *>> = LinkedHashSet()
    properties.forEach { property -> attributes.add(findAttribute(property)) }
    return select(*attributes.toTypedArray())
}

// selection only parts of object - returning data in Tuples
inline fun <T : Persistable, reified E : T> Queryable<T>.selectPartial(vararg properties: KProperty1<E, *>): Selection<Result<Tuple>> {
    val expressions: Array<Expression<*>> = Array(properties.size) { findAttribute(properties[it]) }
    return select(*expressions)
}

interface QueryableAttribute<T, V> : Attribute<T, V>,
        Expression<V>,
        Functional<V>,
        Aliasable<Expression<V>>,
        Conditional<Logical<out Expression<V>, *>, V> {
}
