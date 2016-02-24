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

package io.requery.proxy;

import io.requery.meta.Attribute;
import io.requery.util.Objects;
import io.requery.util.CollectionObserver;

import java.util.ArrayList;
import java.util.Collection;

public class CollectionChanges<T, E> implements CollectionObserver<E> {

    private final EntityProxy<T> proxy;
    private final Attribute<T, ?> attribute;
    private final Collection<E> added;
    private final Collection<E> removed;

    public CollectionChanges(EntityProxy<T> proxy, Attribute<T, ?> attribute) {
        this.proxy = proxy;
        this.attribute = attribute;
        added = new ArrayList<>();
        removed = new ArrayList<>();
    }

    public Collection<E> addedElements() {
        return added;
    }

    public Collection<E> removedElements() {
        return removed;
    }

    @Override
    public void elementAdded(E element) {
        Objects.requireNotNull(element);
        if (added.add(element)) {
            proxy.setState(attribute, PropertyState.MODIFIED);
        }
        removed.remove(element);
    }

    @Override
    public void elementRemoved(E element) {
        Objects.requireNotNull(element);
        added.remove(element);
        if (removed.add(element)) {
            proxy.setState(attribute, PropertyState.MODIFIED);
        }
    }

    @Override
    public void clear() {
        added.clear();
        removed.clear();
    }
}
