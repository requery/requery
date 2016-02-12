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
 * Limit clause
 *
 * @param <E> result type
 */
public interface Limit<E> extends Return<E> {

    /**
     * Defines the maximum amount of elements in the final result.
     *
     * @param limit number of results to limit the query to.
     * @return next query step
     */
    Offset<E> limit(int limit);
}
