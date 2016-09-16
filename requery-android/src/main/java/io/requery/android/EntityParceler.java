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
import android.os.Parcelable;
import io.requery.meta.Attribute;
import io.requery.meta.Type;
import io.requery.proxy.EntityProxy;
import io.requery.proxy.PropertyState;

/**
 * Handler for Android's Parcelable objects. Serializes field types that are handled by
 * {@link Parcel#readValue(ClassLoader)} and {@link Parcel#writeValue(Object)}. If using a field
 * that doesn't support those you must implement the parcelable interface in the object.
 *
 * @author Nikhil Purushe
 */
public class EntityParceler<T> {

    private final Type<T> type;

    public EntityParceler(Type<T> type) {
        this.type = type;
    }

    public T readFromParcel(Parcel in) {
        T entity = type.getFactory().get();
        EntityProxy<T> proxy = type.getProxyProvider().apply(entity);
        for (Attribute<T, ?> attribute : type.getAttributes()) {
            if (attribute.isAssociation()) {
                continue;
            }
            Class<?> typeClass = attribute.getClassType();
            Object value = null;
            if (typeClass.isEnum()) {
                String name = (String) in.readValue(getClass().getClassLoader());
                if (name == null) {
                    value = null;
                } else {
                    @SuppressWarnings("unchecked")
                    Class<? extends Enum> enumClass = (Class<? extends Enum>) typeClass;
                    value = Enum.valueOf(enumClass, name);
                }
            } else if (typeClass.isArray()) {
                int length = in.readInt();
                if (length >= 0) {
                    try {
                        Parcelable.Creator creator = (Parcelable.Creator<?>)
                                typeClass.getField("CREATOR").get(null);
                        Object[] array = creator.newArray(length);
                        in.readTypedArray(array, creator);
                        value = array;
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            } else {
                value = in.readValue(getClass().getClassLoader());
            }
            PropertyState state = PropertyState.LOADED;
            if (!type.isStateless()) {
                state = PropertyState.valueOf(in.readString());
            }
            proxy.setObject(attribute, value, state);
        }
        return entity;
    }

    public void writeToParcel(T entity, Parcel out) {
        EntityProxy<T> proxy = type.getProxyProvider().apply(entity);
        for (Attribute<T, ?> attribute : type.getAttributes()) {
            if (attribute.isAssociation()) {
                continue;
            }
            Object value = proxy.get(attribute, false);
            Class<?> typeClass = attribute.getClassType();
            if (typeClass.isArray()) {
                Parcelable[] array = (Parcelable[]) value;
                if (array == null) {
                    out.writeInt(-1);
                } else {
                    out.writeInt(array.length);
                    out.writeTypedArray(array, 0);
                }
            } else {
                if (typeClass.isEnum()) {
                    if (value != null) {
                        value = value.toString();
                    }
                }
                out.writeValue(value);
            }
            if (!type.isStateless()) {
                PropertyState state = proxy.getState(attribute);
                out.writeString(state.toString());
            }
        }
    }
}
