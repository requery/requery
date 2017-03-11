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

public class Substr<V> extends Function<V> {

    private final Expression<V> expression;
    private final int offset;
    private final int length;

    private Substr(Expression<V> expression, int offset, int length) {
        super("substr", expression.getClassType());
        this.expression = expression;
        this.offset = offset;
        this.length = length;
    }

    public static <U> Substr<U> substr(Expression<U> expression, int offset, int length) {
        return new Substr<>(expression, offset, length);
    }

    @Override
    public Object[] arguments() {
        return new Object[]{expression, offset, length};
    }
}
