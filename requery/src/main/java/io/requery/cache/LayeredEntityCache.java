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

import io.requery.EntityCache;

import java.util.List;

public class LayeredEntityCache implements EntityCache {

    private final List<EntityCache> caches;

    public LayeredEntityCache(List<EntityCache> caches) {
        this.caches = caches;
    }

    @Override
    public <T> T get(Class<T> type, Object key) {
        for (EntityCache cache : caches) {
            T value = cache.get(type, key);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    @Override
    public <T> void put(Class<T> type, Object key, T value) {
        for (EntityCache cache : caches) {
            cache.put(type, key, value);
        }
    }

    @Override
    public boolean contains(Class<?> type, Object key) {
        for (EntityCache cache : caches) {
            boolean contains = cache.contains(type, key);
            if (contains) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void invalidate(Class<?> type) {
        for (EntityCache cache : caches) {
            cache.invalidate(type);
        }
    }

    @Override
    public void invalidate(Class<?> type, Object key) {
        for (EntityCache cache : caches) {
            cache.invalidate(type, key);
        }
    }

    @Override
    public void clear() {
        for (EntityCache cache : caches) {
            cache.clear();
        }
    }
}
