package io.requery.async

import io.requery.TransactionIsolation
import io.requery.kotlin.EntityStore
import io.requery.meta.Attribute
import java.util.concurrent.CompletionStage
import kotlin.reflect.KClass

interface KotlinCompletionStageEntityStore<T : Any> : EntityStore<T, CompletionStage<out Any?>> {

    override fun <E : T> insert(entity: E): CompletionStage<E>
    override fun <E : T> insert(entities: Iterable<E>): CompletionStage<Iterable<E>>
    override fun <K : Any, E : T> insert(entity: E, keyClass: KClass<K>): CompletionStage<K>
    override fun <K : Any, E : T> insert(entities: Iterable<E>, keyClass: KClass<K>): CompletionStage<Iterable<K>>
    override fun <E : T> update(entity: E): CompletionStage<E>
    override fun <E : T> update(entities: Iterable<E>): CompletionStage<Iterable<E>>
    override fun <E : T> upsert(entity: E): CompletionStage<E>
    override fun <E : T> upsert(entities: Iterable<E>): CompletionStage<Iterable<E>>
    override fun <E : T> refresh(entity: E): CompletionStage<E>
    override fun <E : T> refresh(entity: E, vararg attributes: Attribute<*, *>): CompletionStage<E>
    override fun <E : T> refresh(entities: Iterable<E>, vararg attributes: Attribute<*, *>): CompletionStage<Iterable<E>>
    override fun <E : T> refreshAll(entity: E): CompletionStage<E>
    override fun <E : T> delete(entity: E): CompletionStage<Void?>
    override fun <E : T> delete(entities: Iterable<E>): CompletionStage<Void?>
    override fun <E : T, K> findByKey(type: KClass<E>, key: K): CompletionStage<E?>

    fun <V> withTransaction(body: KotlinCompletionStageEntityStore<T>.() -> V): CompletionStage<V>
    fun <V> withTransaction(isolation: TransactionIsolation, body: KotlinCompletionStageEntityStore<T>.() -> V): CompletionStage<V>

    operator fun <V> invoke(block: KotlinCompletionStageEntityStore<T>.() -> V): V = block()
}