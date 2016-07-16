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
import io.requery.query.FieldExpression;
import io.requery.query.ExpressionType;
import io.requery.query.NamedExpression;
import io.requery.util.Objects;

public abstract class Function<V> extends FieldExpression<V> {

    private final String name;
    private final Class<V> type;
    private String alias;

    public Function(String name, Class<V> type) {
        this.name = name;
        this.type = type;
    }

    @Override
    public ExpressionType getExpressionType() {
        return ExpressionType.FUNCTION;
    }

    @Override
    public Function<V> as(String alias) {
        this.alias = alias;
        return this;
    }

    @Override
    public String getAlias() {
        return alias;
    }

    @Override
    public Class<V> getClassType() {
        return type;
    }

    @Override
    public String getName() {
        return name;
    }

    public abstract Object[] arguments();

    public Expression<?> expressionForArgument(int i) {
        Object value = arguments()[i];
        if (value instanceof Expression) {
            return (Expression<?>) value;
        } else {
            if (value == null) {
                return NamedExpression.of("null", type);
            } else {
                return new ArgumentExpression<>(value.getClass());
            }
        }
    }

    @Override
    public boolean equals(Object obj) {
        if(obj == this) {
            return true;
        }
        if(obj instanceof Function) {
            Function other = (Function) obj;
            return Objects.equals(getName(), other.getName()) &&
                   Objects.equals(getClassType(), other.getClassType()) &&
                   Objects.equals(getAlias(), other.getAlias()) &&
                   Objects.equals(arguments(), other.arguments());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(getName(), getClassType(), getAlias(), arguments());
    }

    private static class ArgumentExpression<X> implements Expression<X> {

        private final Class<X> type;

        ArgumentExpression(Class<X> type) {
            this.type = type;
        }

        @Override
        public String getName() {
            return "";
        }

        @Override
        public Class<X> getClassType() {
            return type;
        }

        @Override
        public ExpressionType getExpressionType() {
            return ExpressionType.FUNCTION;
        }
    }
}
