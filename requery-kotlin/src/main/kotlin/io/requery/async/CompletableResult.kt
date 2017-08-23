package io.requery.async

import io.requery.query.Result
import io.requery.query.ResultDelegate
import io.requery.query.element.QueryElement
import io.requery.query.element.QueryWrapper
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor
import java.util.function.Supplier

class CompletableResult<E>(delegate: Result<E>, val executor: Executor) : ResultDelegate<E>(delegate), QueryWrapper<E> {

    inline fun <V> toCompletableFuture(crossinline block: CompletableResult<E>.() -> V): CompletableFuture<V>  {
        return CompletableFuture.supplyAsync(Supplier { block() }, executor)
    }

    override fun unwrapQuery(): QueryElement<E> {
        return (delegate as QueryWrapper<E>).unwrapQuery()
    }
}
