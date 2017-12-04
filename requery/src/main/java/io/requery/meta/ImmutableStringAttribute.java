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

package io.requery.meta;

import io.requery.query.Expression;
import io.requery.query.LogicalCondition;
import io.requery.query.NamedExpression;
import io.requery.query.function.Lower;
import io.requery.query.function.Substr;
import io.requery.query.function.Trim;
import io.requery.query.function.Upper;

final class ImmutableStringAttribute<T, V> extends ImmutableAttribute<T, V> implements StringAttribute<T, V> {

    ImmutableStringAttribute(AttributeBuilder<T, V> builder) {
        super(builder);
    }

    @Override
    @SuppressWarnings("unchecked")
    public LogicalCondition<? extends Expression<V>, ? extends Expression<V>>
    equalsIgnoreCase(CharSequence string) {
        Expression<V> expression = (Expression<V>)NamedExpression.ofString(string.toString()).upper();
        return Upper.upper(this).eq(expression);
    }

    @Override
    public Trim<V> trim(String chars) {
        return Trim.trim(this, chars);
    }

    @Override
    public Trim<V> trim() {
        return trim(null);
    }

    @Override
    public Substr<V> substr(int offset, int length) {
        return Substr.substr(this, offset, length);
    }

    @Override
    public Upper<V> upper() {
        return Upper.upper(this);
    }

    @Override
    public Lower<V> lower() {
        return Lower.lower(this);
    }
}
