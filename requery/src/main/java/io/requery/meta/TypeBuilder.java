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

package io.requery.meta;

import io.requery.proxy.EntityProxy;
import io.requery.util.Objects;
import io.requery.util.function.Function;
import io.requery.util.function.Supplier;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.TreeSet;

public class TypeBuilder<T> extends BaseType<T> {

    public TypeBuilder(Class<T> type, String name) {
        Objects.requireNotNull(type);
        this.type = type;
        // place the ids at the beginning, then the rest alphabetically
        this.attributes = new TreeSet<>(new Comparator<Attribute<T, ?>>() {
            @Override
            public int compare(Attribute<T, ?> o1, Attribute<T, ?> o2) {
                if (o1.isKey()) {
                    return -1;
                }
                if (o2.isKey()) {
                    return 1;
                }
                return o1.getName().compareTo(o2.getName());
            }
        });
        this.name = name;
        this.referencedTypes = new LinkedHashSet<>();
        this.expressions = new LinkedHashSet<>();
    }

    public TypeBuilder<T> setBaseType(Class<? super T> type) {
        this.baseType = type;
        return this;
    }

    public TypeBuilder<T> setCacheable(boolean cacheable) {
        this.cacheable = cacheable;
        return this;
    }

    public TypeBuilder<T> setImmutable(boolean immutable) {
        this.immutable = immutable;
        return this;
    }

    public TypeBuilder<T> setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
        return this;
    }

    public TypeBuilder<T> setStateless(boolean stateless) {
        this.stateless = stateless;
        return this;
    }

    public TypeBuilder<T> setView(boolean isView) {
        this.isView = isView;
        return this;
    }

    public TypeBuilder<T> setFactory(Supplier<T> factory) {
        this.factory = factory;
        return this;
    }

    public TypeBuilder<T> setProxyProvider(Function<T, EntityProxy<T>> provider) {
        this.proxyProvider = provider;
        return this;
    }

    public <B> TypeBuilder<T> setBuilderFactory(Supplier<B> factory) {
        this.builderFactory = factory;
        return this;
    }

    public <B> TypeBuilder<T> setBuilderFunction(Function<B, T> function) {
        this.buildFunction = function;
        return this;
    }

    public TypeBuilder<T> setTableCreateAttributes(String[] attributes) {
        this.tableCreateAttributes = attributes;
        return this;
    }

    public TypeBuilder<T> setTableUniqueIndexes(String[] indexes) {
        this.tableUniqueIndexes = indexes;
        return this;
    }

    public TypeBuilder<T> addAttribute(Attribute<T, ?> attribute) {
        this.attributes.add(attribute);
        return this;
    }

    public TypeBuilder<T> addExpression(QueryExpression<?> expression) {
        this.expressions.add(expression);
        return this;
    }

    public Type<T> build() {
        return new ImmutableType<>(this);
    }
}
