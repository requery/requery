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

package io.requery.query;

import io.requery.rx.ToSingle;
import io.requery.util.function.Consumer;
import io.requery.util.function.Supplier;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Represents a single result from a query operation.
 *
 * @param <E> type of element.
 */
public interface Scalar<E> extends ToSingle<E> {

    /**
     * @return the scalar value, causing the query to be executed.
     */
    E value();

    /**
     * Consume the result with the given {@link Consumer}.
     *
     * @param action to receive the elements in this result.
     */
    void consume(Consumer<? super E> action);

    /**
     * @return {@link CompletableFuture} computing the result.
     */
    CompletableFuture<E> toCompletableFuture();

    /**
     * @param executor to perform the operation
     *
     * @return {@link CompletableFuture} computing the result.
     */
    CompletableFuture<E> toCompletableFuture(Executor executor);

    /**
     * Converts this Scalar computation to a single {@link rx.Single}.
     *
     * @return {@link rx.Single} for the result of this query.
     */
    @Override
    rx.Single<E> toSingle();

    /**
     * @return {@link Supplier} for the result of this query.
     */
    Supplier<E> toSupplier();
}
