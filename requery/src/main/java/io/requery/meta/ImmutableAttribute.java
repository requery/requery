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
        builderProperty = builder.getBuilderProperty();
        cardinality = builder.getCardinality();
        cascadeActions = builder.getCascadeActions();
        classType = builder.getClassType();
        collate = builder.getCollate();
        converter = builder.getConverter();
        defaultValue = builder.getDefaultValue();
        definition = builder.getDefinition();
        deleteAction = builder.getDeleteAction();
        elementClass = builder.getElementClass();
        indexNames = builder.getIndexNames();
        initializer = builder.getInitializer();
        isForeignKey = builder.isForeignKey();
        isGenerated = builder.isGenerated();
        isIndex = builder.isIndexed();
        isKey = builder.isKey();
        isLazy = builder.isLazy();
        isNullable = builder.isNullable();
        isUnique = builder.isUnique();
        isVersion = builder.isVersion();
        length = builder.getLength();
        mapKeyClass = builder.getMapKeyClass();
        mappedAttribute = builder.getMappedAttribute();
        name = builder.getName();
        orderByAttribute = builder.getOrderByAttribute();
        orderByDirection = builder.getOrderByDirection();
        primitiveKind = builder.getPrimitiveKind();
        property = builder.getProperty();
        propertyName = builder.getPropertyName();
        propertyState = builder.getPropertyState();
        referencedAttribute = builder.getReferencedAttribute();
        referencedClass = builder.getReferencedClass();
        updateAction = builder.getUpdateAction();
    }
}
