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
 * order clause
 *
 * @param <Q> next query step
 */
public interface OrderBy<Q> {

    /**
     * order the query with the given expression.
     *
     * @param expression expression
     * @param <V> expression type
     * @return query
     */
    <V> Q orderBy(Expression<V> expression);

    /**
     * order the query with the given expressions.
     *
     * @param expressions expressions
     * @return query
     */
    Q orderBy(Expression<?>... expressions);
}
