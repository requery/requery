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

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

public class ObservableSet<E> implements Set<E>, ObservableCollection<E> {

    private final Set<E> set;
    private final CollectionObserver<E> observer;

    public ObservableSet(Set<E> set, CollectionObserver<E> observer) {
        this.set = Objects.requireNotNull(set);
        this.observer = observer;
    }

    @Override
    public CollectionObserver<E> observer() {
        return observer;
    }

    @Override
    public int size() {
        return set.size();
    }

    @Override
    public boolean isEmpty() {
        return set.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return set.contains(o);
    }

    @Override @Nonnull
    public Iterator<E> iterator() {
        return set.iterator();
    }

    @Override @Nonnull
    public Object[] toArray() {
        return set.toArray();
    }

    @SuppressWarnings("SuspiciousToArrayCall")
    @Override
    @Nonnull
    public <T> T[] toArray(@Nonnull T[] a) {
        return set.toArray(a);
    }

    @Override
    public boolean add(E e) {
        boolean added = set.add(e);
        if (added && observer != null) {
            observer.elementAdded(e);
        }
        return added;
    }

    @Override
    public boolean remove(Object o) {
        boolean removed = set.remove(o);
        if (removed && observer != null) {
            @SuppressWarnings("unchecked")
            E element = (E) o;
            observer.elementRemoved(element);
        }
        return removed;
    }

    @Override
    public boolean containsAll(@Nonnull Collection<?> c) {
        return set.containsAll(c);
    }

    @Override
    public boolean addAll(@Nonnull Collection<? extends E> c) {
        boolean modified = false;
        for (E element : c) {
            boolean added = add(element);
            if (!modified && added) {
                modified = true;
            }
        }
        return modified;
    }

    @Override
    public boolean retainAll(@Nonnull Collection<?> c) {
        ArrayList<E> toRemove = new ArrayList<>();
        for (E element : this) {
            if (!c.contains(element)) {
                toRemove.add(element);
            }
        }
        return removeAll(toRemove);
    }

    @Override
    public boolean removeAll(@Nonnull Collection<?> c) {
        boolean modified = false;
        for (Object element : c) {
            boolean removed = remove(element);
            if (!modified && removed) {
                modified = true;
            }
        }
        return modified;
    }

    @Override
    public void clear() {
        if (observer != null) {
            observer.clear();
            for (E element : this) {
                observer.elementRemoved(element);
            }
        }
        set.clear();
    }
}
