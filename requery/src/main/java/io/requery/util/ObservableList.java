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
import java.util.List;
import java.util.ListIterator;

public class ObservableList<E> implements List<E>, ObservableCollection<E> {

    private final List<E> list;
    private final CollectionObserver<E> observer;

    public ObservableList(List<E> list, CollectionObserver<E> observer) {
        this.list = list;
        this.observer = observer;
    }

    @Override
    public CollectionObserver<E> observer() {
        return observer;
    }

    @Override
    public int size() {
        return list.size();
    }

    @Override
    public boolean isEmpty() {
        return list.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return list.contains(o);
    }

    @Override
    @Nonnull
    public Iterator<E> iterator() {
        return new ObservableCollectionIterator<>(list, observer);
    }

    @Override
    @Nonnull
    public Object[] toArray() {
        return list.toArray();
    }

    @SuppressWarnings("SuspiciousToArrayCall")
    @Override
    @Nonnull
    public <T> T[] toArray(@Nonnull T[] a) {
        return list.toArray(a);
    }

    @Override
    public boolean add(E e) {
        boolean added = list.add(e);
        if (added && observer != null) {
            observer.elementAdded(e);
        }
        return added;
    }

    @Override
    public boolean remove(Object o) {
        boolean removed = list.remove(o);
        if (removed && observer != null) {
            @SuppressWarnings("unchecked")
            E element = (E) o;
            observer.elementRemoved(element);
        }
        return removed;
    }

    @Override
    public boolean containsAll(@Nonnull Collection<?> c) {
        return list.containsAll(c);
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
    public boolean addAll(int index, @Nonnull Collection<? extends E> c) {
        return list.addAll(c);
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
    public void clear() {
        if (observer != null) {
            for (E element : this) {
                observer.elementRemoved(element);
            }
        }
        list.clear();
    }

    @Override
    public E get(int index) {
        return list.get(index);
    }

    @Override
    public E set(int index, E element) {
        Objects.requireNotNull(element);
        E removed = list.set(index, element);
        if (observer != null) {
            if (removed != null) {
                observer.elementRemoved(element);
            }
            observer.elementAdded(element);
        }
        return removed;
    }

    @Override
    public void add(int index, E element) {
        Objects.requireNotNull(element);
        list.add(index, element);
        if (observer != null) {
            observer.elementAdded(element);
        }
    }

    @Override
    public E remove(int index) {
        E removed = list.remove(index);
        if (removed != null && observer != null) {
            observer.elementRemoved(removed);
        }
        return removed;
    }

    @Override
    public int indexOf(Object o) {
        return list.indexOf(o);
    }

    @Override
    public int lastIndexOf(Object o) {
        return list.lastIndexOf(o);
    }

    @Override
    @Nonnull
    public ListIterator<E> listIterator() {
        return list.listIterator();
    }

    @Override
    @Nonnull
    public ListIterator<E> listIterator(int index) {
        return list.listIterator(index);
    }

    @Override
    @Nonnull
    public List<E> subList(int fromIndex, int toIndex) {
        return list.subList(fromIndex, toIndex);
    }
}
