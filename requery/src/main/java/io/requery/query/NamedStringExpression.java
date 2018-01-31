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

package io.requery.query;

import io.requery.query.function.Lower;
import io.requery.query.function.Substr;
import io.requery.query.function.Trim;
import io.requery.query.function.Upper;

public class NamedStringExpression extends NamedExpression<String> implements StringExpression<String> {
    public NamedStringExpression(String name) {
        super(name, String.class);
    }

    @Override
    public LogicalCondition<? extends Expression<String>, ? extends Expression<String>>
    equalsIgnoreCase(CharSequence string) {
        return Upper.upper(this).eq(NamedExpression.ofString(string.toString()).upper());
    }

    @Override
    public Trim<String> trim(String chars) {
        return Trim.trim(this, chars);
    }

    @Override
    public Trim<String> trim() {
        return trim(null);
    }

    @Override
    public Substr<String> substr(int offset, int length) {
        return Substr.substr(this, offset, length);
    }

    @Override
    public Upper<String> upper() {
        return Upper.upper(this);
    }

    @Override
    public Lower<String> lower() {
        return Lower.lower(this);
    }
}
