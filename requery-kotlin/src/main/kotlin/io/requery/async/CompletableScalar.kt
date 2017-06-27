package io.requery.async

import io.requery.query.Scalar
import io.requery.query.ScalarDelegate
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor

class CompletableScalar<E>(delegate: Scalar<E>, private val executor: Executor) : ScalarDelegate<E>(delegate) {

    override fun toCompletableFuture(): CompletableFuture<E> = toCompletableFuture(executor)
}