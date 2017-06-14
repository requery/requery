package io.requery.async

import io.requery.kotlin.BlockingEntityStore
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor
import java.util.function.Supplier

class KotlinCompletableEntityStore<T: Any>(private val store: BlockingEntityStore<T>,
                                           val executor: Executor): BlockingEntityStore<T> by store {

    inline fun <R> execute(crossinline block: BlockingEntityStore<T>.() -> R): CompletableFuture<R> {
        return CompletableFuture.supplyAsync(Supplier { block() }, executor)
    }

}
