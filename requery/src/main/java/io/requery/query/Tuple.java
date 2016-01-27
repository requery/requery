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
 * A collection of key-value pairs from a query.
 *
 * @author Nikhil Purushe
 */
public interface Tuple {

    /**
     * Retrieve a value by a {@link Expression} instance.
     *
     * @param key expression instance, this should be the same expression used in the query used to
     *            derive this result.
     * @param <V> type of result.
     * @return the result for the given expression.
     */
    <V> V get(Expression<V> key);

    /**
     * Retrieve a value by a column or alias name.
     *
     * @param key the alias or column name used in the query
     * @param <V> type of result.
     * @return the result for the given key or null if it does not exist.
     */
    <V> V get(String key);

    /**
     * Retrieve a value by a numeric index.
     *
     * @param index 0 based index of the result set.
     * @param <V>   type of result.
     * @return the result at the given index.
     */
    <V> V get(int index);

    /**
     * @return the number of items in this tuple instance.
     */
    int count();
}
