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

package io.requery.sql;

import io.requery.query.Expression;
import io.requery.query.NamedExpression;
import io.requery.util.Objects;

import java.util.ArrayList;

/**
 * Represents the set of bound parameters for a SQL prepared statement.
 *
 * @author Nikhil Purushe
 */
public class BoundParameters {

    private final ArrayList<Expression<?>> expressions;
    private final ArrayList<Object> values;

    public BoundParameters() {
        expressions = new ArrayList<>();
        values = new ArrayList<>();
    }

    public BoundParameters(Object... parameters) {
        this();
        int index = 0;
        for (Object parameter : parameters) {
            Class type = parameter == null ? Object.class : parameter.getClass();
            Expression expression = NamedExpression.of(String.valueOf(index++), type);
            add(expression, parameter);
        }
    }

    public <V> void add(Expression<V> expression, V value) {
        expressions.add(expression);
        values.add(value);
    }

    Expression<?> expressionAt(int index) {
        return expressions.get(index);
    }

    Object valueAt(int index) {
        return values.get(index);
    }

    public int count() {
        return expressions.size();
    }

    public boolean isEmpty() {
        return count() == 0;
    }

    public void addAll(BoundParameters parameters) {
        expressions.addAll(parameters.expressions);
        values.addAll(parameters.values);
    }

    public void clear() {
        expressions.clear();
        values.clear();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof BoundParameters) {
            BoundParameters parameters = (BoundParameters) obj;
            return Objects.equals(values, parameters.values);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(values);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < values.size(); i++) {
            Object value = valueAt(i);
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(String.valueOf(value));
        }
        sb.append("]");
        return sb.toString();
    }
}
