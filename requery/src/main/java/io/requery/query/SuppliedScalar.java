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

import io.requery.rx.RxSupport;
import io.requery.util.Objects;
import io.requery.util.function.Consumer;
import io.requery.util.function.Supplier;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Single result value that is given by a {@link Supplier}.
 *
 * @author Nikhil Purushe
 *
 * @param <E> type of scalar value
 */
public class SuppliedScalar<E> implements Scalar<E> {

    private final Supplier<E> supplier;
    private final Executor executor;
    private boolean computed;
    private E value;

    public SuppliedScalar(Supplier<E> supplier, Executor executor) {
        this.supplier = Objects.requireNotNull(supplier);
        this.executor = executor; // can be null
    }

    @Override
    public E value() {
        synchronized (supplier) {
            if (!computed) {
                computed = true;
                value = supplier.get();
            }
        }
        return value;
    }

    @Override
    public void consume(Consumer<? super E> action) {
        action.accept(value());
    }

    @Override
    public CompletableFuture<E> toCompletableFuture() {
        return toCompletableFuture(this.executor);
    }

    @Override
    public CompletableFuture<E> toCompletableFuture(Executor executor) {
        final java.util.function.Supplier<E> supplier =
            new java.util.function.Supplier<E>() {
                @Override
                public E get() {
                    return SuppliedScalar.this.value();
                }
            };
        return executor == null ?
            CompletableFuture.supplyAsync(supplier) :
            CompletableFuture.supplyAsync(supplier, executor);
    }

    @Override
    public rx.Single<E> toSingle() {
        return RxSupport.toSingle(this);
    }

    @Override
    public Supplier<E> toSupplier() {
        return new Supplier<E>() {
            @Override
            public E get() {
                return value();
            }
        };
    }
}
