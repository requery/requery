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

package io.requery.reactivex;

import io.requery.meta.QueryAttribute;
import io.requery.query.Deletion;
import io.requery.query.Expression;
import io.requery.query.Insertion;
import io.requery.query.Selection;
import io.requery.query.Tuple;
import io.requery.query.Update;

import javax.annotation.CheckReturnValue;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Set;

@ParametersAreNonnullByDefault
public interface ReactiveQueryable<T> {

    @CheckReturnValue
    <E extends T> Selection<ReactiveResult<E>> select(Class<E> type, QueryAttribute<?, ?>... attributes);

    @CheckReturnValue
    <E extends T> Selection<ReactiveResult<E>> select(Class<E> type, Set<? extends QueryAttribute<E, ?>> attributes);

    @CheckReturnValue
    <E extends T> Insertion<ReactiveResult<Tuple>> insert(Class<E> type);

    @CheckReturnValue
    <E extends T> Update<ReactiveScalar<Integer>> update(Class<E> type);

    @CheckReturnValue
    <E extends T> Deletion<ReactiveScalar<Integer>> delete(Class<E> type);

    @CheckReturnValue
    <E extends T> Selection<ReactiveScalar<Integer>> count(Class<E> type);

    @CheckReturnValue
    Selection<ReactiveScalar<Integer>> count(QueryAttribute<?, ?>... attributes);

    @CheckReturnValue
    Selection<ReactiveResult<Tuple>> select(Expression<?>... expressions);

    @CheckReturnValue
    Selection<ReactiveResult<Tuple>> select(Set<? extends Expression<?>> expressions);

    @CheckReturnValue
    Update<ReactiveScalar<Integer>> update();

    @CheckReturnValue
    Deletion<ReactiveScalar<Integer>> delete();

    @CheckReturnValue
    ReactiveResult<Tuple> raw(String query, Object... parameters);

    @CheckReturnValue
    <E extends T> ReactiveResult<E> raw(Class<E> type, String query, Object... parameters);
}
