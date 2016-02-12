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

public class AliasedExpression<V> extends FieldExpression<V> {

    private final Expression<V> expression;
    private final String alias;
    private final String name;

    public AliasedExpression(Expression<V> expression, String alias) {
        this.expression = expression;
        this.alias = alias;
        this.name = expression.name();
    }

    public AliasedExpression(Expression<V> expression, String name, String alias) {
        this.expression = expression;
        this.alias = alias;
        this.name = name;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public Class<V> classType() {
        return expression.classType();
    }

    @Override
    public ExpressionType type() {
        return ExpressionType.ALIAS;
    }

    @Override
    public String aliasName() {
        return alias;
    }

    public Expression<V> innerExpression() {
        return expression;
    }
}
