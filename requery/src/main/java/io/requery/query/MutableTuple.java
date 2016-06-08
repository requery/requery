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

package io.requery.query;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Implementation of a tuple created from a query.
 *
 * @author Nikhil Purushe
 */
public class MutableTuple implements Tuple, Serializable {

    private static final Map<Class<?>, Class<?>> boxedTypes = new HashMap<>();
    static {
        boxedTypes.put(boolean.class, Boolean.class);
        boxedTypes.put(int.class, Integer.class);
        boxedTypes.put(long.class, Long.class);
        boxedTypes.put(short.class, Short.class);
        boxedTypes.put(float.class, Float.class);
        boxedTypes.put(double.class, Double.class);
        boxedTypes.put(char.class, Character.class);
        boxedTypes.put(byte.class, Byte.class);
    }

    private final Map<String, Object> map;
    private final Object[] values;

    public MutableTuple(int size) {
        if (size <= 0) {
            throw new IllegalStateException();
        }
        map = new HashMap<>(size);
        values = new Object[size];
    }

    private String keyOf(Expression<?> expression) {
        String key = expression.getName();
        if (expression instanceof Aliasable) {
            String alias = ((Aliasable) expression).getAlias();
            if (alias != null) {
                key = alias;
            }
        }
        return key == null ? null : key.toLowerCase(Locale.ROOT);
    }

    public void set(int index, Expression<?> expression, Object value) {
        map.put(keyOf(expression), value);
        values[index] = value;
    }

    @Override
    public <V> V get(Expression<V> key) {
        Object value = map.get(keyOf(key));
        if (value == null) {
            return null;
        }
        Class<V> type = key.getClassType();
        if (type.isPrimitive()) {
            @SuppressWarnings("unchecked")
            V result = (V) boxedTypes.get(type).cast(value);
            return result;
        }
        return type.cast(value);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <V> V get(int index) {
        return (V) values[index];
    }

    @SuppressWarnings("unchecked")
    @Override
    public <V> V get(String key) {
        return (V) map.get(key.toLowerCase(Locale.ROOT));
    }

    @Override
    public int count() {
        return values.length;
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(values);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof MutableTuple) {
            MutableTuple other = (MutableTuple) obj;
            return Arrays.equals(values, other.values);
        }
        return false;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        int index = 0;
        sb.append(" [ ");
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (index > 0) {
                sb.append(", ");
            }
            Object value = entry.getValue();
            sb.append(value == null ? "null" : entry.getValue().toString());
            index++;
        }
        sb.append(" ]");
        return sb.toString();
    }
}
