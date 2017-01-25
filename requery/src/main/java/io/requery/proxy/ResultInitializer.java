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
import io.requery.query.ModifiableResult;
import io.requery.query.Result;
import io.requery.util.function.Supplier;

public class ResultInitializer<E, V> implements Initializer<E, V>, QueryInitializer<E, V> {

    @Override
    public V initialize(EntityProxy<E> proxy, Attribute<E, V> attribute) {
        return initialize(proxy, attribute, null);
    }

    @Override
    public <U> V initialize(EntityProxy<E> proxy,
                            Attribute<E, V> attribute,
                            Supplier<? extends Result<U>> query) {
        Class<?> type = attribute.getClassType();
        CollectionChanges<E, U> changes = new CollectionChanges<>(proxy, attribute);
        Result<U> result = query == null ? null : query.get();
        Object collection;
        if (Iterable.class.isAssignableFrom(type)) {
            collection = new ModifiableResult<>(result, changes);
        } else {
            throw new IllegalStateException("Unsupported result type " + type);
        }
        return attribute.getClassType().cast(collection);
    }
}
