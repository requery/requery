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

import io.requery.query.FieldExpression;
import io.requery.query.ExpressionType;
import io.requery.util.Objects;

public abstract class Function<V> extends FieldExpression<V> {

    private final String name;
    private final Class<V> type;
    private String alias;

    protected Function(String name, Class<V> type) {
        this.name = name;
        this.type = type;
    }

    @Override
    public ExpressionType type() {
        return ExpressionType.FUNCTION;
    }

    @Override
    public Function<V> as(String alias) {
        this.alias = alias;
        return this;
    }

    @Override
    public String aliasName() {
        return alias;
    }

    @Override
    public Class<V> classType() {
        return type;
    }

    @Override
    public String name() {
        return name;
    }

    public abstract Object[] arguments();

    @Override
    public boolean equals(Object obj) {
        if(obj == this) {
            return true;
        }
        if(obj instanceof Function) {
            Function other = (Function) obj;
            return Objects.equals(name(), other.name()) &&
                   Objects.equals(classType(), other.classType()) &&
                   Objects.equals(aliasName(), other.aliasName()) &&
                   Objects.equals(arguments(), other.arguments());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name(), classType(), aliasName(), arguments());
    }
}
