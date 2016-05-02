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

import io.requery.CascadeAction;
import io.requery.Converter;
import io.requery.ReferentialAction;
import io.requery.proxy.Initializer;
import io.requery.proxy.Property;
import io.requery.proxy.PropertyState;
import io.requery.query.ExpressionType;
import io.requery.query.FieldExpression;
import io.requery.query.Order;
import io.requery.util.Objects;
import io.requery.util.function.Supplier;

import java.util.Collections;
import java.util.Set;

abstract class BaseAttribute<T, V> extends FieldExpression<V> implements QueryAttribute<T, V> {

    String name;
    Initializer<T, V> initializer;
    Class<V> classType;
    PrimitiveKind primitiveKind;
    Property<T, V> property;
    Property<?, V> builderProperty;
    Property<T, PropertyState> propertyState;
    boolean isLazy;
    boolean isKey;
    boolean isUnique;
    boolean isGenerated;
    boolean isNullable;
    boolean isVersion;
    boolean isForeignKey;
    boolean isIndex;
    Integer length;
    String defaultValue;
    Set<String> indexNames;
    String collate;
    Cardinality cardinality;
    ReferentialAction deleteAction;
    ReferentialAction updateAction;
    Set<CascadeAction> cascadeActions;
    Converter<V, ?> converter;
    Type<T> declaringType;
    Class<?> mapKeyClass;
    Class<?> elementClass;
    Class<?> referencedClass;
    Supplier<Attribute> mappedAttribute;
    Supplier<Attribute> referencedAttribute;
    Supplier<Attribute> orderByAttribute;
    Order orderByDirection;

    @Override
    public Initializer<T, V> initializer() {
        return initializer;
    }

    @Override
    public Property<T, V> property() {
        return property;
    }

    @Override
    public Property<T, PropertyState> propertyState() {
        return propertyState;
    }

    @Override
    public Property<?, V> builderProperty() {
        return builderProperty;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public Class<V> classType() {
        return classType;
    }

    @Override
    public PrimitiveKind primitiveKind() {
        return primitiveKind;
    }

    @Override
    public ExpressionType type() {
        return ExpressionType.ATTRIBUTE;
    }

    @Override
    public Type<T> declaringType() {
        return declaringType;
    }

    @Override
    public boolean isLazy() {
        return isLazy;
    }

    @Override
    public Integer length() {
        return converter != null ? converter.persistedSize() : length;
    }

    @Override
    public boolean isAssociation() {
        return cardinality != null;
    }

    @Override
    public boolean isKey() {
        return isKey;
    }

    @Override
    public boolean isUnique() {
        return isUnique;
    }

    @Override
    public boolean isGenerated() {
        return isGenerated;
    }

    @Override
    public boolean isNullable() {
        return isNullable;
    }

    @Override
    public boolean isForeignKey() {
        return isForeignKey;
    }

    @Override
    public boolean isVersion() {
        return isVersion;
    }

    @Override
    public boolean isIndexed() {
        return isIndex;
    }

    @Override
    public String defaultValue() {
        return defaultValue;
    }

    @Override
    public Set<String> indexNames() {
        return indexNames;
    }

    @Override
    public String collate() {
        return collate;
    }

    @Override
    public Class<?> mapKeyClass() {
        return mapKeyClass;
    }

    @Override
    public Class<?> elementClass() {
        return elementClass;
    }

    @Override
    public Class<?> referencedClass() {
        return referencedClass;
    }

    @Override
    public Cardinality cardinality() {
        return cardinality;
    }

    @Override
    public ReferentialAction deleteAction() {
        return deleteAction;
    }

    @Override
    public ReferentialAction updateAction() {
        return updateAction;
    }

    @Override
    public Set<CascadeAction> cascadeActions() {
        return cascadeActions == null ? Collections.<CascadeAction>emptySet() : cascadeActions;
    }

    @Override
    public Converter<V, ?> converter() {
        return converter;
    }

    @Override
    public Supplier<Attribute> mappedAttribute() {
        return mappedAttribute;
    }

    @Override
    public Supplier<Attribute> referencedAttribute() {
        return referencedAttribute;
    }

    @Override
    public Supplier<Attribute> orderByAttribute() {
        return orderByAttribute;
    }

    @Override
    public Order orderByDirection() {
        return orderByDirection;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Attribute) {
            Attribute attribute = (Attribute) obj;
            return Objects.equals(name, attribute.name()) &&
                Objects.equals(classType, attribute.classType()) &&
                Objects.equals(declaringType, attribute.declaringType());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, classType, declaringType);
    }

    @Override
    public String toString() {
        return declaringType() == null ?
                name() : declaringType().name() + "." + name();
    }
}
