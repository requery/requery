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

import java.util.Set;

/**
 * A query that in which different {@link Expression}s can be selected.
 *
 * @param <E> result type
 */
public interface Selectable<E> {

    /**
     * selects the given expressions in the query.
     *
     * @param attributes expressions to select
     * @return selection query
     */
    Selection<E> select(Expression<?>... attributes);

    /**
     * selects the given expressions in the query.
     *
     * @param select expressions to select
     * @return selection query
     */
    Selection<E> select(Set<? extends Expression<?>> select);
}
