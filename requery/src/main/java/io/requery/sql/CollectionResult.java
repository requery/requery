/*
 * Copyright 2017 requery.io
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

package io.requery.sql;

import io.requery.query.BaseResult;
import io.requery.util.CloseableIterator;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

/**
 * A {@link io.requery.query.Result} that wraps a collection.
 *
 * @param <E> element type
 */
public class CollectionResult<E> extends BaseResult<E> {

    private Collection<E> elements;

    /**
     * Creates an empty result
     */
    public CollectionResult() {
        this(Collections.<E>emptySet());
    }

    /**
     * Creates a result with a single element
     */
    public CollectionResult(E element) {
        this(Collections.singleton(element));
    }

    /**
     * Creates a result from a collection instance.
     */
    public CollectionResult(Collection<E> collection) {
        super(1);
        this.elements = collection;
    }

    @Override
    public CloseableIterator<E> createIterator(int skip, int take) {
        final Iterator<E> iterator = elements.iterator();
        return new CloseableIterator<E>() {
            @Override
            public void close() {
            }

            @Override
            public boolean hasNext() {
                return iterator.hasNext();
            }

            @Override
            public E next() {
                return iterator.next();
            }
        };
    }
}
