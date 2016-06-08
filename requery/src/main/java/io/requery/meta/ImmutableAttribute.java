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

final class ImmutableAttribute<T, V> extends BaseAttribute<T, V> {

    ImmutableAttribute(AttributeBuilder<T, V> builder) {
        name = builder.getName();
        classType = builder.getClassType();
        primitiveKind = builder.getPrimitiveKind();
        property = builder.getProperty();
        propertyName = builder.getPropertyName();
        propertyState = builder.getPropertyState();
        initializer = builder.getInitializer();
        builderProperty = builder.getBuilderProperty();
        isLazy = builder.isLazy();
        length = builder.getLength();
        isKey = builder.isKey();
        isUnique = builder.isUnique();
        isGenerated = builder.isGenerated();
        isNullable = builder.isNullable();
        isVersion = builder.isVersion();
        isForeignKey = builder.isForeignKey();
        isIndex = builder.isIndexed();
        defaultValue = builder.getDefaultValue();
        collate = builder.getCollate();
        indexNames = builder.getIndexNames();
        cardinality = builder.getCardinality();
        deleteAction = builder.getDeleteAction();
        updateAction = builder.getUpdateAction();
        cascadeActions = builder.getCascadeActions();
        referencedAttribute = builder.getReferencedAttribute();
        referencedClass = builder.getReferencedClass();
        mapKeyClass = builder.getMapKeyClass();
        elementClass = builder.getElementClass();
        isForeignKey = builder.isForeignKey();
        converter = builder.getConverter();
        mappedAttribute = builder.getMappedAttribute();
        orderByAttribute = builder.getOrderByAttribute();
        orderByDirection = builder.getOrderByDirection();
    }
}
