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

import io.requery.util.CloseableIterator;
import io.requery.util.function.Consumer;
import io.requery.util.function.Supplier;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Stream;

public class ResultDelegate<E> implements Result<E> {

    protected final Result<E> delegate;

    public ResultDelegate(Result<E> delegate) {
        this.delegate = delegate;
    }

    @Override
    public CloseableIterator<E> iterator() {
        return delegate.iterator();
    }

    @Override
    public CloseableIterator<E> iterator(int skip, int take) {
        return delegate.iterator(skip, take);
    }

    @Override
    public void close() {
        delegate.close();
    }

    @Override
    public Stream<E> stream() {
        return delegate.stream();
    }

    @Override
    public <C extends Collection<E>> C collect(C collection) {
        return delegate.collect(collection);
    }

    @Override
    public E first() throws NoSuchElementException {
        return delegate.first();
    }

    @Override
    public E firstOr(E defaultElement) {
        return delegate.firstOr(defaultElement);
    }

    @Override
    public E firstOr(Supplier<E> supplier) {
        return delegate.firstOr(supplier);
    }

    @Override
    public E firstOrNull() {
        return delegate.firstOrNull();
    }

    @Override
    public void each(Consumer<? super E> action) {
        delegate.each(action);
    }

    @Override
    public List<E> toList() {
        return delegate.toList();
    }

    @Override
    public <K> Map<K, E> toMap(Expression<K> key) {
        return delegate.toMap(key);
    }

    @Override
    public <K> Map<K, E> toMap(Expression<K> key, Map<K, E> map) {
        return delegate.toMap(key, map);
    }
}
