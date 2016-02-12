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

package io.requery.android;

import android.os.Parcel;
import io.requery.meta.Attribute;
import io.requery.meta.Type;
import io.requery.proxy.EntityProxy;
import io.requery.proxy.Property;
import io.requery.proxy.PropertyState;
import io.requery.util.function.Predicate;

/**
 * Handler for Android's Parcelable objects. Serializes field types that are handled by
 * {@link Parcel#readValue(ClassLoader)} and {@link Parcel#writeValue(Object)}. If using a field
 * that doesn't support those you must implement the parcelable interface in the object.
 *
 * @author Nikhil Purushe
 */
public class EntityParceler<T> {

    private final Type<T> type;
    private final Predicate<Property<T,?>> filter;

    public EntityParceler(Type<T> type) {
        this.type = type;
        this.filter = new Predicate<Property<T, ?>>() {
            @Override
            public boolean test(Property<T, ?> value) {
                Attribute attribute = value.attribute();
                return !attribute.isAssociation();
            }
        };
    }

    public T readFromParcel(Parcel in) {
        T entity = type.factory().get();
        EntityProxy<T> proxy = type.proxyProvider().apply(entity);
        for (Property<T, ?> property : proxy.filterProperties(filter)) {
            Class<?> type = property.attribute().classType();
            Object value;
            if (type.isEnum()) {
                String name = (String) in.readValue(null);
                if (name == null) {
                    value = null;
                } else {
                    @SuppressWarnings("unchecked")
                    Class<? extends Enum> enumClass = (Class<? extends Enum>) type;
                    value = Enum.valueOf(enumClass, name);
                }
            } else {
                value = in.readValue(null);
            }
            PropertyState state = PropertyState.valueOf(in.readValue(null).toString());
            property.setObject(value, state);
        }
        return entity;
    }

    public void writeToParcel(T entity, Parcel out) {
        EntityProxy<T> proxy = type.proxyProvider().apply(entity);
        for (Property<T, ?> property : proxy.filterProperties(filter)) {
            Object value = property.get();
            Class<?> type = property.attribute().classType();
            if (type.isEnum()) {
                if (value != null) {
                    value = value.toString();
                }
            }
            out.writeValue(value);
            PropertyState state = property.state();
            out.writeString(state.toString());
        }
    }
}
