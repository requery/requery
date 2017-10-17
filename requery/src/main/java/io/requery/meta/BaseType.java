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

package io.requery.meta;

import io.requery.proxy.EntityProxy;
import io.requery.query.Expression;
import io.requery.query.ExpressionType;
import io.requery.util.Objects;
import io.requery.util.function.Function;
import io.requery.util.function.Supplier;

import java.util.LinkedHashSet;
import java.util.Set;

abstract class BaseType<T> implements Type<T> {

    Class<T> type;
    Class<? super T> baseType;
    Type<?> superType;
    String name;
    boolean cacheable;
    boolean stateless;
    boolean readOnly;
    boolean immutable;
    boolean isView;
    Set<Attribute<T, ?>> attributes;
    Set<QueryExpression<?>> expressions;
    Supplier<T> factory;
    Function<T, EntityProxy<T>> proxyProvider;
    Set<Class<?>> referencedTypes;
    String[] tableCreateAttributes;
    String[] tableUniqueIndexes;
    Supplier<?> builderFactory;
    Function<?, T> buildFunction;
    Set<Attribute<T, ?>> keyAttributes;
    Attribute<T, ?> keyAttribute;

    BaseType() {
        cacheable = true;
        referencedTypes = new LinkedHashSet<>();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Class<T> getClassType() {
        return type;
    }

    @Override
    public Class<? super T> getBaseType() {
        return baseType;
    }

    @Override
    public ExpressionType getExpressionType() {
        return ExpressionType.NAME;
    }

    @Override
    public Expression<T> getInnerExpression() {
        return null;
    }

    @Override
    public boolean isBuildable() {
        return builderFactory != null;
    }

    @Override
    public boolean isCacheable() {
        return cacheable;
    }

    @Override
    public boolean isImmutable() {
        return immutable;
    }

    @Override
    public boolean isReadOnly() {
        return readOnly;
    }

    @Override
    public boolean isStateless() {
        return stateless;
    }

    @Override
    public boolean isView() {
        return isView;
    }

    @Override
    public Set<Attribute<T, ?>> getAttributes() {
        return attributes;
    }

    @Override
    public Set<Attribute<T, ?>> getKeyAttributes() {
        return keyAttributes;
    }

    @Override
    public Attribute<T, ?> getSingleKeyAttribute() {
        return keyAttribute;
    }

    @Override
    public Set<QueryExpression<?>> getQueryExpressions() {
        return expressions;
    }

    @Override
    public <B> Supplier<B> getBuilderFactory() {
        @SuppressWarnings("unchecked")
        Supplier<B> supplier = (Supplier<B>) builderFactory;
        return supplier;
    }

    @Override
    public <B> Function<B, T> getBuildFunction() {
        @SuppressWarnings("unchecked")
        Function<B, T> function = (Function<B, T>) buildFunction;
        return function;
    }

    @Override
    public Supplier<T> getFactory() {
        return factory;
    }

    @Override
    public Function<T, EntityProxy<T>> getProxyProvider() {
        return proxyProvider;
    }

    @Override
    public String[] getTableCreateAttributes() {
        return tableCreateAttributes;
    }

    @Override
    public String[] getTableUniqueIndexes() {
        return tableUniqueIndexes;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Type) {
            Type other = (Type) obj;
            return Objects.equals(getClassType(), other.getClassType()) &&
                   Objects.equals(getName(), other.getName());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, type);
    }

    @Override
    public String toString() {
        return "classType: " + type.toString() +
            " name: " + name +
            " readonly: " + readOnly +
            " immutable: " + immutable +
            " stateless: " + stateless +
            " cacheable: " + cacheable;
    }
}
