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
import io.requery.meta.Type;
import io.requery.util.Objects;

import java.util.LinkedHashMap;

/**
 * Proxy object for a data entity containing various properties that can be read or written and the
 * {@link Attribute} meta data associated with them.
 *
 * @param <E> entity type
 *
 * @author Nikhil Purushe
 */
public class EntityProxy<E> implements Gettable<E>, Settable<E>, EntityStateListener {

    private final Type<E> type;
    private final E entity;
    private final boolean stateless;
    private PropertyLoader<E> loader;
    private CompositeEntityStateListener<E> listeners;
    private Object key;
    private boolean regenerateKey;

    /**
     * Create a new {@link EntityProxy} instance for a given entity object that can proxy it's
     * getters and setters.
     *
     * @param entity the entity that is being proxied
     * @param type   the entity meta type instance
     */
    public EntityProxy(E entity, Type<E> type) {
        this.entity = entity;
        this.type = type;
        this.stateless = type.isStateless();
    }

    @Override
    public <V> V get(Attribute<E, V> attribute) {
        return get(attribute, true);
    }

    @Override
    public <V> V get(Attribute<E, V> attribute, boolean fetch) {
        PropertyState state = getState(attribute);
        if (fetch && state == PropertyState.FETCH && loader != null) {
            // lazy loaded, get from store then set the value
            loader.load(entity, this, attribute);
        }
        V value = attribute.property().get(entity);
        if (value == null && state == PropertyState.FETCH &&
            attribute.initializer() != null) {

            value = attribute.initializer().initialize(this, attribute);
            set(attribute, value, PropertyState.FETCH);
        }
        return value;
    }

    @Override
    public int getInt(Attribute<E, Integer> attribute) {
        IntProperty<E> property = (IntProperty<E>) attribute.property();
        return property.getInt(entity);
    }

    @Override
    public long getLong(Attribute<E, Long> attribute) {
        LongProperty<E> property = (LongProperty<E>) attribute.property();
        return property.getLong(entity);
    }

    @Override
    public short getShort(Attribute<E, Short> attribute) {
        ShortProperty<E> property = (ShortProperty<E>) attribute.property();
        return property.getShort(entity);
    }

    @Override
    public float getFloat(Attribute<E, Float> attribute) {
        FloatProperty<E> property = (FloatProperty<E>) attribute.property();
        return property.getFloat(entity);
    }

    @Override
    public double getDouble(Attribute<E, Double> attribute) {
        DoubleProperty<E> property = (DoubleProperty<E>) attribute.property();
        return property.getDouble(entity);
    }

    @Override
    public boolean getBoolean(Attribute<E, Boolean> attribute) {
        BooleanProperty<E> property = (BooleanProperty<E>) attribute.property();
        return property.getBoolean(entity);
    }

    @Override
    public <V> void set(Attribute<E, V> attribute, V value) {
        set(attribute, value, PropertyState.MODIFIED);
    }

    @Override
    public <V> void set(Attribute<E, V> attribute, V value, PropertyState state) {
        attribute.property().set(entity, value);
        setState(attribute, state);
        checkRegenerateKey(attribute);
    }

    @Override
    public void setObject(Attribute<E, ?> attribute, Object value, PropertyState state) {
        @SuppressWarnings("unchecked")
        Property<E, Object> property = (Property<E, Object>) attribute.property();
        property.set(entity, value);
        setState(attribute, state);
        checkRegenerateKey(attribute);
    }

    @Override
    public void setInt(Attribute<E, Integer> attribute, int value, PropertyState state) {
        IntProperty<E> property = (IntProperty<E>) attribute.property();
        property.setInt(entity, value);
        setState(attribute, state);
        checkRegenerateKey(attribute);
    }

    @Override
    public void setLong(Attribute<E, Long> attribute, long value, PropertyState state) {
        LongProperty<E> property = (LongProperty<E>) attribute.property();
        property.setLong(entity, value);
        setState(attribute, state);
        checkRegenerateKey(attribute);
    }

    @Override
    public void setShort(Attribute<E, Short> attribute, short value, PropertyState state) {
        ShortProperty<E> property = (ShortProperty<E>) attribute.property();
        property.setShort(entity, value);
        setState(attribute, state);
    }

    @Override
    public void setFloat(Attribute<E, Float> attribute, float value, PropertyState state) {
        FloatProperty<E> property = (FloatProperty<E>) attribute.property();
        property.setFloat(entity, value);
        setState(attribute, state);
    }

    @Override
    public void setDouble(Attribute<E, Double> attribute, double value, PropertyState state) {
        DoubleProperty<E> property = (DoubleProperty<E>) attribute.property();
        property.setDouble(entity, value);
        setState(attribute, state);
    }

    @Override
    public void setBoolean(Attribute<E, Boolean> attribute, boolean value, PropertyState state) {
        BooleanProperty<E> property = (BooleanProperty<E>) attribute.property();
        property.setBoolean(entity, value);
        setState(attribute, state);
    }

    private void checkRegenerateKey(Attribute<E, ?> attribute) {
        if (attribute.isKey()) {
            regenerateKey = true;
        }
    }

    /**
     * Sets the current {@link PropertyState} of a given {@link Attribute}.
     *
     * @param attribute to set
     * @param state     state to set
     */
    public void setState(Attribute<E, ?> attribute, PropertyState state) {
        if (!stateless) {
            attribute.propertyState().set(entity, state);
        }
    }

    /**
     * Gets the current {@link PropertyState} of a given {@link Attribute}.
     *
     * @param attribute to get
     * @return the state of the attribute
     */
    public PropertyState getState(Attribute<E, ?> attribute) {
        if (stateless) {
            return null;
        }
        PropertyState state = attribute.propertyState().get(entity);
        return state == null ? PropertyState.FETCH : state;
    }

    /**
     * @return the key value for this proxy instance which is used to uniquely identify it.
     */
    public Object key() {
        if (regenerateKey || key == null) {
            if (type.singleKeyAttribute() != null) {
                key = get(type.singleKeyAttribute()); // typical case one key attribute
            } else if (type.keyAttributes().size() > 1) {
                LinkedHashMap<Attribute<E, ?>, Object> keys =
                    new LinkedHashMap<>(type.keyAttributes().size());
                for (Attribute<E, ?> attribute : type.keyAttributes()) {
                    keys.put(attribute, get(attribute));
                }
                key = new CompositeKey<>(keys);
            }
        }
        return key;
    }

    /**
     * @return true if linked to a {@link PropertyLoader} instance that will retrieve not loaded
     * property values.
     */
    public boolean isLinked() {
        synchronized (syncObject()) {
            return this.loader != null;
        }
    }

    /**
     * link the proxy to a {@link PropertyLoader} instance that will retrieve not loaded property
     * values.
     *
     * @param loader instance
     */
    public void link(PropertyLoader<E> loader) {
        synchronized (syncObject()) {
            this.loader = loader;
        }
    }

    public void unlink() {
        synchronized (syncObject()) {
            this.loader = null;
        }
    }

    public Type<E> type() {
        return type;
    }

    public Object syncObject() {
        return this;
    }

    // should only be called from the constructor of the entity
    public EntityStateEventListenable<E> modifyListeners() {
        if (listeners == null) {
            listeners = new CompositeEntityStateListener<>(entity);
        }
        return listeners;
    }

    private EntityStateListener stateListener() {
        // if no listeners were ever added return static empty version to avoid overhead of
        // creating listener collection elements
        return listeners == null ? EntityStateListener.EMPTY : listeners;
    }

    @Override
    public void preUpdate() {
        stateListener().preUpdate();
    }

    @Override
    public void postUpdate() {
        stateListener().postUpdate();
    }

    @Override
    public void preInsert() {
        stateListener().preInsert();
    }

    @Override
    public void postInsert() {
        stateListener().postInsert();
    }

    @Override
    public void preDelete() {
        stateListener().preDelete();
    }

    @Override
    public void postDelete() {
        stateListener().postDelete();
    }

    @Override
    public void postLoad() {
        stateListener().postLoad();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof EntityProxy) {
            EntityProxy other = (EntityProxy) obj;
            if (other.entity.getClass().equals(entity.getClass())) {
                for (Attribute<E, ?> attribute : type.attributes()) {
                    // comparing only the non-associative properties for now
                    if (!attribute.isAssociation()) {
                        Object value = get(attribute, false);
                        @SuppressWarnings("unchecked")
                        Object otherValue = other.get(attribute, false);
                        if (!Objects.equals(value, otherValue)) {
                            return false;
                        }
                    }
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public int hashCode() {
        int hash = 31;
        for (Attribute<E, ?> attribute : type.attributes()) {
            if (!attribute.isAssociation()) {
                hash = 31 * hash + Objects.hashCode(get(attribute, false));
            }
        }
        return hash;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(type.name());
        sb.append(" [");
        int index = 0;
        for (Attribute<E, ?> attribute : type.attributes()) {
            if (index > 0) {
                sb.append(", ");
            }
            Object value = get(attribute, false);
            sb.append(value == null ? "null" : value.toString());
            index++;
        }
        sb.append("]");
        return sb.toString();
    }
}
