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
import io.requery.meta.EntityModel;

import javax.cache.CacheManager;
import java.util.LinkedList;

public class EntityCacheBuilder {

    private final EntityModel model;
    private boolean useReferenceCache;
    private boolean useSerializableCache;
    private CacheManager cacheManager;

    public EntityCacheBuilder(EntityModel model) {
        if (model == null) {
            throw new IllegalArgumentException();
        }
        this.model = model;
    }

    public EntityCacheBuilder useReferenceCache(boolean enable) {
        this.useReferenceCache = enable;
        return this;
    }

    public EntityCacheBuilder useSerializableCache(boolean enable) {
        this.useSerializableCache = enable;
        return this;
    }

    public EntityCacheBuilder useCacheManager(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
        return this;
    }

    public EntityCache build() {
        LinkedList<EntityCache> caches = new LinkedList<>();
        if (useReferenceCache) {
            caches.add(new WeakEntityCache());
        }
        if (useSerializableCache) {
            SerializationContext.map(model);
            caches.add(new SerializableEntityCache(model, cacheManager));
        }
        if (caches.isEmpty()) {
            return new EmptyEntityCache();
        } else {
            return new LayeredEntityCache(caches);
        }
    }
}
