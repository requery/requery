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

import java.util.Collection;

/**
 * Defines an expression of one or more expressions as a tuple, also called a row value expression.
 * Note this expression is not supported on all databases.
 *
 * Example:
 * <pre><code>
 *     RowExpression expression = RowExpression.of(Arrays.asList(Person.ID, Group.ID));
 *     Tuple result = data.select(Person.ID, Group.ID)
 *     .where(expression.in(Arrays.asList(1, 1), Arrays.asList(1, 2))).get().first();
 * </code></pre>
 */
public class RowExpression extends FieldExpression<Collection<?>> {

    private Collection<? extends Expression<?>> expressions;

    public static RowExpression of(Collection<? extends Expression<?>> expressions) {
        return new RowExpression(expressions);
    }

    private RowExpression(Collection<? extends Expression<?>> expressions) {
        this.expressions = expressions;
    }

    @Override
    public String getName() {
        StringBuilder sb = new StringBuilder();
        sb.append("(");
        int i = 0;
        for (Object o : expressions) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(o);
            i++;
        }
        sb.append(")");
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    @Override
    public Class<Collection<?>> getClassType() {
        return (Class<Collection<?>>) expressions.getClass();
    }

    @Override
    public ExpressionType getExpressionType() {
        return ExpressionType.ROW;
    }

    public Collection<? extends Expression<?>> getExpressions() {
        return expressions;
    }
}
