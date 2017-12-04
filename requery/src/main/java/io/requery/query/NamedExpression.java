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

package io.requery.query;

import io.requery.query.function.Abs;
import io.requery.query.function.Avg;
import io.requery.query.function.Lower;
import io.requery.query.function.Max;
import io.requery.query.function.Min;
import io.requery.query.function.Round;
import io.requery.query.function.Substr;
import io.requery.query.function.Sum;
import io.requery.query.function.Trim;
import io.requery.query.function.Upper;

public class NamedExpression<V> extends FieldExpression<V> {

    private final String name;
    private final Class<V> type;

    public static <V> NamedExpression<V> of(String name, Class<V> type) {
        return new NamedExpression<>(name, type);
    }

    public static NamedNumericExpression<Integer> ofInteger(String name) {
        return new NamedNumericExpression<>(name, Integer.class);
    }

    public static NamedStringExpression ofString(String name) {
        return new NamedStringExpression("\'" + name + "\'");
    }

    protected NamedExpression(String name, Class<V> type) {
        this.name = name;
        this.type = type;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Class<V> getClassType() {
        return type;
    }

    @Override
    public ExpressionType getExpressionType() {
        return ExpressionType.NAME;
    }

    public static class NamedStringExpression extends NamedExpression<String> implements StringExpression<String> {
        NamedStringExpression(String name) {
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

    public static class NamedNumericExpression<V> extends NamedExpression<V> implements NumericExpression<V> {
        NamedNumericExpression(String name, Class<V> type) {
            super(name, type);
        }

        @Override
        public Abs<V> abs() {
            return Abs.abs(this);
        }

        @Override
        public Max<V> max() {
            return Max.max(this);
        }

        @Override
        public Min<V> min() {
            return Min.min(this);
        }

        @Override
        public Avg<V> avg() {
            return Avg.avg(this);
        }

        @Override
        public Sum<V> sum() {
            return Sum.sum(this);
        }

        @Override
        public Round<V> round() {
            return round(0);
        }

        @Override
        public Round<V> round(int decimals) {
            return Round.round(this, decimals);
        }
    }
}
