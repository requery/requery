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
     * @return the type name, not this can be different from the member/field name.
     */
    String name();

    /**
     * @return the type of {@link Class} this attribute holds.
     */
    Class<V> classType();

    /**
     * @return The {@link Type} that contains this attribute.
     */
    Type<T> declaringType();

    /**
     * @return {@link PrimitiveKind} of this attribute if this attribute is representing a
     * primitive field, returns null otherwise if the field is not primitive (including boxed
     * versions of types)
     */
    PrimitiveKind primitiveKind();

    /**
     * @return {@link Initializer} for defining the default value for the property representing the
     * attribute.
     */
    Initializer<T, V> initializer();

    /**
     * @return {@link Property} representing access to the field of the entity.
     */
    Property<T, V> property();

    /**
     * @return {@link Property} representing access to the state of the held property.
     */
    Property<T, PropertyState> propertyState();

    /**
     * @return {@link Property} used to apply the attribute a builder instance for the type.
     */
    Property<?, V> builderProperty();

    /**
     * @return true if this attribute is lazily loaded, false otherwise.
     */
    boolean isLazy();

    /**
     * @return true if this attribute is a link to another entity.
     */
    boolean isAssociation();

    /**
     * @return true if this attribute is the identity for the type represented, false otherwise.
     */
    boolean isKey();

    /**
     * @return true if this attribute is unique for the given {@link Type}.
     */
    boolean isUnique();

    /**
     * @return true if this attribute is generated from the persistent store.
     */
    boolean isGenerated();

    /**
     * @return true if this attribute is not required to be present in the type, (nullable)
     */
    boolean isNullable();

    /**
     * @return true if this attribute represents a foreign key constraint.
     */
    boolean isForeignKey();

    /**
     * @return true if this attribute is to be used a locking field.
     */
    boolean isVersion();

    /**
     * @return true if this column is indexed.
     */
    boolean isIndexed();

    /**
     * @return the default value expression for the column. Used during the table generation phase.
     */
    String defaultValue();

    /**
     * @return for String types the max length of the field in the persistence store or null.
     */
    Integer length();

    /**
     * @return for an {@link #isIndexed()} true attribute the index name, defaults to empty
     * (auto created).
     */
    String indexName();

    /**
     * @return the collation for the attribute, defaults to null
     */
    String collate();

    /**
     * @return For a collection type the class of element in the collection.
     */
    Class<?> elementClass();

    /**
     * @return For a map type the class of the key.
     */
    Class<?> mapKeyClass();

    /**
     * @return For a foreign key relationship the type being referenced.
     */
    Class<?> referencedClass();

    /**
     * @return For a {@link #isAssociation()} true attribute the type of relation being represented.
     */
    Cardinality cardinality();

    /**
     * @return For a {@link #isForeignKey()} attribute the action to take when the referenced entity
     * is deleted, otherwise null.
     */
    ReferentialAction deleteAction();

    /**
     * @return For a {@link #isForeignKey()} attribute the action to take when the referenced entity
     * is updated, otherwise null.
     */
    ReferentialAction updateAction();

    /**
     * @return For an associative attribute the action to take when the association is modified.
     */
    Set<CascadeAction> cascadeActions();

    /**
     * @return the converter instance used to convert the {@link #classType()} to a persisted value
     * and vice versa.
     */
    Converter<V, ?> converter();

    /**
     * @return For a associative relationship the attribute that is the owning side of the
     * relationship. Note returns a provider since attributes may have cyclic dependencies.
     */
    Supplier<Attribute> mappedAttribute();

    /**
     * @return For a foreign key relationship the attribute being referenced. Note returns a
     * provider since attributes may have cyclic dependencies.
     */
    Supplier<Attribute> referencedAttribute();
}
