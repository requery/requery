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

import io.requery.util.function.Supplier;

import java.util.Collections;
import java.util.LinkedHashSet;

final class ImmutableType<T> extends BaseType<T> {

    ImmutableType(TypeBuilder<T> builder) {
        this.type = builder.getClassType();
        this.baseType = builder.getBaseType();
        this.name = builder.getName();
        this.cacheable = builder.isCacheable();
        this.readOnly = builder.isReadOnly();
        this.immutable = builder.isImmutable();
        this.isView = builder.isView();
        this.stateless = builder.isStateless();
        this.factory = builder.getFactory();
        this.proxyProvider = builder.getProxyProvider();
        this.tableCreateAttributes = builder.getTableCreateAttributes();
        this.tableUniqueIndexes = builder.getTableUniqueIndexes();
        this.builderFactory = builder.getBuilderFactory();
        this.buildFunction = builder.getBuildFunction();

        LinkedHashSet<Attribute<T, ?>> attributes = new LinkedHashSet<>();
        LinkedHashSet<Attribute<T, ?>> keyAttributes = new LinkedHashSet<>();
        for (Attribute<T, ?> attribute : builder.getAttributes()) {
            setDeclaringType(attribute);
            attributes.add(attribute);
            if (attribute.isKey()) {
                keyAttributes.add(attribute);
            }
        }
        this.attributes = Collections.unmodifiableSet(attributes);
        this.keyAttributes = Collections.unmodifiableSet(keyAttributes);
        if (keyAttributes.size() == 1) {
            keyAttribute = keyAttributes.iterator().next();
        }
        this.expressions = Collections.unmodifiableSet(builder.expressions);
        for (QueryExpression<?> expression : builder.expressions) {
            setDeclaringType(expression);
        }
        if (factory == null) {
            // factory will be set by the processor this is a fallback
            factory = new Supplier<T>() {
                @Override
                public T get() {
                    try {
                        return getClassType().newInstance();
                    } catch (InstantiationException | IllegalAccessException e) {
                        throw new RuntimeException(e);
                    }
                }
            };
        }
    }

    private void setDeclaringType(Object expression) {
        if (expression instanceof TypeDeclarable) {
            // cheating here a bit but needed to avoid circular references, will be
            // effectively immutable after the type is constructed
            @SuppressWarnings("unchecked")
            TypeDeclarable<T> declarable = (TypeDeclarable<T>) expression;
            declarable.setDeclaringType(this);
        } else {
            throw new UnsupportedOperationException();
        }
    }
}
