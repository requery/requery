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

package io.requery.cache;

import io.requery.meta.Attribute;
import io.requery.meta.Type;
import io.requery.proxy.EntityProxy;
import io.requery.proxy.Property;
import io.requery.proxy.PropertyState;
import io.requery.util.function.Predicate;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

class SerializedEntity<E> implements Serializable {

    private final Class<E> entityClass;
    private transient E entity;
    private transient Predicate<Property<E, ?>> filter;

    public SerializedEntity(Class<E> entityClass, E entity) {
        this.entityClass = entityClass;
        this.entity = entity;
    }

    private Predicate<Property<E, ?>> getPropertyFilter() {
        if (filter == null) {
            this.filter = new Predicate<Property<E, ?>>() {
                @Override
                public boolean test(Property<E, ?> value) {
                    // currently only non-associative properties are serialized
                    Attribute attribute = value.attribute();
                    return !attribute.isAssociation();
                }
            };
        }
        return filter;
    }

    private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
        stream.defaultReadObject();
        Type<E> type = SerializationContext.getType(entityClass);
        entity = type.factory().get();
        EntityProxy<E> proxy = type.proxyProvider().apply(entity);
        for (Property<E, ?> property : proxy.filterProperties(getPropertyFilter())) {
            Object value = stream.readObject();
            property.setObject(value, PropertyState.LOADED);
        }
    }

    private void writeObject(ObjectOutputStream stream) throws IOException {
        stream.defaultWriteObject();
        Type<E> type = SerializationContext.getType(entityClass);
        EntityProxy<E> proxy = type.proxyProvider().apply(entity);
        for (Property<E, ?> property : proxy.filterProperties(getPropertyFilter())) {
            Object value = property.get();
            stream.writeObject(value);
        }
    }

    public E getEntity() {
        return entity;
    }
}
