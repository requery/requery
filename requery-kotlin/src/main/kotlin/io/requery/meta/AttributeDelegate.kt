/*
 * Copyright 2018 requery.io
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

package io.requery.meta

import io.requery.kotlin.Aliasable
import io.requery.kotlin.Conditional
import io.requery.kotlin.Logical
import io.requery.kotlin.QueryableAttribute
import io.requery.query.*
import io.requery.query.function.Function
import io.requery.query.function.Max
import io.requery.query.function.Min
import io.requery.util.function.Supplier
import java.util.LinkedHashSet

/**
 * Delegates [QueryAttribute] for the core implementation.
 *
 * @author Nikhil Purushe
 */
open class AttributeDelegate<T, V>(attribute : QueryAttribute<T, V>) :
        BaseExpression<V>(),
        QueryableAttribute<T, V>,
        TypeDeclarable<T>,
        Supplier<QueryAttribute<T, V>>,
        Attribute<T, V> by attribute {

    // captures types for use with property extensions
    companion object {
        val types : MutableSet<Type<*>> = LinkedHashSet()
    }

    private val attribute : QueryAttribute<T, V> = attribute

    override fun getName(): String = attribute.name
    override fun getExpressionType(): ExpressionType = attribute.expressionType
    override fun getClassType(): Class<V> = attribute.classType
    override fun getDeclaringType(): Type<T> = attribute.declaringType
    override fun getInnerExpression(): Expression<V>? { return null }

    override fun setDeclaringType(type: Type<T>) {
        if (attribute is BaseAttribute) {
            attribute.declaringType = type
        }
        types.add(type)
    }

    override fun get(): QueryAttribute<T, V> = attribute
}

class StringAttributeDelegate<T, V>(attribute : StringAttribute<T, V>) :
        AttributeDelegate<T, V>(attribute),
        QueryableAttribute<T, V>,
        TypeDeclarable<T>,
        Supplier<QueryAttribute<T, V>>,
        StringExpression<V> by attribute

class NumericAttributeDelegate<T, V>(attribute : NumericAttribute<T, V>) :
        AttributeDelegate<T, V>(attribute),
        QueryableAttribute<T, V>,
        TypeDeclarable<T>,
        Supplier<QueryAttribute<T, V>>,
        NumericExpression<V> by attribute

abstract class BaseExpression<V> protected constructor() :
        Expression<V>,
        Functional<V>,
        Aliasable<Expression<V>>,
        Conditional<Logical<out Expression<V>, *>, V> {

    abstract override fun getName(): String
    abstract override fun getExpressionType(): ExpressionType
    abstract override fun getClassType(): Class<V>

    override val alias: String get() = ""
    override fun `as`(alias: String): Expression<V> = AliasedExpression<V>(this, alias);

    override fun asc(): OrderingExpression<V> = OrderExpression(this, Order.ASC)
    override fun desc(): OrderingExpression<V> = OrderExpression(this, Order.DESC)
    override fun max(): Max<V> = Max.max(this)
    override fun min(): Min<V> = Min.min(this)

    override fun function(name: String): Function<V> {
        return object : Function<V>(name, classType) {
            override fun arguments(): Array<out Any> {
                return arrayOf(this@BaseExpression)
            }
        }
    }

    override fun eq(value: V): Logical<out Expression<V>, V> = LogicalExpression(this, Operator.EQUAL, value)
    override fun ne(value: V): Logical<out Expression<V>, V> = LogicalExpression(this, Operator.NOT_EQUAL, value)
    override fun lt(value: V): Logical<out Expression<V>, V> = LogicalExpression(this, Operator.LESS_THAN, value)
    override fun gt(value: V): Logical<out Expression<V>, V> = LogicalExpression(this, Operator.GREATER_THAN, value)
    override fun lte(value: V): Logical<out Expression<V>, V> = LogicalExpression(this, Operator.LESS_THAN_OR_EQUAL, value)
    override fun gte(value: V): Logical<out Expression<V>, V> = LogicalExpression(this, Operator.GREATER_THAN_OR_EQUAL, value)
    override fun eq(value: Expression<V>): Logical<out Expression<V>, out Expression<V>> = LogicalExpression(this, Operator.EQUAL, value)
    override fun ne(value: Expression<V>): Logical<out Expression<V>, out Expression<V>> = LogicalExpression(this, Operator.NOT_EQUAL, value)
    override fun lt(value: Expression<V>): Logical<out Expression<V>, out Expression<V>> = LogicalExpression(this, Operator.LESS_THAN, value)
    override fun gt(value: Expression<V>): Logical<out Expression<V>, out Expression<V>> = LogicalExpression(this, Operator.GREATER_THAN, value)
    override fun lte(value: Expression<V>): Logical<out Expression<V>, out Expression<V>> = LogicalExpression(this, Operator.LESS_THAN_OR_EQUAL, value)
    override fun gte(value: Expression<V>): Logical<out Expression<V>, out Expression<V>> = LogicalExpression(this, Operator.GREATER_THAN_OR_EQUAL, value)
    override fun `in`(values: Collection<V>): Logical<out Expression<V>, Collection<V>> = LogicalExpression(this, Operator.IN, values)
    override fun notIn(values: Collection<V>): Logical<out Expression<V>, Collection<V>> = LogicalExpression(this, Operator.NOT_IN, values)
    override fun `in`(query: Return<*>): Logical<out Expression<V>, out Return<*>> = LogicalExpression(this, Operator.IN, query)
    override fun notIn(query: Return<*>): Logical<out Expression<V>, out Return<*>> = LogicalExpression(this, Operator.NOT_IN, query)
    override fun isNull(): Logical<out Expression<V>, V> = LogicalExpression(this, Operator.IS_NULL, null)
    override fun notNull(): Logical<out Expression<V>, V> = LogicalExpression(this, Operator.NOT_NULL, null)
    override fun like(expression: String): Logical<out Expression<V>, String> = LogicalExpression(this, Operator.LIKE, expression)
    override fun notLike(expression: String): Logical<out Expression<V>, String> = LogicalExpression(this, Operator.NOT_LIKE, expression)
    override fun between(start: V, end: V): Logical<out Expression<V>, Any> = LogicalExpression(this, Operator.BETWEEN, arrayOf<Any>(start!!, end!!))

    private class LogicalExpression<L, R>
    internal constructor(private val leftOperand: L,
                         private val operator: Operator,
                         private val rightOperand: R?) : Logical<L, R> {

        override fun <V> and(condition: Condition<V, *>): Logical<*, *> = LogicalExpression(this, Operator.AND, condition)
        override fun <V> or(condition: Condition<V, *>): Logical<*, *> = LogicalExpression(this, Operator.OR, condition)

        override fun getOperator(): Operator = operator
        override fun getRightOperand(): R? = rightOperand
        override fun getLeftOperand(): L = leftOperand
    }

    private class OrderExpression<X>
    internal constructor(private val expression: Expression<X>,
                         private val order: Order) : OrderingExpression<X> {

        private var nullOrder: OrderingExpression.NullOrder? = null

        override fun nullsFirst(): OrderingExpression<X> {
            nullOrder = OrderingExpression.NullOrder.FIRST
            return this
        }

        override fun nullsLast(): OrderingExpression<X> {
            nullOrder = OrderingExpression.NullOrder.LAST
            return this
        }
        override fun getOrder(): Order = order
        override fun getNullOrder(): OrderingExpression.NullOrder? = nullOrder
        override fun getName(): String = expression.name
        override fun getClassType(): Class<X> = expression.classType
        override fun getExpressionType(): ExpressionType = ExpressionType.ORDERING
        override fun getInnerExpression(): Expression<X> = expression
    }
}
