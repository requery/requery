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

import io.requery.ForeignKey;
import io.requery.meta.Cardinality;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.util.Types;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Validates the relationships, e.g. One-to-One, Many-to-Many etc between {@link EntityDescriptor}s
 * in a {@link EntityGraph} instance. Ensures their attribute's relationships match up e.g. can't
 * map {@link io.requery.ManyToMany} to {@link io.requery.OneToOne} and that the types referenced in
 * the attributes are compatible with each other.
 *
 * @author Nikhil Purushe
 */
class EntityGraphValidator {

    private final ProcessingEnvironment processingEnvironment;
    private final EntityGraph graph;

    EntityGraphValidator(ProcessingEnvironment processingEnvironment, EntityGraph graph) {
        this.processingEnvironment = processingEnvironment;
        this.graph = graph;
    }

    Set<ElementValidator> validate() {
        Set<ElementValidator> results = new LinkedHashSet<>();
        for (EntityDescriptor entity : graph.entities()) {
            results.addAll( validateEntity(entity) );
        }
        return results;
    }

    private Set<ElementValidator> validateEntity(EntityDescriptor entity) {
        Set<ElementValidator> results = new LinkedHashSet<>();

        for (Map.Entry<Element, ? extends AttributeDescriptor> entry :
            entity.attributes().entrySet()) {

            AttributeDescriptor attribute = entry.getValue();
            ElementValidator validator =
                new ElementValidator(attribute.element(), processingEnvironment);

            // check mapped associations
            if (attribute.cardinality() != null) {
                Optional<EntityDescriptor> referencingEntity = graph.referencingEntity(attribute);

                if (referencingEntity.isPresent()) {
                    EntityDescriptor referenced = referencingEntity.get();
                    Set<AttributeDescriptor> mappings =
                        graph.mappedAttributes(entity, attribute, referenced);

                    if (mappings.isEmpty()) {
                        if (attribute.cardinality() == Cardinality.ONE_TO_ONE &&
                            !attribute.isForeignKey()) {
                            validator.error(
                                "Single sided @OneToOne should specify @ForeignKey/@JoinColumn");
                        } else if (attribute.cardinality() == Cardinality.ONE_TO_MANY) {
                            validator.error(
                                "Corresponding @OneToMany relation not present in mapped entity");
                        }
                    } else if (mappings.size() == 1) {
                        // validate the relationship
                        AttributeDescriptor mapped = mappings.iterator().next();
                        validateRelationship(validator, attribute, mapped);

                    } else if (mappings.size() > 1) {
                        validator.warning(mappings.size() + " mappings found for: " +
                            attribute + " -> " + referenced.typeName());
                    }
                } else {
                    validator.warning("Couldn't find referenced element for " + attribute);
                }
            } else {
                for (EntityDescriptor descriptor : graph.entities()) {
                    Types types = processingEnvironment.getTypeUtils();
                    if (types.isSubtype(descriptor.element().asType(), attribute.typeMirror())) {
                        validator.error("Entity reference missing relationship annotation");
                    }
                }
            }

            // checked foreign key reference
            if (attribute.isForeignKey()) {
                Optional<EntityDescriptor> referenced = graph.referencingEntity(attribute);
                if (referenced.isPresent()) {
                    Optional<? extends AttributeDescriptor> referencedElement =
                        graph.referencingAttribute(attribute, referenced.get());

                    if (!referencedElement.isPresent()) {
                        validator.warning("Couldn't find referenced element " +
                                referenced.get().typeName() + " for " + attribute);
                    } else {
                        // check all the foreign keys and see if they reference this entity
                        referenced.get().attributes().values().stream()
                            .filter(AttributeDescriptor::isForeignKey)
                            .map(graph::referencingEntity)
                            .filter(Optional::isPresent)
                            .map(Optional::get)
                            .filter(entity::equals)
                            .findAny()
                            .ifPresent(other -> validator.warning(
                                "Circular Foreign Key reference found between " +
                                    entity.typeName() +  " and " + other.typeName(),
                                ForeignKey.class));
                    }
                } else if(attribute.cardinality() != null) {
                    validator.error("Couldn't find referenced attribute " +
                            attribute.referencedColumn() + " for " + attribute);
                }
            }
            if (validator.hasErrors() || validator.hasWarnings()) {
                results.add(validator);
            }
        }
        return results;
    }

    private void validateRelationship(ElementValidator validator,
                                      AttributeDescriptor source,
                                      AttributeDescriptor mapped) {

        Cardinality sourceCardinality = source.cardinality();
        Cardinality mappedCardinality = mapped.cardinality();
        Cardinality expectedCardinality;
        switch (sourceCardinality) {
            case ONE_TO_ONE:
                expectedCardinality = Cardinality.ONE_TO_ONE;
                break;
            case ONE_TO_MANY:
                expectedCardinality = Cardinality.MANY_TO_ONE;
                break;
            case MANY_TO_ONE:
                expectedCardinality = Cardinality.ONE_TO_MANY;
                break;
            case MANY_TO_MANY:
                expectedCardinality = Cardinality.MANY_TO_MANY;
                break;
            default:
                throw new IllegalStateException();
        }
        if (mappedCardinality != expectedCardinality && mapped.cardinality() != null) {
            String message = mappingErrorMessage(source, mapped, expectedCardinality);
            validator.error(message);
        } else if (sourceCardinality == Cardinality.MANY_TO_MANY) {
            Optional<AssociativeEntityDescriptor> sourceAssociation = source.associativeEntity();
            Optional<AssociativeEntityDescriptor> mappedAssociation = mapped.associativeEntity();
            if (!sourceAssociation.isPresent() && !mappedAssociation.isPresent()) {
                validator.error("One side of the ManyToMany relationship must specify the " +
                    "@JunctionTable/@JoinTable annotation");
            }
            if (sourceAssociation.isPresent() && mappedAssociation.isPresent()) {
                validator.error("@JunctionTable should be specified on only one side of a " +
                    "ManyToMany relationship");
            }
        } else if (sourceCardinality == Cardinality.ONE_TO_ONE) {
            if (!source.isForeignKey() && !mapped.isForeignKey()) {
                validator.error("One side of the OneToOne relationship must specify the " +
                    "@ForeignKey/@JoinColumn annotation");
            }
            if (source.isForeignKey() && mapped.isForeignKey() && source != mapped) {
                validator.error("Only one side of the OneToOne relationship can specify the " +
                    "@ForeignKey/@JoinColumn annotation");
            }
        }
    }

    private String mappingErrorMessage(AttributeDescriptor source, AttributeDescriptor mapped,
                                       Cardinality expected) {
        String sourceType = source.cardinality().annotationClass().getSimpleName();
        String mappedType = mapped.cardinality().annotationClass().getSimpleName();
        String expectedType = expected.annotationClass().getSimpleName();
        String message = "%s with cardinality %s incorrectly mapped to " +
                         "%s having cardinality %s, expected %s";
        return String.format(message, source, sourceType, mapped, mappedType, expectedType);
    }
}
