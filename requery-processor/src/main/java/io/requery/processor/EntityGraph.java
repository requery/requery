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

import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Holds entity types and navigates relationships between entity objects that will be generated.
 *
 * @author Nikhil Purushe
 */
class EntityGraph {

    private final Map<TypeElement, EntityDescriptor> entities;
    private final Map<TypeElement, ? extends EntityDescriptor> embeddedTypes;
    private final Types types;

    EntityGraph(Types types, Map<TypeElement, ? extends EntityDescriptor> embeddedTypes) {
        this.types = types;
        this.embeddedTypes = embeddedTypes;
        this.entities = new HashMap<>();
    }

    /**
     * @return the collection of {@link EntityDescriptor} descriptors in this graph.
     */
    Collection<EntityDescriptor> entities() {
        return Collections.unmodifiableCollection(entities.values());
    }

    /**
     * Add an {@link EntityDescriptor} to this graph.
     *
     * @param entity to add
     */
    public void add(EntityDescriptor entity) {
        entities.putIfAbsent(entity.element(), entity);
    }

    Optional<EntityDescriptor> embeddedDescriptorOf(AttributeDescriptor attribute) {
        TypeMirror mirror = attribute.typeMirror();
        Element element = types.asElement(mirror);
        if (element instanceof TypeElement) {
            return Optional.ofNullable(embeddedTypes.get(element));
        }
        return Optional.empty();
    }

    private Optional<EntityDescriptor> entityByName(QualifiedName name) {
        boolean ignorePackage = Names.isEmpty(name.packageName());
        for (EntityDescriptor entity : entities.values()) {
            QualifiedName entityName = entity.typeName();
            if ((ignorePackage && entityName.className().equals(name.className())) ||
                entityName.equals(name) || match(entity, name.className())) {
                return Optional.of(entity);
            }
        }
        return Optional.empty();
    }

    /**
     * Given a association attribute in an Entity find the Entity the attribute is referencing.
     *
     * @param attribute association attribute
     * @return Optional Entity type being referenced.
     */
    Optional<EntityDescriptor> referencingEntity(AttributeDescriptor attribute) {
        if (!Names.isEmpty(attribute.referencedTable())) {
            // match by table name
            return entities.values().stream()
                .filter(entity -> entity.tableName().equalsIgnoreCase(attribute.referencedTable()))
                .findFirst();

        } else if (!Names.isEmpty(attribute.referencedType())) {
            // match by type name
            Optional<TypeKind> primitiveType = Stream.of(TypeKind.values())
                .filter(TypeKind::isPrimitive)
                .filter(kind -> kind.toString().toLowerCase().equals(attribute.referencedType()))
                .findFirst();
            if (!primitiveType.isPresent()) {
                QualifiedName referencedType = new QualifiedName(attribute.referencedType());
                return entityByName(referencedType);
            } // else attribute is basic foreign key and not referring to an entity
        } else {
            TypeMirror referencedType = attribute.typeMirror();
            if (attribute.isIterable()) {
                referencedType = collectionElementType(referencedType);
            }
            TypeElement referencedElement = (TypeElement) types.asElement(referencedType);
            if (referencedElement != null) {
                String referencedName = referencedElement.getSimpleName().toString();
                return entities.values().stream()
                        .filter(entity -> match(entity, referencedName))
                        .findFirst();
            }
        }
        return Optional.empty();
    }

    private static boolean match(EntityDescriptor entity, String referenceName) {
        return (entity.typeName().className().equals(referenceName) ||
            entity.element().getSimpleName().contentEquals(referenceName));
    }

    /**
     * Given an attribute in a given type finds the corresponding attribute that it is referencing
     * in that referencing type.
     *
     * @param attribute  attribute
     * @param referenced type being referenced
     * @return optional element it references
     */
    Optional<? extends AttributeDescriptor> referencingAttribute(AttributeDescriptor attribute,
                                                                 EntityDescriptor referenced) {
        String referencedColumn = attribute.referencedColumn();
        if (Names.isEmpty(referencedColumn)) {
            // using the id
            List<AttributeDescriptor> keys = referenced.attributes().values().stream()
                .filter(AttributeDescriptor::isKey).collect(Collectors.toList());

            if (keys.size() == 1) {
                return Optional.of(keys.get(0));
            } else {
                return keys.stream()
                    .filter(other -> other.typeMirror().equals(attribute.typeMirror()))
                    .findFirst();
            }
        } else {
            return referenced.attributes().values().stream()
                .filter(other -> other.name().equals(referencedColumn))
                .findFirst();
        }
    }

    /**
     * Given an association in an entity find the attributes it maps onto in the referenced entity.
     *
     * @param entity     entity owning the attribute
     * @param attribute  association attribute
     * @param referenced type being referenced
     * @return set of mapped attributes
     */
    Set<AttributeDescriptor> mappedAttributes(EntityDescriptor entity,
                                              AttributeDescriptor attribute,
                                              EntityDescriptor referenced) {
        String mappedBy = attribute.mappedBy();
        if (Names.isEmpty(mappedBy)) {
            return referenced.attributes().values().stream()
                .filter(other -> other.cardinality() != null)
                .filter(other -> referencingEntity(other).isPresent())
                .filter(other -> referencingEntity(other)
                        .orElseThrow(IllegalStateException::new) == entity)
                .collect(Collectors.toSet());
        } else {
            return referenced.attributes().values().stream()
                .filter(other -> other.name().equals(mappedBy))
                .collect(Collectors.toSet());
        }
    }

    private static TypeMirror collectionElementType(TypeMirror typeMirror) {
        List<TypeMirror> arguments = Mirrors.listGenericTypeArguments(typeMirror);
        return arguments.isEmpty() ? typeMirror : arguments.get(0);
    }
}
