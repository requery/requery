package io.requery.sql

import io.requery.RollbackException
import io.requery.TransactionIsolation
import io.requery.async.KotlinCompletionStageEntityStore
import io.requery.kotlin.*
import io.requery.meta.Attribute
import io.requery.query.Expression
import io.requery.query.Result
import io.requery.query.Scalar
import io.requery.query.Tuple
import java.util.concurrent.*
import java.util.function.Supplier
import kotlin.reflect.KClass

class KotlinCompletableEntityStore<T : Any> : KotlinCompletionStageEntityStore<T> {

    val createdExecutor: Boolean
    val delegate: KotlinEntityDataStore<T>
    val executor: Executor

    constructor(delegate: KotlinEntityDataStore<T>) {
        this.delegate = delegate
        this.executor = Executors.newSingleThreadExecutor()
        createdExecutor = true
    }

    constructor(delegate: KotlinEntityDataStore<T>, executor: Executor) {
        this.delegate = delegate
        this.executor = executor
        createdExecutor = false
    }

    override fun close() {
        if (createdExecutor) (executor as ExecutorService).shutdown()
        delegate.close()
    }

    override fun delete(): Deletion<out Scalar<Int>> = delegate.delete()

    override fun <E : T> delete(type: KClass<E>): Deletion<Scalar<Int>> = delegate.delete(type)


    override fun <E : T> select(type: KClass<E>): Selection<out Result<E>> = delegate.select(type)

    override fun select(vararg expressions: Expression<*>): Selection<Result<Tuple>> = delegate.select(*expressions)

    override fun <E : T> insert(type: KClass<E>): Insertion<Result<Tuple>> = delegate.insert(type)

    override fun <E : T> insert(type: KClass<E>, vararg attributes: QueryableAttribute<E, *>): InsertInto<out Result<Tuple>>
            = delegate.insert(type, *attributes)

    override fun update(): Update<Scalar<Int>> = delegate.update()

    override fun <E : T> count(type: KClass<E>): Selection<Scalar<Int>> = delegate.count(type)

    override fun count(vararg attributes: QueryableAttribute<T, *>): Selection<Scalar<Int>> = delegate.count(*attributes)

    override fun <E : T> select(type: KClass<E>, vararg attributes: QueryableAttribute<E, *>): Selection<Result<E>>
            = delegate.select(type, *attributes)

    override fun <E : T> update(type: KClass<E>): Update<Scalar<Int>> = delegate.update(type)

    override fun raw(query: String, vararg parameters: Any): Result<Tuple> = delegate.raw(query, *parameters)

    override fun <E : T> raw(type: KClass<E>, query: String, vararg parameters: Any): Result<E> =
            delegate.raw(type, query, *parameters)

    override fun <E : T> insert(entity: E): CompletionStage<E>
            = CompletableFuture.supplyAsync(Supplier { delegate.insert(entity) }, executor)

    override fun <E : T> insert(entities: Iterable<E>): CompletionStage<Iterable<E>>
            = CompletableFuture.supplyAsync(Supplier { delegate.insert(entities) }, executor)

    override fun <K : Any, E : T> insert(entity: E, keyClass: KClass<K>): CompletionStage<K>
            = CompletableFuture.supplyAsync(Supplier { delegate.insert(entity, keyClass) }, executor)

    override fun <K : Any, E : T> insert(entities: Iterable<E>, keyClass: KClass<K>): CompletionStage<Iterable<K>>
            = CompletableFuture.supplyAsync(Supplier { delegate.insert(entities, keyClass) }, executor)

    override fun <E : T> update(entity: E): CompletionStage<E>
            = CompletableFuture.supplyAsync(Supplier { delegate.update(entity) }, executor)

    override fun <E : T> update(entities: Iterable<E>): CompletionStage<Iterable<E>>
            = CompletableFuture.supplyAsync(Supplier { delegate.update(entities) }, executor)

    override fun <E : T> upsert(entity: E): CompletionStage<E>
            = CompletableFuture.supplyAsync(Supplier { delegate.upsert(entity) }, executor)

    override fun <E : T> upsert(entities: Iterable<E>): CompletionStage<Iterable<E>>
            = CompletableFuture.supplyAsync(Supplier { delegate.upsert(entities) }, executor)

    override fun <E : T> refresh(entity: E): CompletionStage<E>
            = CompletableFuture.supplyAsync(Supplier { delegate.refresh(entity) }, executor)

    override fun <E : T> refresh(entity: E, vararg attributes: Attribute<*, *>): CompletionStage<E>
            = CompletableFuture.supplyAsync(Supplier { delegate.refresh(entity, *attributes) }, executor)

    override fun <E : T> refresh(entities: Iterable<E>, vararg attributes: Attribute<*, *>): CompletionStage<Iterable<E>>
            = CompletableFuture.supplyAsync(Supplier { delegate.refresh(entities, *attributes) }, executor)

    override fun <E : T> refreshAll(entity: E): CompletionStage<E>
            = CompletableFuture.supplyAsync(Supplier { delegate.refreshAll(entity) }, executor)

    override fun <E : T> delete(entity: E): CompletionStage<Void?>
            = CompletableFuture.supplyAsync(Supplier { delegate.delete(entity) }, executor)

    override fun <E : T> delete(entities: Iterable<E>): CompletionStage<Void?>
            = CompletableFuture.supplyAsync(Supplier { delegate.delete(entities) }, executor)

    override fun <E : T, K> findByKey(type: KClass<E>, key: K): CompletionStage<E?>
            = CompletableFuture.supplyAsync(Supplier { delegate.findByKey(type, key) }, executor)

    override fun <V> withTransaction(body: KotlinCompletionStageEntityStore<T>.() -> V): CompletionStage<V>
            = CompletableFuture.supplyAsync(
            Supplier {
                val transaction = delegate.data.transaction().begin()
                try {
                    val result = body()
                    transaction.commit()
                    result
                } catch (e: Exception) {
                    transaction.rollback()
                    throw RollbackException(e)
                }
            }, executor)

    override fun <V> withTransaction(isolation: TransactionIsolation,
                                     body: KotlinCompletionStageEntityStore<T>.() -> V): CompletionStage<V>
            = CompletableFuture.supplyAsync(
            Supplier {
                val transaction = delegate.data.transaction().begin(isolation)
                try {
                    val result = body()
                    transaction.commit()
                    result
                } catch (e: Exception) {
                    transaction.rollback()
                    throw RollbackException(e)
                }
            }, executor)

    override fun toBlocking(): BlockingEntityStore<T> = this.delegate.toBlocking()
}