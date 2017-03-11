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

package io.requery.processor;

import io.requery.CascadeAction;
import io.requery.ReferentialAction;
import io.requery.meta.AttributeBuilder;
import io.requery.meta.Cardinality;
import io.requery.query.Order;

import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import java.util.Optional;
import java.util.Set;

/**
 * Defines a persistable attribute of an {@link EntityDescriptor} to be processed.
 *
 * @author Nikhil Purushe
 */
interface AttributeDescriptor {

    /**
     * @return {@link TypeElement} element being represented
     */
    Element element();

    /**
     * @return {@link TypeMirror} of the type this attribute references
     */
    TypeMirror typeMirror();

    /**
     * @return name of the attribute used in the mapping operations
     */
    String name();

    /**
     * @return the type of builder used to construct the attribute in the generated code
     */
    Class<? extends AttributeBuilder> builderClass();

    /**
     * @return the collation string for the column.
     */
    String collate();

    /**
     * @return optional column length for this attribute
     */
    Integer columnLength();

    /**
     * @return class name of the converter for this attribute
     */
    String converterName();

    /**
     * @return optional default value string for this attribute used during table generation.
     */
    String defaultValue();

    /**
     * @return optional column definition used during table generation
     */
    String definition();

    /**
     * @return name of the generated field for this attribute used during code generation.
     */
    String fieldName();

    /**
     * @return optional if this column is indexed the name(s) of the indexes
     */
    Set<String> indexNames();

    /**
     * @return name of the getter accessor method used to access the backing field
     */
    String getterName();

    /**
     * @return name of the setter mutator method used to access the backing field
     */
    String setterName();

    /**
     * @return true if this is a foreign key column, false otherwise
     */
    boolean isForeignKey();

    /**
     * @return true if this a generated column, false otherwise
     */
    boolean isGenerated();

    /**
     * @return true if this a key column in the table being mapped
     */
    boolean isKey();

    /**
     * @return true if this attribute should be loaded lazily, false otherwise
     */
    boolean isLazy();

    /**
     * @return true if this attribute is representing an embedded type
     */
    boolean isEmbedded();

    /**
     * @return true if this column is indexed
     */
    boolean isIndexed();

    /**
     * @return true if this a {@link Iterable} collection type
     */
    boolean isIterable();

    /**
     * @return true if this a {@link java.util.Map} collection type
     */
    boolean isMap();

    /**
     * @return true if this column can hold null values, false otherwise
     */
    boolean isNullable();

    /**
     * @return true if this attribute is accessed through a {@link Optional} method.
     */
    boolean isOptional();

    /**
     * @return true if this attribute is read only and not modifiable
     */
    boolean isReadOnly();

    /**
     * @return true if this a transient method/field and will not be persisted
     */
    boolean isTransient();

    /**
     * @return true if this column has a unique constraint
     */
    boolean isUnique();

    /**
     * @return true if this column is used as the version column for optimistic locking
     */
    boolean isVersion();

    /**
     * @return the cardinality of this attribute if it is an association
     */
    Cardinality cardinality();

    /**
     * @return {@link AssociativeEntityDescriptor} for the attribute if it maps a many-to-many
     * relationship
     */
    Optional<AssociativeEntityDescriptor> associativeEntity();

    /**
     * @return for a foreign key action the delete {@link ReferentialAction} to take for the key
     */
    ReferentialAction deleteAction();

    /**
     * @return for a foreign key action the update {@link ReferentialAction} to take for the key
     */
    ReferentialAction updateAction();

    /**
     * @return for associative attributes the set of {@link CascadeAction} actions to take during
     * create/update/delete operations
     */
    Set<CascadeAction> cascadeActions();

    /**
     * @return name of the field in the associated attribute in the referenced entity this maps to
     * for associative attributes
     */
    String mappedBy();

    /**
     * @return optional in a relational attribute the column use to order the generated query by.
     */
    String orderBy();

    /**
     * @return optional if the order by attribute is provided the sort order for the query.
     */
    Order orderByDirection();

    /**
     * @return for associative attributes the column being referenced.
     */
    String referencedColumn();

    /**
     * @return for associative attributes the type being referenced.
     */
    String referencedType();

    /**
     * @return for associative attributes the table being referenced.
     */
    String referencedTable();
}
