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

package io.requery.query.function;

import io.requery.query.Expression;

public class Abs<V> extends Function<V> {

    private final Expression<V> attribute;

    private Abs(Expression<V> expression) {
        super("abs", expression.getClassType());
        this.attribute = expression;
    }

    public static <U> Abs<U> abs(Expression<U> attribute) {
        return new Abs<>(attribute);
    }

    @Override
    public Object[] arguments() {
        return new Object[] { attribute };
    }
}
