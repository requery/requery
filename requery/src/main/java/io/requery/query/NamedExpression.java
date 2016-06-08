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

public class NamedExpression<V> extends FieldExpression<V> {

    private final String name;
    private final Class<V> type;

    public static <V> NamedExpression<V> of(String name, Class<V> type) {
        return new NamedExpression<>(name, type);
    }

    public static  NamedExpression<Integer> ofInteger(String name) {
        return new NamedExpression<>(name, Integer.class);
    }

    public static  NamedExpression<String> ofString(String name) {
        return new NamedExpression<>(name, String.class);
    }

    private NamedExpression(String name, Class<V> type) {
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
}
