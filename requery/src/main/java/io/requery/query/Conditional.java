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

import java.util.Collection;

/**
 * Defines an interface for objects for which conditions can be applied to.
 *
 * @param <Q> result of applying the condition
 * @param <V> value type
 */
public interface Conditional<Q, V> {

    /**
     * Applies the equal condition
     *
     * @param value to compare, maybe null, which is equivalent to {@link #isNull()}
     * @return next query step
     */
    Q equal(V value);

    /**
     * Applies the not equal condition, maybe null, which is equivalent to {@link #notNull()}
     *
     * @param value to compare
     * @return next query step
     */
    Q notEqual(V value);

    /**
     * Applies the less than condition
     *
     * @param value to compare
     * @return next query step
     */
    Q lessThan(V value);

    /**
     * Applies the greater than condition
     *
     * @param value to compare
     * @return next query step
     */
    Q greaterThan(V value);

    /**
     * Applies the less than or equal to condition
     *
     * @param value to compare
     * @return next query step
     */
    Q lessThanOrEqual(V value);

    /**
     * Applies the greater than or equal to condition
     *
     * @param value to compare
     * @return next query step
     */
    Q greaterThanOrEqual(V value);

    /**
     * Applies the equal condition, equivalent to {@link #equal(Object)}.
     *
     * @param value to compare
     * @return next query step
     */
    Q eq(V value);

    /**
     * Applies the not equal condition, equivalent to {@link #notEqual(Object)}.
     *
     * @param value to compare
     * @return next query step
     */
    Q ne(V value);

    /**
     * Applies the less than condition, equivalent to {@link #lessThan(Object)}.
     *
     * @param value to compare
     * @return next query step
     */
    Q lt(V value);

    /**
     * Applies the greater than condition, equivalent to {@link #greaterThan(Object)}.
     *
     * @param value to compare
     * @return next query step
     */
    Q gt(V value);

    /**
     * Applies the less than or equal to condition, equivalent to {@link #lessThanOrEqual(Object)}.
     *
     * @param value to compare
     * @return next query step
     */
    Q lte(V value);

    /**
     * Applies the greater than or equal to condition, equivalent to
     * {@link #greaterThanOrEqual(Object)}.
     *
     * @param value to compare
     * @return next query step
     */
    Q gte(V value);

    /**
     * Applies the in condition, checking if the condition is applicable to the values in the given
     * collection.
     *
     * @param values to compare (non null)
     * @return next query step
     */
    Q in(Collection<V> values);

    /**
     * Applies the not in condition, checking if the condition is not applicable to the values in
     * the collection.
     *
     * @param values to compare (non null)
     * @return next query step
     */
    Q notIn(Collection<V> values);

    /**
     * Applies the equal condition
     *
     * @param query nested inner query (non null)
     * @return next query step
     */
    Q in(Return<?> query);

    /**
     * Applies the equal condition
     *
     * @param query nested inner query (non null)
     * @return next query step
     */
    Q notIn(Return<?> query);

    /**
     * Applies the 'is null' condition
     *
     * @return next query step
     */
    Q isNull();

    /**
     * Applies the 'not null' condition
     *
     * @return next query step
     */
    Q notNull();

    /**
     * Applies the equal condition
     *
     * @param expression like expression
     * @return next query step
     */
    Q like(String expression);

    /**
     * Applies the not like condition
     *
     * @param expression like expression
     * @return next query step
     */
    Q notLike(String expression);

    /**
     * Applies the between condition
     *
     * @param start starting value to compare to
     * @param end ending value to compare to
     * @return next query step
     */
    Q between(V start, V end);
}
