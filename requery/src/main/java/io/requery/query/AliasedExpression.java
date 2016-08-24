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
        this(expression, expression.getName(), alias);
    }

    public AliasedExpression(Expression<V> expression, String name, String alias) {
        this.expression = expression;
        this.alias = alias;
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Class<V> getClassType() {
        return expression.getClassType();
    }

    @Override
    public ExpressionType getExpressionType() {
        return ExpressionType.ALIAS;
    }

    @Override
    public String getAlias() {
        return alias;
    }

    public Expression<V> getInnerExpression() {
        return expression;
    }
}
