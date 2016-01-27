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

package io.requery.util;

import io.requery.util.function.Predicate;

import java.util.Iterator;

/**
 * Filters items out of an {@link Iterator} using a {@link Predicate} instance.
 *
 * @param <E> type of elements in the iterator.
 *
 * @author Nikhil Purushe
 */
public class FilteringIterator<E> implements Iterator<E> {

    private final Predicate<? super E> filter;
    private final Iterator<E> iterator;
    private E pending;
    private boolean hasNext;

    public FilteringIterator(Iterator<E> iterator, Predicate<? super E> filter) {
        this.iterator = Objects.requireNotNull(iterator);
        this.filter = Objects.requireNotNull(filter);
    }

    @Override
    public boolean hasNext() {
        if (hasNext) {
            return true;
        }
        while (iterator.hasNext()) {
            E element = iterator.next();
            if (filter.test(element)) {
                pending = element;
                hasNext = true;
                return true;
            }
        }
        return false;
    }

    @Override
    public E next() {
        E element;
        if (hasNext) {
            element = pending;
            pending = null;
            hasNext = false;
            return element;
        } else {
            element = iterator.next();
            if (filter.test(element)) {
                return element;
            } else {
                return next();
            }
        }
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }
}
