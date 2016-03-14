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

package io.requery.sql;

import io.requery.meta.Attribute;
import io.requery.proxy.EntityProxy;
import io.requery.proxy.PropertyState;
import io.requery.proxy.Settable;

import java.util.ArrayList;

/**
 * A list that collects all keys set on it, optionally passing through to a source proxy.
 *
 * @param <E> entity type
 */
class GeneratedKeys<E> extends ArrayList<Object> implements Settable<E> {

    private EntityProxy<E> proxy;

    GeneratedKeys() {
    }

    GeneratedKeys(EntityProxy<E> proxy) {
        this.proxy = proxy;
    }

    GeneratedKeys<E> proxy(EntityProxy<E> proxy) {
        this.proxy = proxy;
        return this;
    }

    @Override
    public <V> void set(Attribute<E, V> attribute, V value) {
        if (proxy != null) {
            proxy.set(attribute, value);
        }
        add(value);
    }

    @Override
    public <V> void set(Attribute<E, V> attribute, V value, PropertyState state) {
        if (proxy != null) {
            proxy.set(attribute, value, state);
        }
        add(value);
    }

    @Override
    public void setObject(Attribute<E, ?> attribute, Object value, PropertyState state) {
        if (proxy != null) {
            proxy.setObject(attribute, value, state);
        }
        add(value);
    }

    @Override
    public void setBoolean(Attribute<E, Boolean> attribute, boolean value, PropertyState state) {
        if (proxy != null) {
            proxy.setBoolean(attribute, value, state);
        }
        add(value);
    }

    @Override
    public void setDouble(Attribute<E, Double> attribute, double value, PropertyState state) {
        if (proxy != null) {
            proxy.setDouble(attribute, value, state);
        }
        add(value);
    }

    @Override
    public void setFloat(Attribute<E, Float> attribute, float value, PropertyState state) {
        if (proxy != null) {
            proxy.setFloat(attribute, value, state);
        }
        add(value);
    }

    @Override
    public void setByte(Attribute<E, Byte> attribute, byte value, PropertyState state) {
        if (proxy != null) {
            proxy.setByte(attribute, value, state);
        }
        add(value);
    }

    @Override
    public void setShort(Attribute<E, Short> attribute, short value, PropertyState state) {
        if (proxy != null) {
            proxy.setShort(attribute, value, state);
        }
        add(value);
    }

    @Override
    public void setInt(Attribute<E, Integer> attribute, int value, PropertyState state) {
        if (proxy != null) {
            proxy.setInt(attribute, value, state);
        }
        add(value);
    }

    @Override
    public void setLong(Attribute<E, Long> attribute, long value, PropertyState state) {
        if (proxy != null) {
            proxy.setLong(attribute, value, state);
        }
        add(value);
    }
}
