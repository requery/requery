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

import io.requery.query.function.Abs;
import io.requery.query.function.Avg;
import io.requery.query.function.Max;
import io.requery.query.function.Min;
import io.requery.query.function.Round;
import io.requery.query.function.Sum;

final class ImmutableNumericAttribute<T, V> extends ImmutableAttribute<T, V> implements NumericAttribute<T, V> {

    ImmutableNumericAttribute(AttributeBuilder<T, V> builder) {
        super(builder);
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
