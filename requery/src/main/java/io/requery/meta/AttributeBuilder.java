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
import io.requery.query.Order;
import io.requery.util.Objects;
import io.requery.util.function.Supplier;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashSet;

/**
 * Builds information about an attribute on a specific {@link Type}.
 *
 * @param <T> entity type
 * @param <V> value type
 *
 * @author Nikhil Purushe
 */
public class AttributeBuilder<T, V> extends BaseAttribute<T, V> {

    public AttributeBuilder(String name, Class<V> type) {
        this.name = Objects.requireNotNull(name);
        this.classType = Objects.requireNotNull(type);
        this.primitiveKind = PrimitiveKind.fromClass(classType);
    }

    public <B> AttributeBuilder<T, V> setBuilderProperty(Property<B, V> property) {
        this.builderProperty = property;
        return this;
    }

    public AttributeBuilder<T, V> setCardinality(Cardinality cardinality) {
        this.cardinality = cardinality;
        return this;
    }

    public AttributeBuilder<T, V> setCascadeAction(CascadeAction ...actions) {
        this.cascadeActions = EnumSet.copyOf(Arrays.asList(actions));
        return this;
    }

    public AttributeBuilder<T, V> setCollate(String collate) {
        this.collate = collate;
        return this;
    }

    public AttributeBuilder<T, V> setConverter(Converter<V, ?> converter) {
        this.converter = converter;
        return this;
    }

    public AttributeBuilder<T, V> setDefaultValue(String value) {
        this.defaultValue = value;
        return this;
    }

    public AttributeBuilder<T, V> setDefinition(String definition) {
        this.definition = definition;
        return this;
    }

    public AttributeBuilder<T, V> setDeleteAction(ReferentialAction action) {
        this.deleteAction = action;
        return this;
    }

    public AttributeBuilder<T, V> setForeignKey(boolean foreignKey) {
        this.isForeignKey = foreignKey;
        return this;
    }

    public AttributeBuilder<T, V> setGenerated(boolean generated) {
        this.isGenerated = generated;
        return this;
    }

    public AttributeBuilder<T, V> setIndexed(boolean indexed) {
        this.isIndex = indexed;
        return this;
    }

    public AttributeBuilder<T, V> setIndexNames(String... names) {
        this.indexNames = new LinkedHashSet<>();
        Collections.addAll(indexNames, names);
        return this;
    }

    public AttributeBuilder<T, V> setInitializer(Initializer<T, V> initializer) {
        this.initializer = initializer;
        return this;
    }

    public AttributeBuilder<T, V> setKey(boolean key) {
        this.isKey = key;
        return this;
    }

    public AttributeBuilder<T, V> setLazy(boolean lazy) {
        this.isLazy = lazy;
        return this;
    }

    public AttributeBuilder<T, V> setLength(Integer length) {
        this.length = length;
        return this;
    }

    public AttributeBuilder<T, V> setMappedAttribute(Supplier<Attribute> attribute) {
        this.mappedAttribute = attribute;
        return this;
    }

    public AttributeBuilder<T, V> setNullable(boolean nullable) {
        this.isNullable = nullable;
        return this;
    }

    public AttributeBuilder<T, V> setOrderByAttribute(Supplier<Attribute> attribute) {
        this.orderByAttribute = attribute;
        return this;
    }

    public AttributeBuilder<T, V> setOrderByDirection(Order order) {
        this.orderByDirection = order;
        return this;
    }

    public AttributeBuilder<T, V> setProperty(Property<T, V> property) {
        this.property = property;
        return this;
    }

    public AttributeBuilder<T, V> setPropertyName(String name) {
        this.propertyName = name;
        return this;
    }

    public AttributeBuilder<T, V> setPropertyState(Property<T, PropertyState> property) {
        this.propertyState = property;
        return this;
    }

    public AttributeBuilder<T, V> setReferencedAttribute(Supplier<Attribute> attribute) {
        this.referencedAttribute = attribute;
        return this;
    }

    public AttributeBuilder<T, V> setReferencedClass(Class<?> type) {
        this.referencedClass = type;
        return this;
    }

    public AttributeBuilder<T, V> setUnique(boolean unique) {
        this.isUnique = unique;
        return this;
    }

    public AttributeBuilder<T, V> setUpdateAction(ReferentialAction action) {
        this.updateAction = action;
        return this;
    }

    public AttributeBuilder<T, V> setVersion(boolean version) {
        this.isVersion = version;
        return this;
    }

    public QueryAttribute<T, V> build() {
        return new ImmutableAttribute<>(this);
    }
}
