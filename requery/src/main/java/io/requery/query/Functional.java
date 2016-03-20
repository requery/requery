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

package io.requery.query;

import io.requery.query.function.Avg;
import io.requery.query.function.Substr;
import io.requery.query.function.Trim;
import io.requery.query.function.Abs;
import io.requery.query.function.Lower;
import io.requery.query.function.Max;
import io.requery.query.function.Min;
import io.requery.query.function.Round;
import io.requery.query.function.Sum;
import io.requery.query.function.Upper;

public interface Functional<V> {

    OrderingExpression<V> asc();

    OrderingExpression<V> desc();

    Abs<V> abs();

    Max<V> max();

    Min<V> min();

    Avg<V> avg();

    Sum<V> sum();

    Round<V> round();

    Round<V> round(int decimals);

    Trim<V> trim(String chars);

    Trim<V> trim();

    Substr<V> substr(int offset, int length);

    Upper<V> upper();

    Lower<V> lower();
}
