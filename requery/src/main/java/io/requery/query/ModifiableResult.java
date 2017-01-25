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

import io.requery.proxy.CollectionChanges;
import io.requery.util.CloseableIterator;
import io.requery.util.CollectionObserver;
import io.requery.util.CompositeIterator;
import io.requery.util.FilteringIterator;
import io.requery.util.ObservableCollection;
import io.requery.util.function.Consumer;
import io.requery.util.function.Predicate;
import io.requery.util.function.Supplier;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class ModifiableResult<E> implements MutableResult<E>, ObservableCollection<E> {

    private final Result<E> result;
    private final CollectionChanges<?, E> changes;

    public ModifiableResult(Result<E> result, CollectionChanges<?, E> changes) {
        this.result = result;
        this.changes = changes;
    }

    @Override
    public CollectionObserver<E> observer() {
        return changes;
    }

    @Override
    public void add(E element) {
        changes.elementAdded(element);
    }

    @Override
    public void remove(E element) {
        changes.elementRemoved(element);
    }

    @Override
    public CloseableIterator<E> iterator() {
        // iterator that combines query results with not added elements and filtering out
        // removed elements
        final Iterator<E> queried = result == null ?
                Collections.<E>emptyIterator() : result.iterator();
        final Iterator<E> added = changes.addedElements().iterator();
        final Iterator<E> composite = new CompositeIterator<>(queried, added);
        final Iterator<E> filtered = new FilteringIterator<>(composite, new Predicate<E>() {
            @Override
            public boolean test(E e) {
                return !changes.removedElements().contains(e);
            }
        });
        return new CloseableIterator<E>() {
            @Override
            public void close() {
                if (queried instanceof CloseableIterator) {
                    CloseableIterator closeable = (CloseableIterator) queried;
                    closeable.close();
                }
            }

            @Override
            public boolean hasNext() {
                return filtered.hasNext();
            }

            @Override
            public E next() {
                return filtered.next();
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    @Override
    public CloseableIterator<E> iterator(int skip, int take) {
        return iterator();
    }

    @Override
    public void close() {
        if (result != null) {
            result.close();
        }
    }

    @Override
    public List<E> toList() {
        return result == null ? Collections.<E>emptyList() : result.toList();
    }

    @Override
    public <C extends Collection<E>> C collect(C collection) {
        if (result != null) {
            return result.collect(collection);
        }
        return collection;
    }

    @Override
    public E first() throws NoSuchElementException {
        if (result == null) {
            throw new NoSuchElementException();
        }
        return result.first();
    }

    @Override
    public E firstOr(E defaultElement) {
        return result == null ? defaultElement : result.firstOr(defaultElement);
    }

    @Override
    public E firstOrNull() {
        return firstOr((E)null);
    }

    @Override
    public E firstOr(Supplier<E> supplier) {
        if (result != null) {
            return result.firstOr(supplier);
        }
        return supplier.get();
    }

    @Override
    public void each(Consumer<? super E> action) {
        if (result != null) {
            result.each(action);
        }
    }

    @Override
    public Stream<E> stream() {
        return result == null ?
            StreamSupport.stream(Spliterators.<E>emptySpliterator(), false) : result.stream();
    }

    @Override
    public <K> Map<K, E> toMap(Expression<K> key) {
        return result == null ? Collections.<K, E>emptyMap() : result.toMap(key);
    }

    @Override
    public <K> Map<K, E> toMap(Expression<K> key, Map<K, E> map) {
        if (result != null) {
            return result.toMap(key, map);
        }
        return map;
    }
}
