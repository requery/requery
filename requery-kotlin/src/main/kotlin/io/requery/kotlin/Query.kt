/*
 * Copyright 2017 requery.io
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

package io.requery.kotlin

import io.requery.query.Condition
import io.requery.query.Expression
import io.requery.query.Return
import io.requery.query.Scalar
import io.requery.util.function.Supplier
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

operator fun <R> Return<R>.invoke() = get()

interface Conditional<out Q, V> {

    infix fun eq(value: V): Q
    infix fun ne(value: V): Q
    infix fun lt(value: V): Q
    infix fun gt(value: V): Q
    infix fun lte(value: V): Q
    infix fun gte(value: V): Q
    infix fun eq(value: Expression<V>): Q
    infix fun ne(value: Expression<V>): Q
    infix fun lt(value: Expression<V>): Q
    infix fun gt(value: Expression<V>): Q
    infix fun lte(value: Expression<V>): Q
    infix fun gte(value: Expression<V>): Q
    infix fun `in`(values: Collection<V>): Q
    infix fun notIn(values: Collection<V>): Q
    fun `in`(query: Return<*>): Q
    fun notIn(query: Return<*>): Q
    fun isNull(): Q
    fun notNull(): Q
    infix fun like(expression: String): Q
    infix fun notLike(expression: String): Q
    fun between(start: V, end: V): Q
}


interface Logical<out L, out R> : Condition<@UnsafeVariance L, @UnsafeVariance R>, AndOr<Logical<*, *>>

interface Aliasable<out T> {
    infix fun `as`(alias: String): T
    val alias: String
}

interface AndOr<out Q> {
    infix fun <V> and(condition: Condition<V, *>): Q
    infix fun <V> or(condition: Condition<V, *>): Q
}

interface Deletion<E> :
        From<E>, Join<E>, Where<E>, GroupBy<SetHavingOrderByLimit<E>>, OrderBy<Limit<E>>, Return<E>

interface Distinct<out Q> {
    fun distinct(): Q
}

interface DistinctSelection<E> :
        From<E>, Join<E>, Where<E>, SetOperation<Selectable<E>>, GroupBy<SetHavingOrderByLimit<E>>,
        OrderBy<Limit<E>>, Return<E>

interface Exists<out Q> {
    infix fun exists(query: Return<*>): Q
    infix fun notExists(query: Return<*>): Q
}

interface From<E> : Return<E> {
    fun from(vararg types: KClass<out Any>): JoinWhereGroupByOrderBy<E>
    fun from(vararg types: Class<out Any>): JoinWhereGroupByOrderBy<E>
    fun from(vararg queries: Supplier<*>): JoinWhereGroupByOrderBy<E>
}

interface GroupBy<out Q> {
    fun groupBy(vararg expressions: Expression<*>): Q
    infix fun <V> groupBy(expression: Expression<V>): Q
}

interface Having<E> {
    infix fun <V> having(condition: Condition<V, *>): HavingAndOr<E>
}

interface HavingAndOr<E> : AndOr<HavingAndOr<E>>, OrderByLimit<E>

interface Insertion<E> : Return<E> {
    fun <V> value(expression: Expression<V>, value: V): Insertion<E>
}

interface InsertInto<Q> : Return<Q> {
    infix fun query(query: Return<*>): Return<Q>
}

interface Join<E> {
    infix fun join(type: KClass<out Any>): JoinOn<E>
    infix fun leftJoin(type: KClass<out Any>): JoinOn<E>
    infix fun rightJoin(type: KClass<out Any>): JoinOn<E>
    infix fun <J> join(query: Return<J>): JoinOn<E>
    infix fun <J> leftJoin(query: Return<J>): JoinOn<E>
    infix fun <J> rightJoin(query: Return<J>): JoinOn<E>
}

interface JoinAndOr<E> : AndOr<JoinAndOr<E>>, JoinWhereGroupByOrderBy<E>

interface JoinOn<E> {
    infix fun <V> on(field: Condition<V, *>): JoinAndOr<E>
}

interface JoinWhereGroupByOrderBy<E> : Join<E>, Where<E>, GroupBy<SetHavingOrderByLimit<E>>,
        OrderBy<Limit<E>>, Return<E>

interface Limit<E> : Return<E> {
    infix fun limit(limit: Int): Offset<E>
}

interface Offset<E> : Return<E> {
    infix fun offset(offset: Int): Return<E>
}

interface OrderBy<out Q> {
    infix fun <V> orderBy(expression: Expression<V>): Q
    fun orderBy(vararg expressions: Expression<*>): Q
}

interface OrderByLimit<E> : OrderBy<Limit<E>>, Limit<E>

interface Selectable<E> {
    fun select(vararg attributes: Expression<*>): Selection<E>
}

interface Selection<E> : Distinct<DistinctSelection<E>>,
        From<E>,
        Join<E>,
        Where<E>,
        SetOperation<Selectable<E>>,
        GroupBy<SetHavingOrderByLimit<E>>,
        OrderBy<Limit<E>>,
        Return<E>

interface SetGroupByOrderByLimit<E> :
        SetOperation<Selectable<E>>,
        GroupBy<SetHavingOrderByLimit<E>>,
        OrderBy<Limit<E>>,
        Limit<E>

interface SetHavingOrderByLimit<E> :
        SetOperation<Selectable<E>>, Having<E>, OrderBy<Limit<E>>, Limit<E>

interface SetOperation<Q> {
    fun union(): Q
    fun unionAll(): Q
    fun intersect(): Q
    fun except(): Q
}

interface Update<E> :
        Join<E>,
        Where<E>,
        GroupBy<SetHavingOrderByLimit<E>>,
        OrderBy<Limit<E>>,
        Return<E> {
    fun <V> set(expression: Expression<V>, value: V): Update<E>
}

inline fun <reified T : Any, V> Update<Scalar<Int>>
        .set(property: KProperty1<T, V>, value: V): Update<Scalar<Int>> {
    return set(findAttribute(property), value)
}

interface Where<E> : SetGroupByOrderByLimit<E>, Return<E> {
    fun where(): Exists<SetGroupByOrderByLimit<E>>
    infix fun <V> where(condition: Condition<V, *>): WhereAndOr<E>
}

interface WhereAndOr<E> : AndOr<WhereAndOr<E>>, SetGroupByOrderByLimit<E>
