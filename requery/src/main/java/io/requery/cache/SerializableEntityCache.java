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
import io.requery.meta.Attribute;
import io.requery.meta.EntityModel;
import io.requery.meta.Type;
import io.requery.proxy.CompositeKey;
import io.requery.util.ClassMap;

import javax.cache.Cache;
import javax.cache.CacheException;
import javax.cache.CacheManager;
import javax.cache.configuration.Factory;
import javax.cache.configuration.MutableConfiguration;
import javax.cache.expiry.EternalExpiryPolicy;
import javax.cache.expiry.ExpiryPolicy;
import java.util.Map;
import java.util.Set;

/**
 * Cache using the JSR-107 API.
 *
 * @author Nikhil Purushe
 */
@SuppressWarnings("unchecked")
public class SerializableEntityCache implements EntityCache {

    private final EntityModel model;
    private final CacheManager cacheManager;
    private final ClassMap<Cache<?, ?>> caches;
    private final Factory<ExpiryPolicy> expiryPolicyFactory;

    public SerializableEntityCache(EntityModel model, CacheManager cacheManager) {
        if (cacheManager == null) {
            throw new IllegalArgumentException();
        }
        this.model = model;
        this.cacheManager = cacheManager;
        this.expiryPolicyFactory = EternalExpiryPolicy.factoryOf();
        this.caches = new ClassMap<>();
    }

    protected String getCacheName(Type<?> type) {
        return type.getName();
    }

    protected <K, V> void configure(MutableConfiguration<K, V> configuration) {
        configuration.setExpiryPolicyFactory(expiryPolicyFactory);
    }

    private <T> Class getKeyClass(Type<T> type) {
        Set<Attribute<T, ?>> ids = type.getKeyAttributes();
        Class keyClass;
        if (ids.isEmpty()) {
            // use hash code
            return Integer.class;
        }
        if (ids.size() == 1) {
            keyClass = ids.iterator().next().getClassType();
            if (keyClass.isPrimitive()) {
                if (keyClass == int.class) {
                    keyClass = Integer.class;
                } else if (keyClass == long.class) {
                    keyClass = Long.class;
                }
            }
        } else {
            keyClass = CompositeKey.class;
        }
        return keyClass;
    }

    protected <K, T> Cache<K, SerializedEntity<T>> createCache(String cacheName, Type<T> type) {
        Class keyClass = getKeyClass(type);
        if (keyClass == null) {
            throw new IllegalStateException();
        }
        MutableConfiguration<K, SerializedEntity<T>> configuration =
                new MutableConfiguration<>();

        configuration.setTypes(keyClass, (Class) SerializedEntity.class);
        configure(configuration);
        return cacheManager.createCache(cacheName, configuration);
    }

    private <K, T> Cache<K, SerializedEntity<T>> tryCreateCache(Class<T> type) {
        Type<T> declaredType = model.typeOf(type);
        String cacheName = getCacheName(declaredType);
        Cache<K, SerializedEntity<T>> cache = cacheManager.getCache(cacheName);
        if (cache == null) {
            // try creating it, if failed see if it was already created
            try {
                cache = createCache(cacheName, declaredType);
            } catch (CacheException ce) {
                cache = cacheManager.getCache(cacheName);
                if (cache == null) {
                    throw ce;
                }
            }
        }
        return cache;
    }

    private Cache getCache(Class<?> type) {
        Cache cache;
        synchronized (caches) {
            cache = caches.get(type);
            if (cache == null) {
                Type declaredType = model.typeOf(type);
                String cacheName = getCacheName(declaredType);
                Class keyClass = getKeyClass(declaredType);
                cache = cacheManager.getCache(cacheName, keyClass, SerializedEntity.class);
            }
        }
        return cache;
    }

    @Override
    public <T> T get(Class<T> type, Object key) {
        Cache cache = getCache(type);
        if (cache != null && cache.isClosed()) {
            cache = null;
        }
        if (cache != null) {
            SerializedEntity container = (SerializedEntity) cache.get(key);
            if (container != null) {
                return type.cast(container.getEntity());
            }
        }
        return null;
    }

    @Override
    public <T> void put(Class<T> type, Object key, T value) {
        Cache<Object, SerializedEntity<T>> cache;
        synchronized (caches) {
            cache = getCache(type);
            if (cache == null) {
                cache = tryCreateCache(type);
            }
        }
        cache.put(key, new SerializedEntity<>(type, value));
    }

    @Override
    public boolean contains(Class<?> type, Object key) {
        Cache cache = getCache(type);
        return cache != null && !cache.isClosed() && cache.containsKey(key);
    }

    @Override
    public void invalidate(Class<?> type) {
        Cache cache = getCache(type);
        if (cache != null) {
            cache.clear();
            String cacheName = getCacheName(model.typeOf(type));
            cacheManager.destroyCache(cacheName);
            synchronized (caches) {
                caches.remove(type);
            }
            cache.close();
        }
    }

    @Override
    public void invalidate(Class<?> type, Object key) {
        Cache cache = getCache(type);
        if (cache != null && !cache.isClosed()) {
            cache.remove(key);
        }
    }

    @Override
    public void clear() {
        synchronized (caches) {
            for (Map.Entry<Class<?>, Cache<?, ?>> entry : caches.entrySet()) {
                invalidate(entry.getKey());
            }
        }
    }
}
