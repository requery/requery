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

import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.NoSuchElementException;
import java.util.Queue;

/**
 * Combines the results of several iterators into one single iterator.
 *
 * @param <E> type of elements in the iterator.
 *
 * @author Nikhil Purushe
 */
public class CompositeIterator<E> implements Iterator<E> {

    private final Queue<Iterator<E>> queue;
    private Iterator<E> current;

    @SafeVarargs
    public CompositeIterator(Iterator<E>... iterators) {
        queue = new LinkedList<>();
        queue.addAll(Arrays.asList(iterators));
        if (!queue.isEmpty()) {
            current = queue.poll();
        }
    }

    @Override
    public boolean hasNext() {
        if (current == null) {
            return false;
        }
        boolean hasNext = current.hasNext();
        while (!hasNext) {
            if (queue.isEmpty()) {
                return false;
            }
            current = queue.poll();
            hasNext = current.hasNext();
            if (hasNext) {
                return true;
            }
        }
        return true;
    }

    @Override
    public E next() {
        if (current != null) {
            if (current.hasNext()) {
                return current.next();
            } else {
                while (!queue.isEmpty()) {
                    current = queue.poll();
                    if (current.hasNext()) {
                        return current.next();
                    }
                }
            }
        }
        throw new NoSuchElementException();
    }

    @Override
    public void remove() {
        if (current != null) {
            current.remove();
        } else {
            throw new IllegalStateException();
        }
    }
}
