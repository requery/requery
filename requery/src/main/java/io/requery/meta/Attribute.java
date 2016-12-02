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
import io.requery.util.function.Supplier;

import java.util.Set;

/**
 * Represents information about an attribute on a specific {@link Type}.
 *
 * @param <T> entity type
 * @param <V> value type
 *
 * @author Nikhil Purushe
 */
public interface Attribute<T, V> {

    /**
     * @return {@link Property} used to apply the attribute a builder instance for the type.
     */
    Property<?, V> getBuilderProperty();

    /**
     * @return the type of {@link Class} this attribute holds.
     */
    Class<V> getClassType();

    /**
     * @return the collation for the attribute, defaults to null
     */
    String getCollate();

    /**
     * @return For a {@link #isAssociation()} true attribute the type of relation being represented.
     */
    Cardinality getCardinality();

    /**
     * @return For an associative attribute the action to take when the association is modified.
     */
    Set<CascadeAction> getCascadeActions();

    /**
     * @return the converter instance used to convert the {@link #getClassType()} to a persisted value
     * and vice versa.
     */
    Converter<V, ?> getConverter();

    /**
     * @return The {@link Type} that contains this attribute.
     */
    Type<T> getDeclaringType();

    /**
     * @return the default value expression for the column. Used during the table generation phase.
     */
    String getDefaultValue();

    /**
     * @return optional table column definition {@link io.requery.Column#definition()}
     */
    String getDefinition();

    /**
     * @return For a {@link #isForeignKey()} attribute the action to take when the referenced entity
     * is deleted, otherwise null.
     */
    ReferentialAction getDeleteAction();

    /**
     * @return For a collection type the class of element in the collection.
     */
    Class<?> getElementClass();

    /**
     * @return for an {@link #isIndexed()} true attribute the index name(s), defaults to empty
     * (auto created).
     */
    Set<String> getIndexNames();

    /**
     * @return {@link Initializer} for defining the default value for the property representing the
     * attribute.
     */
    Initializer<T, V> getInitializer();

    /**
     * @return for String types the max length of the field in the persistence store or null.
     */
    Integer getLength();

    /**
     * @return For a map type the class of the key.
     */
    Class<?> getMapKeyClass();

    /**
     * @return For a associative relationship the attribute that is the owning side of the
     * relationship. Note returns a provider since attributes may have cyclic dependencies.
     */
    Supplier<Attribute> getMappedAttribute();

    /**
     * @return the type name, not this can be different from the member/field name.
     */
    String getName();

    /**
     * @return {@link PrimitiveKind} of this attribute if this attribute is representing a
     * primitive field, returns null otherwise if the field is not primitive (including boxed
     * versions of types)
     */
    PrimitiveKind getPrimitiveKind();

    /**
     * @return For a relationship with a multiple result query the attribute used to order the
     * query.
     */
    Supplier<Attribute> getOrderByAttribute();

    /**
     * @return If {@link #getOrderByAttribute()} is specified the order direction to use in the
     * generated query.
     */
    Order getOrderByDirection();

    /**
     * @return {@link Property} representing access to the field of the entity.
     */
    Property<T, V> getProperty();

    /**
     * @return Name of the source field or method that this attribute is mapped to.
     */
    String getPropertyName();

    /**
     * @return {@link Property} representing access to the state of the held property.
     */
    Property<T, PropertyState> getPropertyState();

    /**
     * @return For a foreign key relationship the attribute being referenced. Note returns a
     * provider since attributes may have cyclic dependencies.
     */
    Supplier<Attribute> getReferencedAttribute();

    /**
     * @return For a foreign key relationship the type being referenced.
     */
    Class<?> getReferencedClass();

    /**
     * @return For a {@link #isForeignKey()} attribute the action to take when the referenced entity
     * is updated, otherwise null.
     */
    ReferentialAction getUpdateAction();

    /**
     * @return true if this attribute is a link to another entity.
     */
    boolean isAssociation();

    /**
     * @return true if this attribute represents a foreign key constraint.
     */
    boolean isForeignKey();

    /**
     * @return true if this attribute is generated from the persistent store.
     */
    boolean isGenerated();

    /**
     * @return true if this column is indexed.
     */
    boolean isIndexed();

    /**
     * @return true if this attribute is the identity for the type represented, false otherwise.
     */
    boolean isKey();

    /**
     * @return true if this attribute is lazily loaded, false otherwise.
     */
    boolean isLazy();

    /**
     * @return true if this attribute is not required to be present in the type, (nullable)
     */
    boolean isNullable();

    /**
     * @return true if this attribute is unique for the given {@link Type}.
     */
    boolean isUnique();

    /**
     * @return true if this attribute is to be used a locking field.
     */
    boolean isVersion();
}
