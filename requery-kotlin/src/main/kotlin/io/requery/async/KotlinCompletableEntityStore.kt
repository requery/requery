package io.requery.async

import io.requery.TransactionIsolation
import io.requery.kotlin.*
import io.requery.kotlin.Deletion
import io.requery.kotlin.InsertInto
import io.requery.kotlin.Insertion
import io.requery.kotlin.Selection
import io.requery.kotlin.Update
import io.requery.meta.Attribute
import io.requery.query.*
import io.requery.util.function.Function
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor
import java.util.function.Supplier
import kotlin.reflect.KClass

class KotlinCompletableEntityStore<T : Any>(private val store: BlockingEntityStore<T>,
                                            val executor: Executor) : EntityStore<T, Any> {

    override fun close() = store.close()

    override infix fun <E : T> select(type: KClass<E>): Selection<CompletableResult<E>> = result(store.select(type))
    override fun <E : T> select(type: KClass<E>, vararg attributes: QueryableAttribute<E, *>): Selection<CompletableResult<E>> = result(store.select(type, *attributes))
    override fun select(vararg expressions: Expression<*>): Selection<CompletableResult<Tuple>> = result(store.select(*expressions))

    override fun <E : T> insert(type: KClass<E>): Insertion<CompletableResult<Tuple>> = result(store.insert(type))
    override fun <E : T> insert(type: KClass<E>, vararg attributes: QueryableAttribute<E, *>): InsertInto<out Result<Tuple>> = result(store.insert(type, *attributes))
    override fun update(): Update<CompletableScalar<Int>> = scalar(store.update())
    override fun <E : T> update(type: KClass<E>): Update<CompletableScalar<Int>> = scalar(store.update(type))

    override fun delete(): Deletion<CompletableScalar<Int>> = scalar(store.delete())
    override fun <E : T> delete(type: KClass<E>): Deletion<CompletableScalar<Int>> = scalar(store.delete(type))

    override fun <E : T> count(type: KClass<E>): Selection<CompletableScalar<Int>> = scalar(store.count(type))
    override fun count(vararg attributes: QueryableAttribute<T, *>): Selection<CompletableScalar<Int>> = scalar(store.count(*attributes))

    override fun <E : T> insert(entity: E): CompletableFuture<E> = execute { store.insert(entity) }
    override fun <E : T> insert(entities: Iterable<E>): CompletableFuture<Iterable<E>> = execute { store.insert(entities) }
    override fun <K : Any, E : T> insert(entity: E, keyClass: KClass<K>): CompletableFuture<K> = execute { store.insert(entity, keyClass) }
    override fun <K : Any, E : T> insert(entities: Iterable<E>, keyClass: KClass<K>): CompletableFuture<Iterable<K>> = execute { store.insert(entities, keyClass) }

    override fun <E : T> update(entity: E): CompletableFuture<E> = execute { store.update(entity) }
    override fun <E : T> update(entities: Iterable<E>): CompletableFuture<Iterable<E>> = execute { store.update(entities) }

    override fun <E : T> upsert(entity: E): CompletableFuture<E> = execute { store.upsert(entity) }
    override fun <E : T> upsert(entities: Iterable<E>): CompletableFuture<Iterable<E>> = execute { store.upsert(entities) }

    override fun <E : T> refresh(entity: E): CompletableFuture<E> = execute { store.refresh(entity) }
    override fun <E : T> refresh(entity: E, vararg attributes: Attribute<*, *>): CompletableFuture<E> = execute { store.refresh(entity, *attributes) }

    override fun <E : T> refresh(entities: Iterable<E>, vararg attributes: Attribute<*, *>): CompletableFuture<Iterable<E>> = execute { store.refresh(entities, *attributes) }
    override fun <E : T> refreshAll(entity: E): CompletableFuture<E> = execute { store.refreshAll(entity) }

    override fun <E : T> delete(entity: E): CompletableFuture<*> = execute { store.delete(entity) }
    override fun <E : T> delete(entities: Iterable<E>): CompletableFuture<*> = execute { store.delete(entities) }

    override fun raw(query: String, vararg parameters: Any): Result<Tuple> = store.raw(query, parameters)
    override fun <E : T> raw(type: KClass<E>, query: String, vararg parameters: Any): Result<E> = store.raw(type, query, parameters)

    override fun <E : T, K> findByKey(type: KClass<E>, key: K): CompletableFuture<E?> = execute { store.findByKey(type, key) }

    override fun toBlocking(): BlockingEntityStore<T> = store

    fun <V> withTransaction(body: BlockingEntityStore<T>.() -> V): CompletableFuture<V> = execute { store.withTransaction(body) }
    fun <V> withTransaction(isolation: TransactionIsolation, body: BlockingEntityStore<T>.() -> V): CompletableFuture<V> = execute { store.withTransaction(isolation, body) }
    
    inline fun <V> execute(crossinline block: KotlinCompletableEntityStore<T>.() -> V): CompletableFuture<V> {
        return CompletableFuture.supplyAsync(Supplier { block() }, executor)
    }

    @Suppress("UNCHECKED_CAST")
    private fun <E> result(query: Return<out Result<E>>): QueryDelegate<CompletableResult<E>> {
        val element = query as QueryDelegate<Result<E>>
        return element.extend(Function { result -> CompletableResult(result, executor) })
    }

    @Suppress("UNCHECKED_CAST")
    private fun <E> scalar(query: Return<out Scalar<E>>): QueryDelegate<CompletableScalar<E>> {
        val element = query as QueryDelegate<Scalar<E>>
        return element.extend(Function { result -> CompletableScalar(result, executor) })
    }
}
