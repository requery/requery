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

import io.requery.meta.Attribute;
import io.requery.meta.Type;
import io.requery.proxy.EntityProxy;
import io.requery.util.CloseableIterable;
import io.requery.util.CloseableIterator;
import io.requery.util.function.Consumer;
import io.requery.util.function.Supplier;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Provides a base {@link Result} implementation.
 *
 * @param <E> entity type being selected.
 *
 * @author Nikhil Purushe
 */
public abstract class BaseResult<E> implements Result<E>, CloseableIterable<E> {

    private final Integer maxSize;
    private final Queue<CloseableIterator<E>> iterators;
    private final AtomicBoolean closed;

    protected BaseResult() {
        this(null);
    }

    protected BaseResult(Integer maxSize) {
        this.maxSize = maxSize;
        this.iterators = new ConcurrentLinkedQueue<>();
        this.closed = new AtomicBoolean();
    }

    public abstract CloseableIterator<E> iterator(int skip, int take);

    @Override
    public List<E> toList() {
        List<E> list = maxSize == null ? new ArrayList<E>() : new ArrayList<E>(maxSize);
        collect(list);
        return Collections.unmodifiableList(list);
    }

    @Override
    public <C extends Collection<E>> C collect(C collection) {
        try (CloseableIterator<E> iterator = iterator()) {
            while (iterator.hasNext()) {
                collection.add(iterator.next());
            }
        }
        return collection;
    }

    @Override
    public E first() {
        try (CloseableIterator<E> iterator = iterator()) {
            return iterator.next();
        }
    }

    @Override
    public E firstOr(E defaultElement) {
        try (CloseableIterator<E> iterator = iterator()) {
            if (iterator.hasNext()) {
                return iterator.next();
            }
        }
        return defaultElement;
    }

    @Override
    public E firstOr(Supplier<E> supplier) {
        try (CloseableIterator<E> iterator = iterator()) {
            if (iterator.hasNext()) {
                return iterator.next();
            }
        }
        return supplier.get();
    }

    @Override
    public E firstOrNull() {
        return firstOr((E)null);
    }

    @Override
    public CloseableIterator<E> iterator() {
        // check closed
        if (closed.get()) {
            throw new IllegalStateException();
        }
        CloseableIterator<E> iterator = iterator(0, Integer.MAX_VALUE);
        iterators.add(iterator);
        return iterator;
    }

    @Override
    public Stream<E> stream() {
        final CloseableIterator<E> iterator = iterator();
        Spliterator<E> spliterator = maxSize == null ?
            Spliterators.spliteratorUnknownSize(iterator, 0) :
            Spliterators.spliterator(iterator, maxSize, 0);
        return StreamSupport.stream(spliterator, false).onClose(new Runnable() {
            @Override
            public void run() {
                iterator.close();
            }
        });
    }

    @Override
    public void each(Consumer<? super E> action) {
        try (CloseableIterator<E> iterator = iterator()) {
            while (iterator.hasNext()) {
                action.accept(iterator.next());
            }
        }
    }

    @Override
    public <K> Map<K, E> toMap(Expression<K> key) {
        return toMap(key, new HashMap<K, E>());
    }

    @Override
    @SuppressWarnings("unchecked")
    public <K> Map<K, E> toMap(Expression<K> key, Map<K, E> map) {
        try (CloseableIterator<E> iterator = iterator()) {
            while (iterator.hasNext()) {
                E value = iterator.next();
                Type<E> type = null;
                if (key instanceof Attribute) {
                    Attribute attribute = (Attribute) key;
                    type = (Type<E>) attribute.getDeclaringType();
                }
                if (type != null) {
                    EntityProxy<E> proxy = type.getProxyProvider().apply(value);
                    map.put(proxy.get((Attribute<E, K>) key), value);
                } else if (value instanceof Tuple) {
                    map.put(((Tuple) value).get(key), value);
                } else {
                    throw new UnsupportedOperationException();
                }
            }
        }
        return map;
    }

    @Override
    public void close() {
        if (closed.compareAndSet(false, true)) {
            CloseableIterator<E> iterator = iterators.poll();
            while (iterator != null) {
                iterator.close();
                iterator = iterators.poll();
            }
        }
    }
}
