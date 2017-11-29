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

package io.requery.sql

import io.requery.RollbackException
import io.requery.Transaction
import io.requery.TransactionIsolation
import io.requery.kotlin.*
import io.requery.meta.Attribute
import io.requery.meta.EntityModel
import io.requery.query.Expression
import io.requery.query.Result
import io.requery.query.Scalar
import io.requery.query.Tuple
import io.requery.query.element.QueryType
import io.requery.query.function.Count
import java.util.Arrays
import java.util.LinkedHashSet
import kotlin.reflect.KClass

/**
 * Concrete implementation of [BlockingEntityStore] connecting to SQL database.
 *
 * @author Nikhil Purushe
 */
class KotlinEntityDataStore<T : Any>(configuration: Configuration) : BlockingEntityStore<T> {

    val data: EntityDataStore<T> = EntityDataStore(configuration)

    private val context : EntityContext<T> = data.context()
    private val model : EntityModel = configuration.model

    override fun close() = data.close()

    override fun delete(): Deletion<Scalar<Int>> =
            QueryDelegate(QueryType.DELETE, model, UpdateOperation(context))

    override infix fun <E : T> select(type: KClass<E>): Selection<Result<E>> {
        val reader = context.read<E>(type.java)
        val selection: Set<Expression<*>> = reader.defaultSelection()
        val resultReader = reader.newResultReader(reader.defaultSelectionAttributes())
        val query = QueryDelegate(QueryType.SELECT, model, SelectOperation(context, resultReader))
        query.select(*selection.toTypedArray()).from(type)
        return query
    }

    override fun <E : T> select(type: KClass<E>, vararg attributes: QueryableAttribute<E, *>): Selection<Result<E>> {
        if (attributes.isEmpty()) {
            throw IllegalArgumentException()
        }
        val reader = context.read<E>(type.java)
        val selection: Set<Expression<*>> = LinkedHashSet(Arrays.asList<Expression<*>>(*attributes))
        val resultReader = reader.newResultReader(attributes)
        val query = QueryDelegate(QueryType.SELECT, model, SelectOperation(context, resultReader))
        query.select(*selection.toTypedArray()).from(type.java)
        return query
    }

    override fun select(vararg expressions: Expression<*>): Selection<Result<Tuple>> {
        val reader = TupleResultReader(context)
        val select = SelectOperation(context, reader)
        return QueryDelegate(QueryType.SELECT, model, select).select(*expressions)
    }

    override fun <E : T> insert(type: KClass<E>): Insertion<Result<Tuple>> {
        val selection = data.generatedExpressions(type.java)
        val operation = InsertReturningOperation(context, selection)
        val query = QueryDelegate<Result<Tuple>>(QueryType.INSERT, model, operation)
        query.from(type)
        return query
    }

    override fun <E : T> insert(type: KClass<E>, vararg attributes: QueryableAttribute<E, *>): InsertInto<out Result<Tuple>> {
        val selection = data.generatedExpressions(type.java)
        val operation = InsertReturningOperation(context, selection)
        val query = QueryDelegate<Result<Tuple>>(QueryType.INSERT, model, operation)
        return query.insertColumns(attributes)
    }

    override fun <E : T> update(type: KClass<E>): Update<Scalar<Int>> =
            QueryDelegate(QueryType.UPDATE, model, UpdateOperation(context))

    override fun <E : T> delete(type: KClass<E>): Deletion<Scalar<Int>> {
        val query = QueryDelegate(QueryType.DELETE, model, UpdateOperation(context))
        query.from(type)
        return query
    }

    override fun <E : T> count(type: KClass<E>): Selection<Scalar<Int>> {
        val operation = SelectCountOperation(context)
        val query = QueryDelegate<Scalar<Int>>(QueryType.SELECT, model, operation)
        query.select(Count.count(type.java)).from(type)
        return query
    }

    override fun count(vararg attributes: QueryableAttribute<T, *>): Selection<Scalar<Int>> {
        val operation = SelectCountOperation(context)
        return QueryDelegate<Scalar<Int>>(QueryType.SELECT, model, operation)
                .select(Count.count(*attributes))
    }

    override fun update(): Update<Scalar<Int>> =
            QueryDelegate(QueryType.UPDATE, model, UpdateOperation(context))

    override fun <E : T> insert(entity: E): E = data.insert(entity)
    override fun <E : T> insert(entities: Iterable<E>): Iterable<E> = data.insert(entities)
    override fun <K : Any, E : T> insert(entity: E, keyClass: KClass<K>): K =
            data.insert(entity, keyClass.javaObjectType)
    override fun <K : Any, E : T> insert(entities: Iterable<E>, keyClass: KClass<K>): Iterable<K> =
            data.insert(entities, keyClass.javaObjectType)

    override fun <E : T> update(entity: E): E = data.update(entity)
    override fun <E : T> update(entities: Iterable<E>): Iterable<E> = data.update(entities)

    override fun <E : T> upsert(entity: E): E = data.upsert(entity)
    override fun <E : T> upsert(entities: Iterable<E>): Iterable<E> = data.upsert(entities)

    override fun <E : T> refresh(entity: E): E = data.refresh(entity)
    override fun <E : T> refresh(entity: E, vararg attributes: Attribute<*, *>): E =
            data.refresh(entity, *attributes)

    override fun <E : T> refresh(entities: Iterable<E>, vararg attributes: Attribute<*, *>): Iterable<E> =
            data.refresh(entities, *attributes)
    override fun <E : T> refreshAll(entity: E): E = data.refreshAll(entity)

    override fun <E : T> delete(entity: E): Void? = data.delete(entity)
    override fun <E : T> delete(entities: Iterable<E>): Void? = data.delete(entities)

    override fun <E : T, K> findByKey(type: KClass<E>, key: K): E? = data.findByKey(type.java, key)

    override fun raw(query: String, vararg parameters: Any): Result<Tuple> = data.raw(query, *parameters)
    override fun <E : T> raw(type: KClass<E>, query: String, vararg parameters: Any): Result<E> =
            data.raw(type.java, query, *parameters)

    override fun <V> withTransaction(body: BlockingEntityStore<T>.() -> V): V {
        transaction.begin()
        try {
            val result = body()
            transaction.commit()
            return result
        } catch (e : Exception) {
            transaction.rollback()
            throw RollbackException(e)
        }
    }

    override fun <V> withTransaction(isolation: TransactionIsolation,
                                     body: BlockingEntityStore<T>.() -> V): V {
        transaction.begin(isolation)
        try {
            val result = body()
            transaction.commit()
            return result
        } catch (e : Exception) {
            transaction.rollback()
            throw RollbackException(e)
        }
    }

    override val transaction: Transaction
        get() = data.transaction()

    override fun toBlocking(): BlockingEntityStore<T> = this
}
