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

package io.requery.util;

import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

/**
 * A map of class instances to values. Maps classes to values in such a way that if you have a
 * class B that extends A:
 * <pre>
 * <code>
 *      map.put(A.class, "any A");
 *      String s = map.get(B.class);
 * </code>
 * </pre>
 * returns "any A" since B extends A. Null keys aren't allowed.
 *
 * @param <V> type of mapped values
 *
 * @author Nikhil Purushe
 */
@SuppressWarnings("NullableProblems")
public class ClassMap<V> implements Map<Class<?>, V> {

    private final IdentityHashMap<Class<?>, V> map = new IdentityHashMap<>();
    private Class[] keys;

    private Class<?> findKey(Class<?> key) {
        // normally the maps are created upfront and not modified so this is faster
        if (keys == null) {
            Set<Class<?>> keySet = keySet();
            keys = keySet.toArray(new Class[keySet.size()]);
        }
        for (Class<?> cls : keys) {
            if (cls == key) {
                return cls;
            }
        }
        for (Class<?> cls : keys) {
            if (cls.isAssignableFrom(key)) {
                return cls;
            }
        }
        /*if (map.containsKey(key)) {
            return key;
        } else {
            for (Class<?> type : keySet()) {
                if (type.isAssignableFrom(key)) {
                    return type;
                }
            }
        }*/
        return null;
    }

    @Override
    public int size() {
        return map.size();
    }

    @Override
    public boolean isEmpty() {
        return map.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return map.containsKey(findKey((Class<?>) key));
    }

    @Override
    public boolean containsValue(Object value) {
        return map.containsValue(value);
    }

    @Override
    public V get(Object key) {
        return map.get(findKey((Class<?>) key));
    }

    @Override
    public V put(Class<?> key, V value) {
        if (key == null) {
            throw new IllegalArgumentException();
        }
        keys = null;
        return map.put(key, value);
    }

    @Override
    public V remove(Object key) {
        return map.remove(key);
    }

    @Override
    public void putAll(Map<? extends Class<?>, ? extends V> m) {
        if (m != null) {
            for (Map.Entry<? extends Class<?>, ? extends V> entry : m.entrySet()) {
                put(entry.getKey(), entry.getValue());
            }
        }
    }

    @Override
    public void clear() {
        map.clear();
    }

    @Override
    public Set<Class<?>> keySet() {
        return map.keySet();
    }

    @Override
    public Collection<V> values() {
        return map.values();
    }

    @Override
    public Set<Entry<Class<?>, V>> entrySet() {
        return map.entrySet();
    }
}
