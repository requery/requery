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

import io.requery.util.Objects;
import io.requery.EntityCache;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

/**
 * Weak reference cache of entity objects using primary keys.
 *
 * @author Nikhil Purushe
 */
public class WeakEntityCache implements EntityCache {

    private final Map<Class<?>, WeakReferenceMap<?>> maps;

    private static class KeyReference<S> extends WeakReference<S> {

        private final Object key;

        KeyReference(Object key, S value, ReferenceQueue<S> referenceQueue) {
            super(value, referenceQueue);
            this.key = key;
        }

        Object getKey() {
            return key;
        }
    }

    private static class WeakReferenceMap<T> extends HashMap<Object, Reference<T>> {

        private final ReferenceQueue<T> referenceQueue = new ReferenceQueue<>();

        public T getValue(Object key) {
            removeStaleEntries();
            Reference<T> ref = get(key);
            return ref == null ? null : ref.get();
        }

        public void putValue(Object key, T value) {
            Objects.requireNotNull(key);
            Objects.requireNotNull(value);
            removeStaleEntries();
            put(key, new KeyReference<>(key, value, referenceQueue));
        }

        private void removeStaleEntries() {
            Reference<? extends T> reference;
            while ((reference = referenceQueue.poll()) != null) {
                T value = reference.get();
                if (value == null) {
                    KeyReference keyReference = KeyReference.class.cast(reference);
                    Object key = keyReference.getKey();
                    remove(key);
                }
            }
        }
    }

    public WeakEntityCache() {
        maps = new HashMap<>();
    }

    @Override
    public <T> T get(Class<T> type, Object key) {
        synchronized (maps) {
            WeakReferenceMap<?> map = maps.get(type);
            if (map == null) {
                return null;
            }
            Object value = map.getValue(key);
            return type.cast(value);
        }
    }

    @Override
    public <T> void put(Class<T> type, Object key, T value) {
        Objects.requireNotNull(type);
        synchronized (maps) {
            @SuppressWarnings("unchecked")
            WeakReferenceMap<T> map = (WeakReferenceMap<T>) maps.get(type);
            if (map == null) {
                maps.put(type, map = new WeakReferenceMap<>());
            }
            map.putValue(key, value);
        }
    }

    @Override
    public boolean contains(Class<?> type, Object key) {
        synchronized (maps) {
            WeakReferenceMap<?> map = maps.get(type);
            return map != null && map.containsKey(key);
        }
    }

    @Override
    public void invalidate(Class<?> type, Object key) {
        synchronized (maps) {
            WeakReferenceMap<?> map = maps.get(type);
            if (map != null) {
                map.remove(key);
            }
        }
    }

    @Override
    public void invalidate(Class<?> type) {
        synchronized (maps) {
            WeakReferenceMap<?> map = maps.get(type);
            if (map != null) {
                map.clear();
            }
        }
    }

    @Override
    public void clear() {
        synchronized (maps) {
            maps.clear();
        }
    }
}
