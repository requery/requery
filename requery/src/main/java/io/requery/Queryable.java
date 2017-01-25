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

import io.requery.meta.QueryAttribute;
import io.requery.query.Deletion;
import io.requery.query.Expression;
import io.requery.query.Insertion;
import io.requery.query.Result;
import io.requery.query.Return;
import io.requery.query.Scalar;
import io.requery.query.Selection;
import io.requery.query.Tuple;
import io.requery.query.Update;

import javax.annotation.CheckReturnValue;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Set;

/**
 * Interface for querying both {@link Entity} objects and selectable attributes of those objects.
 *
 * @param <T> the base class or interface to restrict all entities that are queried to.
 *
 * @author Nikhil Purushe
 */
@ParametersAreNonnullByDefault
public interface Queryable<T> {

    /**
     * Initiates a query against a set of expression values return a specific entity type in a
     * {@link Result}.
     *
     * @param type       of entity
     * @param attributes to select (must of be of type E, not enforced due to erasure)
     * @param <E>        entity type
     * @return next query step
     */
    @CheckReturnValue
    <E extends T> Selection<? extends Result<E>>
    select(Class<E> type, QueryAttribute<?, ?>... attributes);

    /**
     * Initiates a query against a set of expression values return a specific entity type in a
     * {@link Result}.
     *
     * @param type       of entity
     * @param attributes to select (must of be of type E, not enforced due to erasure)
     * @param <E>        entity type
     * @return next query step
     */
    @CheckReturnValue
    <E extends T> Selection<? extends Result<E>>
    select(Class<E> type, Set<? extends QueryAttribute<E, ?>> attributes);

    /**
     * Initiates an insert operation for a type. After completing the expression call
     * {@link Return#get()} to perform the operation. If the type has no generated key values the
     * result is a tuple containing a single element with the number of rows affected by the
     * operation. Otherwise if the type has generated keys those keys are returned as the result.
     *
     * @param type of entity
     * @param <E>  entity type
     * @return next query step
     */
    @CheckReturnValue
    <E extends T> Insertion<? extends Result<Tuple>> insert(Class<E> type);

    /**
     * Initiates an update query for a type. After completing the expression call
     * {@link Return#get()} to perform the operation. The result is the number of rows
     * affected by the call. Note that aggregate update queries will not affect existing entity
     * objects in memory.
     *
     * @param type of entity
     * @param <E>  entity type
     * @return next query step
     */
    @CheckReturnValue
    <E extends T> Update<? extends Scalar<Integer>> update(Class<E> type);

    /**
     * Initiates an delete query for a type. After completing the expression call
     * {@link Return#get()} to perform the operation. Note that aggregate delete queries
     * will not affect existing entity objects in memory or in the {@link EntityCache} associated
     * with the store.
     *
     * @param type of entity
     * @param <E>  entity type
     * @return next query step
     */
    @CheckReturnValue
    <E extends T> Deletion<? extends Scalar<Integer>> delete(Class<E> type);

    /**
     * Initiates a query to count the number of entities of a given type.
     *
     * @param type of entity
     * @param <E>  entity type
     * @return next query step
     */
    @CheckReturnValue
    <E extends T> Selection<? extends Scalar<Integer>> count(Class<E> type);

    /**
     * Initiates a query to count a given selection.
     *
     * @param attributes to select
     * @return next query step
     */
    @CheckReturnValue
    Selection<? extends Scalar<Integer>> count(QueryAttribute<?, ?>... attributes);

    /**
     * Initiates a query against a set of expression values.
     *
     * @param expressions to select
     * @return next query step
     */
    @CheckReturnValue
    Selection<? extends Result<Tuple>> select(Expression<?>... expressions);

    /**
     * Initiates a query against a set of expression values.
     *
     * @param expressions to select, cannot be null or empty
     * @return next query step
     */
    @CheckReturnValue
    Selection<? extends Result<Tuple>> select(Set<? extends Expression<?>> expressions);

    /**
     * Initiates an update query against this data store. After completing the expression call
     * {@link Return#get()} to perform the operation. The result is the number of rows
     * affected by the call. Note that aggregate update queries will not affect existing entity
     * objects in memory.
     *
     * @return next query step
     */
    @CheckReturnValue
    Update<? extends Scalar<Integer>> update();

    /**
     * Initiates a delete query against this data store. After completing the expression call
     * {@link Return#get()} to perform the operation. The result is the number of rows
     * affected by the call. Note that aggregate update queries will not affect existing entity
     * objects in memory.
     *
     * @return next query step
     */
    @CheckReturnValue
    Deletion<? extends Scalar<Integer>> delete();

    /**
     * Executes a raw query against the data store.
     *
     * @param query      raw query to execute
     * @param parameters query arguments, the number of arguments must match the number of place
     *                   holder values in the query or a {@link PersistenceException} will be
     *                   thrown.
     * @return the result of the query as a {@link Tuple}.
     */
    @CheckReturnValue
    Result<Tuple> raw(String query, Object... parameters);

    /**
     * Executes a raw query against the data store mapping on to a specific entity type.
     *
     * @param type       entity type
     * @param query      raw query to execute
     * @param parameters query arguments, the number of arguments must match the number of place
     *                   holder values in the query or a {@link PersistenceException} will be
     *                   thrown.
     * @param <E>        entity type
     * @return the result of the query.
     */
    @CheckReturnValue
    <E extends T> Result<E> raw(Class<E> type, String query, Object... parameters);
}
