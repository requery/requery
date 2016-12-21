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

package io.requery.rx

import io.requery.kotlin.*
import io.requery.kotlin.Deletion
import io.requery.kotlin.Insertion
import io.requery.kotlin.Selection
import io.requery.kotlin.Update
import io.requery.meta.Attribute
import io.requery.query.*
import io.requery.util.function.Function
import rx.Completable
import rx.Single
import kotlin.reflect.KClass

/**
 * Wraps a [BlockingEntityStore] instance returning [rx.Single] and [rx.Completable] for entity
 * operations, and [RxResult] or [RxScalar] for queries.
 */
class KotlinRxEntityStore<T : Any>(private var store: BlockingEntityStore<T>) : EntityStore<T, Any> {

    override fun close() = store.close()

    override infix fun <E : T> select(type: KClass<E>): Selection<RxResult<E>> = result(store.select(type))
    override fun <E : T> select(vararg attributes: QueryableAttribute<E, *>): Selection<RxResult<E>> = result(store.select(*attributes))
    override fun select(vararg expressions: Expression<*>): Selection<RxResult<Tuple>> = result(store.select(*expressions))

    override fun <E : T> insert(type: KClass<E>): Insertion<RxResult<Tuple>> = result(store.insert(type))
    override fun update(): Update<RxScalar<Int>> = scalar(store.update())
    override fun <E : T> update(type: KClass<E>): Update<RxScalar<Int>> = scalar(store.update(type))

    override fun delete(): Deletion<RxScalar<Int>> = scalar(store.delete())
    override fun <E : T> delete(type: KClass<E>): Deletion<RxScalar<Int>> = scalar(store.delete(type))

    override fun <E : T> count(type: KClass<E>): Selection<RxScalar<Int>> = scalar(store.count(type))
    override fun count(vararg attributes: QueryableAttribute<T, *>): Selection<RxScalar<Int>> = scalar(store.count(*attributes))

    override fun <E : T> insert(entity: E): Single<E> = Single.fromCallable { store.insert(entity) }
    override fun <E : T> insert(entities: Iterable<E>): Single<Iterable<E>> = Single.fromCallable { store.insert(entities) }
    override fun <K : Any, E : T> insert(entity: E, keyClass: KClass<K>): Single<K> = Single.fromCallable { store.insert(entity, keyClass) }
    override fun <K : Any, E : T> insert(entities: Iterable<E>, keyClass: KClass<K>): Single<Iterable<K>> = Single.fromCallable { store.insert(entities, keyClass) }

    override fun <E : T> update(entity: E): Single<E> = Single.fromCallable { store.update(entity) }
    override fun <E : T> update(entities: Iterable<E>): Single<Iterable<E>> = Single.fromCallable { store.update(entities) }

    override fun <E : T> upsert(entity: E): Single<E> = Single.fromCallable { store.upsert(entity) }
    override fun <E : T> upsert(entities: Iterable<E>): Single<Iterable<E>> = Single.fromCallable { store.upsert(entities) }

    override fun <E : T> refresh(entity: E): Single<E> = Single.fromCallable { store.refresh(entity) }
    override fun <E : T> refresh(entity: E, vararg attributes: Attribute<*, *>): Single<E> = Single.fromCallable { store.refresh(entity, *attributes) }

    override fun <E : T> refresh(entities: Iterable<E>, vararg attributes: Attribute<*, *>): Single<Iterable<E>> = Single.fromCallable { store.refresh(entities, *attributes) }
    override fun <E : T> refreshAll(entity: E): Single<E> = Single.fromCallable { store.refreshAll(entity) }

    override fun <E : T> delete(entity: E): Completable = Completable.fromCallable { store.delete(entity) }
    override fun <E : T> delete(entities: Iterable<E>): Completable = Completable.fromCallable { store.delete(entities) }

    override fun <E : T, K> findByKey(type: KClass<E>, key: K): Single<E> = Single.fromCallable { store.findByKey(type, key) }

    override fun toBlocking(): BlockingEntityStore<T> = store

    @Suppress("UNCHECKED_CAST")
    private fun <E> result(query: Return<out Result<E>>): QueryDelegate<RxResult<E>> {
        val element = query as QueryDelegate<Result<E>>
        return element.extend(Function { result -> RxResult(result) })
    }

    @Suppress("UNCHECKED_CAST")
    private fun <E> scalar(query: Return<out Scalar<E>>): QueryDelegate<RxScalar<E>> {
        val element = query as QueryDelegate<Scalar<E>>
        return element.extend(Function { result -> RxScalar(result) })
    }
}
