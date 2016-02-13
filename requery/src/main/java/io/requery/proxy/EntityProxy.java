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
import io.requery.util.FilteringIterator;
import io.requery.util.Objects;
import io.requery.util.function.Predicate;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Proxy object for a data entity containing various properties that can be read or written and the
 * {@link Attribute} meta data associated with them.
 *
 * @param <E> entity type
 * @author Nikhil Purushe
 */
public class EntityProxy<E> implements Iterable<Property<E, ?>>, EntityStateListener {

    private final Type<E> type;
    private final E entity;
    private final Map<Attribute<E, ?>, Property<E, ?>> properties;
    private final int keyCount;
    private Attribute<E, ?> keyAttribute;
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
        this.entity = Objects.requireNotNull(entity);
        this.type = Objects.requireNotNull(type);
        this.properties = new LinkedHashMap<>();
        this.keyCount = type.keyAttributes().size();

        for (Attribute<E, ?> attribute : type.attributes()) {
            Property<E, ?> property = new Property<>(entity, attribute);
            properties.put(attribute, property);
            // if there is only 1 key then keyAttribute will be used for the optimal case
            if (attribute.isKey() && keyCount == 1) {
                keyAttribute = attribute;
            }
        }
    }

    /**
     * get the current value for an attribute through this proxy.
     *
     * @param attribute to get
     * @param <V>       type of the attribute
     * @return the current value of that attribute using {@link PropertyLoader} instance to
     * retrieve it if required.
     */
    public <V> V get(Attribute<E, V> attribute) {
        return get(attribute, true);
    }

    /**
     * get the current value for an attribute through this proxy.
     *
     * @param attribute to get
     * @param fetch     true to fetch the value through the {@link PropertyLoader} if set
     * @param <V>       type fo the attribute
     * @return the current value of that attribute using {@link PropertyLoader} instance to
     * retrieve it if fetch = true.
     */
    public <V> V get(Attribute<E, V> attribute, boolean fetch) {
        Property<E, V> property = propertyOf(attribute);
        if (fetch && property.state() == PropertyState.FETCH && isLinked()) {
            // lazy loaded, get from store then set the value
            loader.load(entity, this, property);
        }
        V value = attribute.classType().cast(property.get());
        if (value == null && property.state() == PropertyState.FETCH &&
            attribute.fieldAccess().initializer() != null) {

            value = attribute.fieldAccess().initializer().initialize(property);
            set(attribute, value, PropertyState.FETCH);
        }
        return value;
    }

    /**
     * Set a value through this proxy and change it's corresponding {@link PropertyState} to
     * {@link PropertyState#MODIFIED}.
     *
     * @param attribute attribute to change
     * @param value     new property value
     * @param <V>       type of the value
     */
    public <V> void set(Attribute<E, V> attribute, V value) {
        set(attribute, value, PropertyState.MODIFIED);
    }

    /**
     * Set a value through this proxy and change it's corresponding {@link PropertyState}.
     *
     * @param attribute attribute to change
     * @param value     new property value
     * @param state     new property state
     * @param <V>       type of the value
     */
    public <V> void set(Attribute<E, V> attribute, V value, PropertyState state) {
        Property<E, V> property = propertyOf(attribute);
        property.set(value, state);
        if (attribute == keyAttribute) {
            regenerateKey = true;
        }
    }

    /**
     * @return the key value for this proxy instance which is used to uniquely identify it.
     */
    public Object key() {
        if (regenerateKey || key == null) {
            if (keyAttribute != null) {
                key = get(keyAttribute); // typical case one key attribute
            } else if (keyCount > 1) {
                LinkedHashMap<Attribute<E, ?>, Object> keys = new LinkedHashMap<>(keyCount);
                for (Property<E, ?> property : this) {
                    if (property.attribute().isKey()) {
                        keys.put(property.attribute(), property.get());
                    }
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
    public Iterator<Property<E, ?>> iterator() {
        return properties.values().iterator();
    }

    public Iterable<Property<E, ?>> filterProperties(final Predicate<Property<E, ?>> filter) {
        return new Iterable<Property<E, ?>>() {
            @Override
            public Iterator<Property<E, ?>> iterator() {
                Iterator<Property<E, ?>> iterator = properties.values().iterator();
                return new FilteringIterator<>(iterator, filter);
            }
        };
    }

    public <V> Property<E, V> propertyOf(Attribute<E, V> attribute) {
        @SuppressWarnings("unchecked")
        Property<E, V> property = (Property<E, V>) properties.get(attribute);
        return Objects.requireNotNull(property);
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
                for (Map.Entry<Attribute<E, ?>, Property<E, ?>> entry : properties.entrySet()) {
                    Attribute<E, ?> attribute = entry.getKey();
                    // comparing only the non-associative properties for now
                    if (!attribute.isAssociation()) {
                        Property p = entry.getValue();
                        if (!p.equals(other.properties.get(attribute))) {
                            return false;
                        }
                    }
                    // compare foreign key
                    if (attribute.isForeignKey()) {
                        Property p = entry.getValue();
                        if (!p.equals(other.properties.get(attribute))) {
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
        for (Property<E, ?> property : this) {
            Attribute<?, ?> attribute = property.attribute();
            if (!attribute.isAssociation()) {
                hash = 31 * hash + property.hashCode();
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
        for (Map.Entry<Attribute<E, ?>, Property<E, ?>> entry : properties.entrySet()) {
            if (index > 0) {
                sb.append(", ");
            }
            sb.append(entry.getValue().toString());
            index++;
        }
        sb.append("]");
        return sb.toString();
    }
}
