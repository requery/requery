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

import io.requery.util.function.Consumer;
import io.requery.util.function.Supplier;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class ScalarDelegate<E> implements Scalar<E> {

    private final Scalar<E> delegate;

    public ScalarDelegate(Scalar<E> delegate) {
        this.delegate = delegate;
    }

    @Override
    public E value() {
        return delegate.value();
    }

    @Override
    public E call() throws Exception {
        return delegate.call();
    }

    @Override
    public void consume(Consumer<? super E> action) {
        delegate.consume(action);
    }

    @Override
    public CompletableFuture<E> toCompletableFuture() {
        return delegate.toCompletableFuture();
    }

    @Override
    public CompletableFuture<E> toCompletableFuture(Executor executor) {
        return delegate.toCompletableFuture(executor);
    }

    @Override
    public Supplier<E> toSupplier() {
        return delegate.toSupplier();
    }
}
