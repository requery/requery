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

package io.requery.kotlin

import io.requery.meta.EntityModel
import io.requery.query.Condition
import io.requery.query.Expression
import io.requery.query.Return
import io.requery.query.element.*
import io.requery.util.function.Supplier
import java.util.*
import kotlin.reflect.KClass

class ExistsDelegate<E : Any>(element: ExistsElement<E>, query : QueryDelegate<E>)
: Exists<SetGroupByOrderByLimit<E>> {

    private val element: ExistsElement<E> = element
    private val query : QueryDelegate<E> = query

    override fun exists(query: Return<*>): SetGroupByOrderByLimit<E> {
        element.exists(query)
        return this.query
    }

    override fun notExists(query: Return<*>): SetGroupByOrderByLimit<E> {
        element.notExists(query)
        return this.query
    }
}

class HavingDelegate<E : Any>(element: HavingConditionElement<E>, query
: QueryDelegate<E>) : HavingAndOr<E> {

    private var element : HavingConditionElement<E> = element
    private var query : QueryDelegate<E> = query

    override fun limit(limit: Int): Offset<E> {
        query.limit(limit)
        return query
    }

    override fun `as`(alias: String?): Return<E>? {
        query.`as`(alias)
        return query
    }

    override fun getAlias(): String? = query.alias

    override fun get(): E = query.get()

    override fun <V> and(condition: Condition<V, *>): HavingAndOr<E> =
            HavingDelegate(element.and(condition) as HavingConditionElement<E>, query)

    override fun <V> or(condition: Condition<V, *>): HavingAndOr<E> =
            HavingDelegate(element.or(condition) as HavingConditionElement<E>, query)

    override fun <V> orderBy(expression: Expression<V>): Limit<E> {
        query.orderBy(expression)
        return query
    }

    override fun orderBy(vararg expressions: Expression<*>): Limit<E> {
        query.orderBy(*expressions)
        return query
    }
}

class WhereDelegate<E : Any>(element: WhereConditionElement<E>, query : QueryDelegate<E>)
: WhereAndOr<E>, SetGroupByOrderByLimit<E> by query {

    private var element : WhereConditionElement<E> = element
    private var query : QueryDelegate<E> = query

    override fun <V> and(condition: Condition<V, *>): WhereAndOr<E> =
            WhereDelegate(element.and(condition) as WhereConditionElement<E>, query)

    override fun <V> or(condition: Condition<V, *>): WhereAndOr<E> =
            WhereDelegate(element.or(condition) as WhereConditionElement<E>, query)
}

class JoinDelegate<E : Any>(element : JoinConditionElement<E>, query: QueryDelegate<E>)
: JoinAndOr<E>, JoinWhereGroupByOrderBy<E> by query {

    private var element : JoinConditionElement<E> = element
    private var query : QueryDelegate<E> = query

    override fun <V> and(condition: Condition<V, *>): JoinAndOr<E> =
            JoinDelegate(element.and(condition) as JoinConditionElement<E>, query)

    override fun <V> or(condition: Condition<V, *>): JoinAndOr<E> =
            JoinDelegate(element.or(condition) as JoinConditionElement<E>, query)
}

class JoinOnDelegate<E : Any>(element : JoinOnElement<E>, query : QueryDelegate<E>) : JoinOn<E> {
    private var query : QueryDelegate<E> = query
    private var element : JoinOnElement<E> = element

    override fun <V> on(field: Condition<V, *>): JoinAndOr<E> {
        val join = element.on(field) as JoinConditionElement<E>
        return JoinDelegate(join, query)
    }
}

class QueryDelegate<E : Any>(element : QueryElement<E>) :
        Selectable<E>,
        Selection<E>,
        DistinctSelection<E>,
        Insertion<E>,
        Update<E>,
        Deletion<E>,
        JoinWhereGroupByOrderBy<E>,
        SetGroupByOrderByLimit<E>,
        SetHavingOrderByLimit<E>,
        OrderByLimit<E>,
        Offset<E> {

    private var element : QueryElement<E> = element

    constructor(type : QueryType, model : EntityModel, operation : QueryOperation<E>)
    : this(QueryElement(type, model, operation))

    override fun select(vararg attributes: Expression<*>): Selection<E> {
        element.select(*attributes)
        return this
    }

    override fun union(): Selectable<E> = QueryDelegate(element.union() as QueryElement<E>)

    override fun unionAll(): Selectable<E> = QueryDelegate(element.unionAll() as QueryElement<E>)

    override fun intersect(): Selectable<E> = QueryDelegate(element.intersect() as QueryElement<E>)

    override fun except(): Selectable<E> = QueryDelegate(element.except() as QueryElement<E>)

    override fun join(type: KClass<out Any>): JoinOn<E> =
            JoinOnDelegate(element.join(type.java) as JoinOnElement<E>, this)

    override fun leftJoin(type: KClass<out Any>): JoinOn<E> =
            JoinOnDelegate(element.leftJoin(type.java) as JoinOnElement<E>, this)

    override fun rightJoin(type: KClass<out Any>): JoinOn<E> =
            JoinOnDelegate(element.rightJoin(type.java) as JoinOnElement<E>, this)

    override fun <J> join(query: Return<J>): JoinOn<E> =
            JoinOnDelegate(element.join(query) as JoinOnElement<E>, this)

    override fun <J> leftJoin(query: Return<J>): JoinOn<E> =
            JoinOnDelegate(element.leftJoin(query) as JoinOnElement<E>, this)

    override fun <J> rightJoin(query: Return<J>): JoinOn<E> =
            JoinOnDelegate(element.rightJoin(query) as JoinOnElement<E>, this)

    override fun groupBy(vararg expressions: Expression<*>): SetHavingOrderByLimit<E> {
        element.groupBy(*expressions)
        return this
    }

    override fun <V> groupBy(expression: Expression<V>): SetHavingOrderByLimit<E> {
        element.groupBy(expression)
        return this
    }

    override fun <V> having(condition: Condition<V, *>): HavingAndOr<E> =
            HavingDelegate(element.having(condition) as HavingConditionElement<E>, this)

    override fun <V> orderBy(expression: Expression<V>): Limit<E> {
        element.orderBy(expression)
        return this
    }

    override fun orderBy(vararg expressions: Expression<*>): Limit<E> {
        element.orderBy(*expressions)
        return this
    }

    @Suppress("UNCHECKED_CAST")
    override fun where(): Exists<SetGroupByOrderByLimit<E>> =
            ExistsDelegate(element.where() as ExistsElement<E>, this)

    override fun <V> where(condition: Condition<V, *>): WhereAndOr<E> =
            WhereDelegate(element.where(condition) as WhereConditionElement<E>, this)

    override fun `as`(alias: String?): Return<E>? = element.`as`(alias)

    override fun getAlias(): String? = element.alias

    override fun distinct(): DistinctSelection<E> {
        element.distinct()
        return this
    }

    override fun from(vararg types: KClass<out Any>): JoinWhereGroupByOrderBy<E> {
        val javaClasses = Array<Class<*>?>(types.size, {i -> types[i].java })
        element.from(*javaClasses)
        return this
    }

    override fun from(vararg types: Class<out Any>): JoinWhereGroupByOrderBy<E> {
        element.from(*types)
        return this
    }

    override fun from(vararg subqueries: Supplier<*>): JoinWhereGroupByOrderBy<E> {
        val list = ArrayList<QueryDelegate<*>>()
        subqueries.forEach { it -> run {
            val element = it.get()
            if (it is QueryDelegate) {
                list.add(element as QueryDelegate<*>)
            }
        } }
        return this
    }

    override fun get(): E = element.get()

    override fun limit(limit: Int): Offset<E> {
        element.limit(limit)
        return this
    }

    override fun offset(offset: Int): Return<E> = element.offset(offset)

    override fun <V> value(expression: Expression<V>, value: V): Insertion<E> {
        element.value(expression, value)
        return this
    }

    override fun <V> set(expression: Expression<V>, value: V): Update<E> {
        element.set(expression, value)
        return this
    }

    override fun equals(other: Any?): Boolean {
        if (other is QueryDelegate<*>) {
            return other.element.equals(element)
        }
        return false
    }

    override fun hashCode(): Int = element.hashCode()

    @Suppress("UNCHECKED_CAST")
    fun <F : E> extend(transform: io.requery.util.function.Function<E, F>): QueryDelegate<F> {
        element.extend(transform)
        return this as QueryDelegate<F>
    }
}
