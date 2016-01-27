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

/**
 * Result from which elements can be added or removed.
 *
 * @param <E> element type
 */
public interface MutableResult<E> extends Result<E> {

    /**
     * Adds the specified element. This element will be returned in any subsequent
     * {@link #iterator()} but will not be persisted to the backing store until update is called
     * for the entity holding this object.
     *
     * @param element to add
     */
    void add(E element);

    /**
     * Removes the specified element. This element will no longer be returned in any subsequent
     * {@link #iterator()}. Note the element is not actually removed from the backing store until
     * the entity holding this object is updated.
     *
     * @param element to remove
     */
    void remove(E element);

}
