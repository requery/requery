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

public class ResultInitializer<V> implements Initializer<V>, QueryInitializer<V> {

    @Override
    public V initialize(Property<?, V> property) {
        return initialize(property, null);
    }

    @Override
    public <U> V initialize(Property<?, V> property, Supplier<Result<U>> query) {
        Attribute attribute = property.attribute();
        Class<?> type = attribute.classType();
        CollectionChanges<U> changes = new CollectionChanges<>(property);
        Result<U> result = query == null ? null : query.get();
        Object collection;
        if (Iterable.class.isAssignableFrom(type)) {
            collection = new ModifiableResult<>(result, changes);
        } else {
            throw new IllegalStateException("Unsupported result type " + type);
        }
        V value = property.attribute().classType().cast(collection);
        property.set(value, PropertyState.LOADED);
        return value;
    }
}
