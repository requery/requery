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
 * Interface for querying both [io.requery.Entity] objects and selectable attributes of those objects.
 *
 * @param <T> the base class or interface to restrict all entities that are queried to.
 */
interface Queryable<T : Any> {

    /**
     * Initiates a query against a set of expression values return a specific entity type in a
     * [io.requery.query.Result].
     */
    infix fun <E : T> select(type: KClass<E>): Selection<out Result<E>>

    /**
     * Initiates a query against a set of attribute values return a specific entity type in a
     * [io.requery.query.Result].
     */
    fun <E : T> select(type: KClass<E>, vararg attributes: QueryableAttribute<E, *>): Selection<out Result<E>>

    /**
     * Initiates an insert operation for a type. After completing the expression call
     * {@link Return#get()} to perform the operation. If the type has no generated key values the
     * result is a tuple containing a single element with the number of rows affected by the
     * operation. Otherwise if the type has generated keys those keys are returned as the result.
     */
    infix fun <E : T> insert(type: KClass<E>): Insertion<out Result<Tuple>>

    /**
     * Initiates an insert operation for a type. After completing the expression call
     * {@link Return#get()} to perform the operation. If the type has no generated key values the
     * result is a tuple containing a single element with the number of rows affected by the
     * operation. Otherwise if the type has generated keys those keys are returned as the result.
     */
    fun <E : T> insert(type: KClass<E>, vararg attributes: QueryableAttribute<E, *>): InsertInto<out Result<Tuple>>

    /**
     * Initiates an update query for a type. The result is the number of rows
     * affected by the call. Note that aggregate update queries will not affect existing entity
     * objects in memory.
     */
    infix fun <E : T> update(type: KClass<E>): Update<out Scalar<Int>>

    /**
     * Initiates an delete query for a type. Note that aggregate delete queries
     * will not affect existing entity objects in memory or in the [io.requery.EntityCache] associated
     * with the store.
     */
    infix fun <E : T> delete(type: KClass<E>): Deletion<out Scalar<Int>>

    /**
     * Initiates a query to count the number of entities of a given type.
     */
    infix fun <E : T> count(type: KClass<E>): Selection<out Scalar<Int>>

    /**
     * Initiates a query to count a given selection.
     */
    fun count(vararg attributes: QueryableAttribute<T, *>): Selection<out Scalar<Int>>

    /**
     * Initiates a query against a set of expression values.
     */
    fun select(vararg expressions: Expression<*>): Selection<out Result<Tuple>>

    /**
     * Initiates an update query against this data store. The result is the number of rows
     * affected by the call. Note that aggregate update queries will not affect existing entity
     * objects in memory.
     */
    fun update(): Update<out Scalar<Int>>

    /**
     * Initiates a delete query against this data store. The result is the number of rows
     * affected by the call. Note that aggregate update queries will not affect existing entity
     * objects in memory.
     */
    fun delete(): Deletion<out Scalar<Int>>

    /**
     * Executes a raw query against the data store.
     */
    fun raw(query: String, vararg parameters: Any): Result<Tuple>

    /**
     * Executes a raw query against the data store mapping on to a specific entity type.
     */
    fun <E : T> raw(type: KClass<E>, query: String, vararg parameters: Any): Result<E>
}

// property selection support

/**
 * Initiates a query against a set of property attribute values returning a specific entity type in a
 * [io.requery.query.Result].
 */
inline fun <T : Any, reified E : T> Queryable<T>
        .select(type: KClass<E>, vararg properties: KProperty1<E, *>): Selection<out Result<E>> {
    val attributes: MutableSet<QueryableAttribute<E, *>> = LinkedHashSet()
    properties.forEach { property -> attributes.add(findAttribute(property)) }
    return select(type, *attributes.toTypedArray())
}

/**
 * Initiates a query against a set of property attribute values returning values as tuples in a
 * [io.requery.query.Result].
 */
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
