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
import io.requery.query.Expression;
import io.requery.query.Tuple;
import io.requery.util.Objects;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Represents a composite key of multiple values.
 *
 * @param <T> entity type
 *
 * @author Nikhil Purushe
 */
public class CompositeKey<T> implements Serializable, Tuple {

    // attribute not used since this class is serializable, the key is the attribute name
    private final Map<String, Object> map;

    /**
     * Creates a new composite key instance.
     *
     * @param values a map of key {@link Attribute} to their corresponding values.
     */
    public CompositeKey(Map<? extends Attribute<T, ?>, ?> values) {
        if (values.isEmpty()) {
            throw new IllegalArgumentException();
        }
        this.map = new LinkedHashMap<>();
        for (Map.Entry<? extends Attribute<T, ?>, ?> entry : values.entrySet()) {
            map.put(entry.getKey().name(), entry.getValue());
        }
    }

    @Override
    public <V> V get(Expression<V> key) {
        if (key instanceof Attribute) {
            return key.classType().cast(map.get(key.name()));
        }
        return null;
    }

    @Override
    public <V> V get(String key) {
        @SuppressWarnings("unchecked")
        V value = (V) map.get(key);
        return value;
    }

    @Override
    public <V> V get(int index) {
        @SuppressWarnings("unchecked")
        V value = (V) map.values().toArray()[index];
        return value;
    }

    @Override
    public int count() {
        return map.size();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof CompositeKey) {
            CompositeKey other = (CompositeKey) obj;
            return Objects.equals(map, other.map);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(map);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        int index = 0;
        for (Map.Entry<String, ?> entry : map.entrySet()) {
            if (index > 0) {
                sb.append(", ");
            }
            index++;
            sb.append(entry.getKey());
            sb.append(" = ");
            sb.append(entry.getValue());
        }
        sb.append("]");
        return sb.toString();
    }
}
