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

abstract class BaseAttribute<T, V> extends FieldExpression<V> implements
    QueryAttribute<T, V>, TypeDeclarable<T> {

    Property<?, V> builderProperty;
    Cardinality cardinality;
    Set<CascadeAction> cascadeActions;
    Class<V> classType;
    String collate;
    Converter<V, ?> converter;
    Type<T> declaringType;
    String defaultValue;
    String definition;
    ReferentialAction deleteAction;
    Class<?> elementClass;
    Set<String> indexNames;
    Initializer<T, V> initializer;
    boolean isForeignKey;
    boolean isKey;
    boolean isGenerated;
    boolean isIndex;
    boolean isLazy;
    boolean isNullable;
    boolean isUnique;
    boolean isVersion;
    Integer length;
    Class<?> mapKeyClass;
    Supplier<Attribute> mappedAttribute;
    String name;
    Supplier<Attribute> orderByAttribute;
    Order orderByDirection;
    PrimitiveKind primitiveKind;
    Property<T, V> property;
    String propertyName;
    Property<T, PropertyState> propertyState;
    Supplier<Attribute> referencedAttribute;
    Class<?> referencedClass;
    ReferentialAction updateAction;

    @Override
    public Property<?, V> getBuilderProperty() {
        return builderProperty;
    }

    @Override
    public Class<V> getClassType() {
        return classType;
    }

    @Override
    public Cardinality getCardinality() {
        return cardinality;
    }

    @Override
    public Set<CascadeAction> getCascadeActions() {
        return cascadeActions == null ? Collections.<CascadeAction>emptySet() : cascadeActions;
    }

    @Override
    public String getCollate() {
        return collate;
    }

    @Override
    public Converter<V, ?> getConverter() {
        return converter;
    }

    @Override
    public Type<T> getDeclaringType() {
        return declaringType;
    }

    @Override
    public String getDefaultValue() {
        return defaultValue;
    }

    @Override
    public String getDefinition() {
        return definition;
    }

    @Override
    public ReferentialAction getDeleteAction() {
        return deleteAction;
    }

    @Override
    public Class<?> getElementClass() {
        return elementClass;
    }

    @Override
    public ExpressionType getExpressionType() {
        return ExpressionType.ATTRIBUTE;
    }

    @Override
    public Set<String> getIndexNames() {
        return indexNames;
    }

    @Override
    public Initializer<T, V> getInitializer() {
        return initializer;
    }

    @Override
    public Integer getLength() {
        return converter != null ? converter.getPersistedSize() : length;
    }

    @Override
    public Class<?> getMapKeyClass() {
        return mapKeyClass;
    }

    @Override
    public Supplier<Attribute> getMappedAttribute() {
        return mappedAttribute;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Supplier<Attribute> getOrderByAttribute() {
        return orderByAttribute;
    }

    @Override
    public Order getOrderByDirection() {
        return orderByDirection;
    }

    @Override
    public PrimitiveKind getPrimitiveKind() {
        return primitiveKind;
    }

    @Override
    public Property<T, V> getProperty() {
        return property;
    }

    @Override
    public String getPropertyName() {
        return propertyName;
    }

    @Override
    public Property<T, PropertyState> getPropertyState() {
        return propertyState;
    }

    @Override
    public Supplier<Attribute> getReferencedAttribute() {
        return referencedAttribute;
    }

    @Override
    public Class<?> getReferencedClass() {
        return referencedClass;
    }

    @Override
    public ReferentialAction getUpdateAction() {
        return updateAction;
    }

    @Override
    public boolean isAssociation() {
        return cardinality != null;
    }

    @Override
    public boolean isForeignKey() {
        return isForeignKey;
    }

    @Override
    public boolean isGenerated() {
        return isGenerated;
    }

    @Override
    public boolean isIndexed() {
        return isIndex;
    }

    @Override
    public boolean isKey() {
        return isKey;
    }

    @Override
    public boolean isLazy() {
        return isLazy;
    }

    @Override
    public boolean isNullable() {
        return isNullable;
    }

    @Override
    public boolean isUnique() {
        return isUnique;
    }

    @Override
    public boolean isVersion() {
        return isVersion;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Attribute) {
            Attribute attribute = (Attribute) obj;
            return Objects.equals(name, attribute.getName()) &&
                Objects.equals(classType, attribute.getClassType()) &&
                Objects.equals(declaringType, attribute.getDeclaringType());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, classType, declaringType);
    }

    @Override
    public String toString() {
        return getDeclaringType() == null ?
               getName() : getDeclaringType().getName() + "." + getName();
    }

    @Override
    public void setDeclaringType(Type<T> type) {
        declaringType = type;
    }
}
