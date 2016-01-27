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
import io.requery.util.function.Supplier;

/**
 * Encapsulates access to a field member and it's property state {@link PropertyState}.
 *
 * @param <E> entity type
 * @param <V> value type
 *
 * @author Nikhil Purushe
 */
public class Property<E, V> implements Supplier<V> {

    private final E entity;
    private final Attribute<E, V> attribute;
    private final Field<E, V> field;
    private PropertyState state;

    public Property(E entity, Attribute<E, V> attribute) {
        this.entity = entity;
        this.attribute = attribute;
        this.field = attribute.fieldAccess();
        this.state = PropertyState.FETCH;
    }

    /**
     * @return the {@link Attribute} associated with this property instance.
     */
    public Attribute<E, V> attribute() {
        return attribute;
    }

    /**
     * @return the current value of this property instance.
     */
    @Override
    public V get() {
        return field.getter().get(entity);
    }

    /**
     * @return the current state of this property instance.
     */
    public PropertyState state() {
        return state;
    }

    /**
     * Sets the state
     * @param state to set (not null)
     */
    public void setState(PropertyState state) {
        this.state = Objects.requireNotNull(state);
    }

    /**
     * Sets the value from the given object and also sets the state
     * @param value to set
     * @param state to set (not null)
     */
    public void set(V value, PropertyState state) {
        if (value == null && !attribute.isNullable()) {
            throw new IllegalArgumentException(attribute.name() + " cannot be null");
        }
        field.setter().set(entity, value);
        this.state = state;
    }

    /**
     * Sets the value from any object and the state
     * @param value to set
     * @param state to set (not null)
     * @throws ClassCastException if the value is not of the correct type
     */
    public void setObject(Object value, PropertyState state) {
        if (value == null && !attribute.isNullable()) {
            throw new IllegalArgumentException(attribute.name() + " cannot be null");
        }
        field.setter().set(entity, attribute.classType().cast(value));
        this.state = state;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Property) {
            Property other = (Property) obj;
            // must the same type of property
            if (other.attribute().equals(attribute)) {
                Object value = get();
                return Objects.equals(value, other.get());
            }
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(get());
    }

    @Override
    public String toString() {
        return attribute.name() + "(" + state.name().toLowerCase() + ")" + " = " + get();
    }
}
