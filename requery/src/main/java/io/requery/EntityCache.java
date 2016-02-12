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

package io.requery;

/**
 * Cache of entity objects.
 *
 * @author Nikhil Purushe
 */
public interface EntityCache {

    /**
     * Retrieve an existing entity if it exists in the cache.
     *
     * @param type entity class
     * @param key  entity key
     * @param <T>  entity type
     * @return the cached entity or null.
     */
    <T> T get(Class<T> type, Object key);

    /**
     * Put a reference to an entity.
     *
     * @param type  entity class
     * @param key   entity key
     * @param value entity reference
     * @param <T>   entity type
     */
    <T> void put(Class<T> type, Object key, T value);

    /**
     * Check if a reference to an entity exists.
     *
     * @param type entity class
     * @param key  entity key
     * @return true if a reference exists false otherwise.
     */
    boolean contains(Class<?> type, Object key);

    /**
     * Removes all references for the given type from the cache.
     *
     * @param type entity class
     */
    void invalidate(Class<?> type);

    /**
     * Remove a reference to an entity from the cache.
     *
     * @param type entity class
     * @param key  entity key
     */
    void invalidate(Class<?> type, Object key);

    /**
     * Evict all references from the cache.
     */
    void clear();
}
