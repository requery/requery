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

import io.requery.meta.Attribute
import io.requery.meta.AttributeDelegate
import io.requery.meta.Type
import io.requery.query.Expression
import io.requery.query.OrderingExpression
import io.requery.query.Return
import io.requery.query.function.Abs
import io.requery.query.function.Avg
import io.requery.query.function.Lower
import io.requery.query.function.Max
import io.requery.query.function.Min
import io.requery.query.function.Round
import io.requery.query.function.Substr
import io.requery.query.function.Sum
import io.requery.query.function.Trim
import io.requery.query.function.Upper
import kotlin.reflect.KProperty1

inline fun <reified T : Any, R> KProperty1<T, R>.asc(): OrderingExpression<R> = findAttribute(this).asc()
inline fun <reified T : Any, R> KProperty1<T, R>.desc(): OrderingExpression<R> = findAttribute(this).desc()
inline fun <reified T : Any, R> KProperty1<T, R>.abs(): Abs<R> = findAttribute(this).abs()
inline fun <reified T : Any, R> KProperty1<T, R>.max(): Max<R> = findAttribute(this).max()
inline fun <reified T : Any, R> KProperty1<T, R>.min(): Min<R> = findAttribute(this).min()
inline fun <reified T : Any, R> KProperty1<T, R>.avg(): Avg<R> = findAttribute(this).avg()
inline fun <reified T : Any, R> KProperty1<T, R>.sum(): Sum<R> = findAttribute(this).sum()
inline fun <reified T : Any, R> KProperty1<T, R>.round(): Round<R> = findAttribute(this).round()
inline fun <reified T : Any, R> KProperty1<T, R>.round(decimals: Int): Round<R> = findAttribute(this).round(decimals)
inline fun <reified T : Any, R> KProperty1<T, R>.trim(chars: String?): Trim<R> = findAttribute(this).trim(chars)
inline fun <reified T : Any, R> KProperty1<T, R>.trim(): Trim<R> = findAttribute(this).trim()
inline fun <reified T : Any, R> KProperty1<T, R>.substr(offset: Int, length: Int): Substr<R> = findAttribute(this).substr(offset, length)
inline fun <reified T : Any, R> KProperty1<T, R>.upper(): Upper<R> = findAttribute(this).upper()
inline fun <reified T : Any, R> KProperty1<T, R>.lower(): Lower<R> = findAttribute(this).lower()

inline infix fun <reified T : Any, R> KProperty1<T, R>.eq(value: R): Logical<out Expression<R>, R> = findAttribute(this).eq(value)
inline infix fun <reified T : Any, R> KProperty1<T, R>.ne(value: R): Logical<out Expression<R>, R> = findAttribute(this).ne(value)
inline infix fun <reified T : Any, R> KProperty1<T, R>.lt(value: R): Logical<out Expression<R>, R> = findAttribute(this).lt(value)
inline infix fun <reified T : Any, R> KProperty1<T, R>.gt(value: R): Logical<out Expression<R>, R> = findAttribute(this).gt(value)
inline infix fun <reified T : Any, R> KProperty1<T, R>.lte(value: R): Logical<out Expression<R>, R> = findAttribute(this).lte(value)
inline infix fun <reified T : Any, R> KProperty1<T, R>.gte(value: R): Logical<out Expression<R>, R> = findAttribute(this).gte(value)
inline infix fun <reified T : Any, R> KProperty1<T, R>.eq(value: Expression<R>): Logical<out Expression<R>, out Expression<R>> = findAttribute(this).eq(value)
inline infix fun <reified T : Any, R> KProperty1<T, R>.ne(value: Expression<R>): Logical<out Expression<R>, out Expression<R>> = findAttribute(this).ne(value)
inline infix fun <reified T : Any, R> KProperty1<T, R>.lt(value: Expression<R>): Logical<out Expression<R>, out Expression<R>> = findAttribute(this).lt(value)
inline infix fun <reified T : Any, R> KProperty1<T, R>.gt(value: Expression<R>): Logical<out Expression<R>, out Expression<R>> = findAttribute(this).gt(value)
inline infix fun <reified T : Any, R> KProperty1<T, R>.lte(value: Expression<R>): Logical<out Expression<R>, out Expression<R>> = findAttribute(this).lte(value)
inline infix fun <reified T : Any, R> KProperty1<T, R>.gte(value: Expression<R>): Logical<out Expression<R>, out Expression<R>> = findAttribute(this).gte(value)
inline infix fun <reified T : Any, R> KProperty1<T, R>.`in`(value: Collection<R>): Logical<out Expression<R>, out Collection<R>> = findAttribute(this).`in`(value)
inline infix fun <reified T : Any, R> KProperty1<T, R>.notIn(value: Collection<R>): Logical<out Expression<R>, out Collection<R>> = findAttribute(this).notIn(value)
inline infix fun <reified T : Any, R> KProperty1<T, R>.`in`(query: Return<*>): Logical<out Expression<R>, out Return<*>> = findAttribute(this).`in`(query)
inline infix fun <reified T : Any, R> KProperty1<T, R>.notIn(query: Return<*>): Logical<out Expression<R>, out Return<*>> = findAttribute(this).notIn(query)

inline fun <reified T : Any, R> KProperty1<T, R>.isNull(): Logical<out Expression<R>, out R> = findAttribute(this).isNull()
inline fun <reified T : Any, R> KProperty1<T, R>.notNull(): Logical<out Expression<R>, out R> = findAttribute(this).notNull()
inline fun <reified T : Any, R> KProperty1<T, R>.like(expression: String): Logical<out Expression<R>, out String> = findAttribute(this).like(expression)
inline fun <reified T : Any, R> KProperty1<T, R>.notLike(expression: String): Logical<out Expression<R>, out String> = findAttribute(this).notLike(expression)
inline fun <reified T : Any, R> KProperty1<T, R>.between(start: R, end: R): Logical<out Expression<R>, Any> = findAttribute(this).between(start, end)

/** Given a property find the corresponding generated attribute for it */
inline fun <reified T : Any, R> findAttribute(property: KProperty1<T, R>):
        AttributeDelegate<T, R> {
    val type: Type<*> = AttributeDelegate.types
            .filter { type -> (type.classType == T::class.java || type.baseType == T::class.java)}
            .first()
    val attribute: Attribute<*, *>? = type.attributes
            .filter { attribute ->
                attribute.propertyName.replaceFirst("get", "")
                        .equals(property.name, ignoreCase = true) }.firstOrNull()
    @Suppress("UNCHECKED_CAST")
    return attribute as AttributeDelegate<T, R>
}
