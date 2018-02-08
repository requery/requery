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

import io.requery.query.function.Function;
import io.requery.query.function.Max;
import io.requery.query.function.Min;

public interface Functional<V> {

    OrderingExpression<V> asc();

    OrderingExpression<V> desc();

    Max<V> max();

    Min<V> min();

    Function<V> function(String name);
}
