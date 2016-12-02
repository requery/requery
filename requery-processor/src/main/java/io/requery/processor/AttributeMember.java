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
import io.requery.Column;
import io.requery.Convert;
import io.requery.Embedded;
import io.requery.ForeignKey;
import io.requery.Generated;
import io.requery.Index;
import io.requery.JunctionTable;
import io.requery.Key;
import io.requery.Lazy;
import io.requery.ManyToMany;
import io.requery.ManyToOne;
import io.requery.Naming;
import io.requery.Nullable;
import io.requery.OneToMany;
import io.requery.OneToOne;
import io.requery.OrderBy;
import io.requery.PropertyNameStyle;
import io.requery.ReadOnly;
import io.requery.ReferentialAction;
import io.requery.Transient;
import io.requery.Version;
import io.requery.converter.EnumOrdinalConverter;
import io.requery.meta.AttributeBuilder;
import io.requery.meta.Cardinality;
import io.requery.meta.ListAttributeBuilder;
import io.requery.meta.MapAttributeBuilder;
import io.requery.meta.ResultAttributeBuilder;
import io.requery.meta.SetAttributeBuilder;
import io.requery.query.Order;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.ConstraintMode;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.JoinColumn;
import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Processes field level annotations on an abstract entity type where the annotated element can be
 * either a field in class or a method.
 *
 * @author Nikhil Purushe
 */
class AttributeMember extends BaseProcessableElement<Element> implements AttributeDescriptor {

    private final EntityDescriptor entity;
    private String name;
    private boolean isBoolean;
    private boolean isEmbedded;
    private boolean isForeignKey;
    private boolean isGenerated;
    private boolean isIndexed;
    private boolean isIterable;
    private boolean isKey;
    private boolean isLazy;
    private boolean isMap;
    private boolean isNullable;
    private boolean isOptional;
    private boolean isReadOnly;
    private boolean isTransient;
    private boolean isUnique;
    private boolean isVersion;
    private Class<? extends AttributeBuilder> builderClass;
    private Integer length;
    private Set<String> indexNames;
    private Cardinality cardinality;
    private String converterType;
    private CascadeAction[] cascadeActions;
    private ReferentialAction deleteAction;
    private ReferentialAction updateAction;
    private String referencedColumn;
    private String referencedType;
    private String referencedTable;
    private String mappedBy;
    private String defaultValue;
    private String definition;
    private String collate;
    private String orderByColumn;
    private Order orderByDirection;
    private AssociativeEntityDescriptor associativeDescriptor;

    AttributeMember(Element element, EntityDescriptor entity) {
        super(element);
        if (!element.getKind().isField() && element.getKind() != ElementKind.METHOD) {
            throw new IllegalStateException();
        }
        this.entity = entity;
        this.indexNames = new LinkedHashSet<>();
    }

    @Override
    public Set<ElementValidator> process(ProcessingEnvironment processingEnvironment) {
        Set<ElementValidator> validators = new LinkedHashSet<>();
        ElementValidator validator = new ElementValidator(element(), processingEnvironment);
        validators.add(validator);
        validateField(validator);
        processFieldAccessAnnotations(validator);
        processBasicColumnAnnotations(validator);
        processAssociativeAnnotations(processingEnvironment, validator);
        processConverterAnnotation(validator);
        checkMemberType(processingEnvironment, typeMirror(), validators);
        if (cardinality() != null && entity.isImmutable()) {
            validator.error("Immutable value type cannot contain relational references");
        }
        checkReserved(name(), validator);
        isEmbedded = annotationOf(Embedded.class).isPresent() ||
            annotationOf(javax.persistence.Embedded.class).isPresent();
        indexNames.forEach(name -> checkReserved(name, validator));
        return validators;
    }

    private void validateField(ElementValidator validator) {
        if (element().getKind().isField()) {
            Set<Modifier> modifiers = element().getModifiers();
            if (!entity.isUnimplementable() && modifiers.contains(Modifier.PRIVATE)) {
                validator.error("Entity field cannot be private");
            }
            if (modifiers.contains(Modifier.STATIC)) {
                validator.error("Entity field cannot be static");
            }
            if (modifiers.contains(Modifier.FINAL)) {
                validator.error("Entity field cannot be final");
            }
        }
    }

    private void checkMemberType(ProcessingEnvironment processingEnvironment, TypeMirror type,
                                 Set<ElementValidator> validators) {
        builderClass = AttributeBuilder.class;
        Types types = processingEnvironment.getTypeUtils();
        isBoolean = type.getKind() == TypeKind.BOOLEAN;
        if (type.getKind() == TypeKind.DECLARED) {
            TypeElement element = (TypeElement) types.asElement(type);
            if (element != null) {
                // only set if the attribute is relational
                if (cardinality != null) {
                    isIterable = Mirrors.isInstance(types, element, Iterable.class);
                }
                isMap = Mirrors.isInstance(types, element, Map.class);
                if (isMap && cardinality != null) {
                    builderClass = MapAttributeBuilder.class;
                }
                isOptional = Mirrors.isInstance(types, element, Optional.class);
                isBoolean = Mirrors.isInstance(types, element, Boolean.class);
            }
        }
        if (isIterable) {
            validators.add(validateCollectionType(processingEnvironment));
        }
    }

    private ElementValidator validateCollectionType(ProcessingEnvironment processingEnvironment) {
        Types types = processingEnvironment.getTypeUtils();
        TypeElement collectionElement = (TypeElement) types.asElement(typeMirror());
        ElementValidator validator = new ElementValidator(collectionElement, processingEnvironment);
        if (Mirrors.isInstance(types, collectionElement, List.class)) {
            builderClass = ListAttributeBuilder.class;
        } else if (Mirrors.isInstance(types, collectionElement, Set.class)) {
            builderClass = SetAttributeBuilder.class;
        } else if (Mirrors.isInstance(types, collectionElement, Iterable.class)) {
            builderClass = ResultAttributeBuilder.class;
        } else {
            validator.error("Invalid collection type, must be Set, List or Iterable");
        }
        return validator;
    }

    private void processFieldAccessAnnotations(ElementValidator validator) {
        if (annotationOf(Transient.class).isPresent() ||
            annotationOf(java.beans.Transient.class).isPresent() ||
            annotationOf(javax.persistence.Transient.class).isPresent() ||
                element().getModifiers().contains(Modifier.TRANSIENT)) {
            isTransient = true;
        }
        isReadOnly = annotationOf(ReadOnly.class).isPresent();
        if (!SourceVersion.isIdentifier(getterName())) {
            validator.error("Invalid getter name " + getterName(), Naming.class);
        }
        if (!SourceVersion.isIdentifier(setterName())) {
            validator.error("Invalid setter name " + setterName(), Naming.class);
        }
    }

    private void processBasicColumnAnnotations(ElementValidator validator) {
        if (annotationOf(Key.class).isPresent() ||
            annotationOf(javax.persistence.Id.class).isPresent()) {
            isKey = true;
            if (isTransient) {
                validator.error("Key field cannot be transient");
            }
        }
        // generated keys can't be set through a setter
        if (annotationOf(Generated.class).isPresent() ||
            annotationOf(GeneratedValue.class).isPresent()) {
            isGenerated = true;
            isReadOnly = true;

            // check generation strategy
            annotationOf(GeneratedValue.class).ifPresent(generatedValue -> {
                if (generatedValue.strategy() != GenerationType.IDENTITY  &&
                    generatedValue.strategy() != GenerationType.AUTO) {
                    validator.warning("GeneratedValue.strategy() " +
                        generatedValue.strategy() + " not supported", generatedValue.getClass());
                }
            });
        }
        if (annotationOf(Lazy.class).isPresent()) {
            if (isKey) {
                cannotCombine(validator, Key.class, Lazy.class);
            }
            isLazy = true;
        }
        if (annotationOf(Nullable.class).isPresent() || isOptional ||
            Mirrors.findAnnotationMirror(element(), "javax.annotation.Nullable").isPresent()) {
            isNullable = true;
        } else {
            // if not a primitive type the value assumed nullable
            if (element().getKind().isField()) {
                isNullable = !element().asType().getKind().isPrimitive();
            } else if(element().getKind() == ElementKind.METHOD) {
                ExecutableElement executableElement = (ExecutableElement) element();
                isNullable = !executableElement.getReturnType().getKind().isPrimitive();
            }
        }
        if (annotationOf(Version.class).isPresent() ||
            annotationOf(javax.persistence.Version.class).isPresent()) {
            isVersion = true;
            if (isKey) {
                cannotCombine(validator, Key.class, Version.class);
            }
        }

        Column column = annotationOf(Column.class).orElse(null);
        ForeignKey foreignKey = null;
        boolean foreignKeySetFromColumn = false;
        if (column != null) {
            name = "".equals(column.name()) ? null : column.name();
            isUnique = column.unique();
            isNullable = column.nullable();
            defaultValue = column.value();
            collate = column.collate();
            definition = column.definition();
            if (column.length() > 0) {
                length = column.length();
            }
            if (column.foreignKey().length > 0) {
                foreignKey = column.foreignKey()[0];
                foreignKeySetFromColumn = true;
            }
        }
        if (!foreignKeySetFromColumn) {
            foreignKey = annotationOf(ForeignKey.class).orElse(null);
        }
        if (foreignKey != null) {
            this.isForeignKey = true;
            deleteAction = foreignKey.delete();
            updateAction = foreignKey.update();
            referencedColumn = foreignKey.referencedColumn();
        }
        annotationOf(Index.class).ifPresent(index -> {
            isIndexed = true;
            Collections.addAll(indexNames, index.value());
        });

        // JPA specific
        annotationOf(Basic.class).ifPresent(basic -> {
            isNullable = basic.optional();
            isLazy = basic.fetch() == FetchType.LAZY;
        });

        annotationOf(javax.persistence.Index.class).ifPresent(index -> {
            isIndexed = true;
            Collections.addAll(indexNames, index.name());
        });

        annotationOf(JoinColumn.class).ifPresent(joinColumn -> {
            javax.persistence.ForeignKey joinForeignKey = joinColumn.foreignKey();
            this.isForeignKey = true;
            ConstraintMode constraintMode = joinForeignKey.value();
            switch (constraintMode) {
                default:
                case PROVIDER_DEFAULT:
                case CONSTRAINT:
                    deleteAction = ReferentialAction.CASCADE;
                    updateAction = ReferentialAction.CASCADE;
                    break;
                case NO_CONSTRAINT:
                    deleteAction = ReferentialAction.NO_ACTION;
                    updateAction = ReferentialAction.NO_ACTION;
                    break;
            }
            this.referencedTable = joinColumn.table();
            this.referencedColumn = joinColumn.referencedColumnName();
        });

        annotationOf(javax.persistence.Column.class).ifPresent(persistenceColumn -> {
            name = "".equals(persistenceColumn.name()) ? null : persistenceColumn.name();
            isUnique = persistenceColumn.unique();
            isNullable = persistenceColumn.nullable();
            length = persistenceColumn.length();
            isReadOnly = !persistenceColumn.updatable();
            definition = persistenceColumn.columnDefinition();
        });

        annotationOf(Enumerated.class).ifPresent(enumerated -> {
            EnumType enumType = enumerated.value();
            if (enumType == EnumType.ORDINAL) {
                converterType = EnumOrdinalConverter.class.getCanonicalName();
            }
        });
    }

    private void processAssociativeAnnotations(ProcessingEnvironment processingEnvironment,
                                               ElementValidator validator) {
        Optional<Annotation> oneToOne = annotationOf(OneToOne.class);
        Optional<Annotation> oneToMany = annotationOf(OneToMany.class);
        Optional<Annotation> manyToOne = annotationOf(ManyToOne.class);
        Optional<Annotation> manyToMany = annotationOf(ManyToMany.class);

        oneToOne = oneToOne.isPresent() ? oneToOne :
            annotationOf(javax.persistence.OneToOne.class);
        oneToMany = oneToMany.isPresent() ? oneToMany :
            annotationOf(javax.persistence.OneToMany.class);
        manyToOne = manyToOne.isPresent() ? manyToOne :
            annotationOf(javax.persistence.ManyToOne.class);
        manyToMany = manyToMany.isPresent() ? manyToMany :
            annotationOf(javax.persistence.ManyToMany.class);

        if (Stream.of(oneToOne, oneToMany, manyToOne, manyToMany)
            .filter(Optional::isPresent).count() > 1) {
            validator.error("Cannot have more than one associative annotation per field");
        }
        if (oneToOne.isPresent()) {
            cardinality = Cardinality.ONE_TO_ONE;
            ReflectiveAssociation reflect = new ReflectiveAssociation(oneToOne.get());
            mappedBy = reflect.mappedBy();
            cascadeActions = reflect.cascade();
            if (!isForeignKey()) {
                isReadOnly = true;
                if (!isKey()) {
                    isUnique = true;
                }
            }
        }
        if (oneToMany.isPresent()) {
            isIterable = true;
            cardinality = Cardinality.ONE_TO_MANY;
            isReadOnly = true;
            ReflectiveAssociation reflect = new ReflectiveAssociation(oneToMany.get());
            mappedBy = reflect.mappedBy();
            cascadeActions = reflect.cascade();
            checkIterable(validator);
            processOrderBy();
        }
        if (manyToOne.isPresent()) {
            cardinality = Cardinality.MANY_TO_ONE;
            isForeignKey = true;
            ReflectiveAssociation reflect = new ReflectiveAssociation(manyToOne.get());
            cascadeActions = reflect.cascade();
            if (deleteAction == null) {
                deleteAction = ReferentialAction.CASCADE;
            }
            if (updateAction == null) {
                updateAction = ReferentialAction.CASCADE;
            }
        }
        if (manyToMany.isPresent()) {
            isIterable = true;
            cardinality = Cardinality.MANY_TO_MANY;
            ReflectiveAssociation reflect = new ReflectiveAssociation(manyToMany.get());
            mappedBy = reflect.mappedBy();
            cascadeActions = reflect.cascade();
            Optional<JunctionTable> junctionTable = annotationOf(JunctionTable.class);
            Optional<javax.persistence.JoinTable> joinTable =
                annotationOf(javax.persistence.JoinTable.class);

            if (junctionTable.isPresent()) {
                Elements elements = processingEnvironment.getElementUtils();
                associativeDescriptor =
                    new JunctionTableAssociation(elements, this, junctionTable.get());

            } else if(joinTable.isPresent()) {
                associativeDescriptor = new JoinTableAssociation(joinTable.get());
            }
            isReadOnly = true;
            checkIterable(validator);
            processOrderBy();
        }
        if (isForeignKey()) {
            if (deleteAction == ReferentialAction.SET_NULL && !isNullable()) {
                validator.error("Cannot SET_NULL on optional attribute", ForeignKey.class);
            }
            // user mirror so generated type can be referenced
            Optional<? extends AnnotationMirror> mirror =
                    Mirrors.findAnnotationMirror(element(), ForeignKey.class);
            if (mirror.isPresent()) {
                referencedType = mirror.flatMap(m -> Mirrors.findAnnotationValue(m, "references"))
                        .map(value -> value.getValue().toString())
                        .orElse(null);
            } else if (!typeMirror().getKind().isPrimitive()) {
                referencedType = typeMirror().toString();
            }
        }
    }

    private void checkReserved(String name, ElementValidator validator) {
        if (Stream.of(ReservedKeyword.values())
            .anyMatch(keyword -> keyword.toString().equalsIgnoreCase(name))) {
            validator.warning("Column or index name " + name + " may need to be escaped");
        }
    }

    private void checkIterable(ElementValidator validator) {
        if (!isIterable()) {
            validator.error("Many relation must be stored in an iterable type");
        }
    }

    private void processConverterAnnotation(ElementValidator validator) {
        if (annotationOf(Convert.class).isPresent()) {
            Optional<? extends AnnotationMirror> mirror =
                Mirrors.findAnnotationMirror(element(), Convert.class);
            converterType = mirror.map(Mirrors::findAnnotationValue)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(value -> value.getValue().toString()).orElse(null);

        } else if (annotationOf(javax.persistence.Convert.class).isPresent()) {

            Optional<? extends AnnotationMirror> mirror =
                Mirrors.findAnnotationMirror(element(), javax.persistence.Convert.class);
            converterType = mirror.map(m -> Mirrors.findAnnotationValue(m, "converter"))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(value -> value.getValue().toString()).orElse(null);
        }

        if (converterType != null && cardinality != null) {
            validator.warning("Cannot specify converter on association field", Convert.class);
        }
    }

    private void processOrderBy() {
        annotationOf(OrderBy.class).ifPresent(orderBy -> {
            orderByColumn = orderBy.value();
            orderByDirection = orderBy.order();
        });
        annotationOf(javax.persistence.OrderBy.class).ifPresent(orderBy -> {
            String value = orderBy.value();
            String[] parts = value.split(" ");
            if (parts.length > 0) {
                orderByColumn = parts[0].trim();
                if (parts.length > 1) {
                    String direction = parts[1].toUpperCase().trim();
                    try {
                        orderByDirection = Order.valueOf(direction);
                    } catch (IllegalArgumentException e) {
                        orderByDirection = Order.ASC;
                    }
                }
            }
        });
    }

    private void cannotCombine(ElementValidator validator,
                               Class<? extends Annotation> annotation1,
                               Class<? extends Annotation> annotation2) {
        String first = annotation1.getSimpleName();
        String second = annotation2.getSimpleName();
        validator.error("The " + first +
                " annotation cannot be combined with annotation " + second, annotation2);
    }

    private String getMethodName(String override, String prefix) {
        override = override.replace("\"", "");
        if (Names.isEmpty(override)) {
            CharSequence simpleName = Names.removeMemberPrefixes(element().getSimpleName());
            return Names.isEmpty(prefix) ?
                Names.lowerCaseFirst(simpleName) :
                prefix + Names.upperCaseFirst(simpleName);
        } else {
            return override;
        }
    }

    @Override
    public TypeMirror typeMirror() {
        if (element().getKind().isField()) {
            return element().asType();
        } else {
            ExecutableElement executableElement = (ExecutableElement) element();
            return executableElement.getReturnType();
        }
    }

    @Override
    public String fieldName() {
        if (element().getKind().isField()) {
            return element().getSimpleName().toString();
        } else if (element().getKind() == ElementKind.METHOD) {
            ExecutableElement methodElement = (ExecutableElement) element();
            String originalName = methodElement.getSimpleName().toString();
            String name = Names.removeMethodPrefixes(originalName);
            if (Names.isAllUpper(name)) {
                name = name.toLowerCase(Locale.ROOT);
            } else {
                name = Names.lowerCaseFirst(name);
            }
            return Names.checkReservedName(name, originalName);
        } else {
            throw new IllegalStateException();
        }
    }

    @Override
    public String getterName() {
        if (element().getKind().isField()) {
            String name = annotationOf(Naming.class).map(Naming::getter).orElse("");
            String prefix = "";
            if (useBeanStyleProperties()) {
                prefix = isBoolean ? "is" : "get";
            }
            return getMethodName(name, prefix);
        } else {
            return element().getSimpleName().toString();
        }
    }

    @Override
    public String setterName() {
        if (element().getKind().isField()) {
            String name = annotationOf(Naming.class).map(Naming::setter).orElse("");
            return getMethodName(name, useBeanStyleProperties() ? "set" : "");
        } else {
            // if an interface try to find a matching setter to implement
            for (ExecutableElement element :
                ElementFilter.methodsIn(entity.element().getEnclosedElements())) {
                List<? extends VariableElement> parameters = element.getParameters();
                if (parameters.size() == 1) {
                    String property =
                        Names.removeMethodPrefixes(element.getSimpleName().toString());
                    if (property.toLowerCase(Locale.ROOT).equalsIgnoreCase(name())) {
                        return element.getSimpleName().toString();
                    }
                }
            }
            // otherwise create one
            ExecutableElement executableElement = (ExecutableElement) element();
            String elementName = element().getSimpleName().toString();
            AccessorNamePrefix prefix = AccessorNamePrefix.fromElement(executableElement);
            switch (prefix) {
                case GET:
                    return elementName.replaceFirst("get", "set");
                case IS:
                    return elementName.replaceFirst("is", "set");
                case NONE:
                default:
                    return elementName;
            }
        }
    }

    private boolean useBeanStyleProperties() {
        return entity.propertyNameStyle() == PropertyNameStyle.BEAN ||
               entity.propertyNameStyle() == PropertyNameStyle.FLUENT_BEAN;
    }

    @Override
    public String name() {
        if (!Names.isEmpty(name)) {
            return name;
        }
        String elementName = element().getSimpleName().toString();
        // for a method strip any accessor prefix such as get/is
        if (element().getKind() == ElementKind.METHOD) {
            ExecutableElement executableElement = (ExecutableElement) element();
            String originalName = elementName;
            AccessorNamePrefix prefix = AccessorNamePrefix.fromElement(executableElement);
            switch (prefix) {
                case GET:
                    elementName = elementName.replaceFirst("get", "");
                    break;
                case IS:
                    elementName = elementName.replaceFirst("is", "");
                    break;
            }
            elementName = Names.isAllUpper(elementName) ?
                    elementName : Names.lowerCaseFirst(elementName);
            return Names.checkReservedName(elementName, originalName);
        }
        return elementName;
    }

    @Override
    public String collate() {
        return collate;
    }

    @Override
    public Integer columnLength() {
        return length;
    }

    @Override
    public String converterName() {
        return converterType;
    }

    @Override
    public Set<String> indexNames() {
        return indexNames;
    }

    @Override
    public String defaultValue() {
        return defaultValue;
    }

    @Override
    public String definition() {
        return definition;
    }

    @Override
    public boolean isEmbedded() {
        return isEmbedded;
    }

    @Override
    public boolean isForeignKey() {
        return isForeignKey;
    }

    @Override
    public boolean isNullable() {
        return isNullable;
    }

    @Override
    public boolean isGenerated() {
        return isGenerated;
    }

    @Override
    public boolean isIndexed() {
        return isIndexed;
    }

    @Override
    public boolean isIterable() {
        return isIterable;
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
    public boolean isMap() {
        return isMap;
    }

    @Override
    public boolean isOptional() {
        return isOptional;
    }

    @Override
    public boolean isReadOnly() {
        return isReadOnly;
    }

    @Override
    public boolean isTransient() {
        return isTransient;
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
        EnumSet<CascadeAction> actions = EnumSet.noneOf(CascadeAction.class);
        if (cascadeActions != null) {
            actions.addAll(Arrays.asList(cascadeActions));
        }
        return actions;
    }

    @Override
    public String referencedColumn() {
        return referencedColumn;
    }

    @Override
    public String referencedType() {
        return referencedType;
    }

    @Override
    public String referencedTable() {
        return referencedTable;
    }

    @Override
    public String mappedBy() {
        return mappedBy;
    }

    @Override
    public String orderBy() {
        return orderByColumn;
    }

    @Override
    public Order orderByDirection() {
        return orderByDirection;
    }

    @Override
    public Class<? extends AttributeBuilder> builderClass() {
        return builderClass;
    }

    @Override
    public Optional<AssociativeEntityDescriptor> associativeEntity() {
        return Optional.ofNullable(associativeDescriptor);
    }

    @Override
    public String toString() {
        return entity.typeName() + "." + name();
    }

    private static class ReflectiveAssociation {

        private final Annotation annotation;

        ReflectiveAssociation(Annotation annotation) {
            this.annotation = annotation;
        }

        String mappedBy() {
            try {
                return (String) annotation.getClass().getMethod("mappedBy").invoke(annotation);
            } catch (Exception e) {
                return null;
            }
        }

        CascadeAction[] cascade() {
            try {
                return (CascadeAction[])
                    annotation.getClass().getMethod("cascade").invoke(annotation);
            } catch (Exception e) {
                try {
                    CascadeType[] cascadeTypes = (CascadeType[])
                        annotation.getClass().getMethod("cascade").invoke(annotation);
                    return mapCascadeActions(cascadeTypes);
                } catch (Exception ee) {
                    return null;
                }
            }
        }

        private static CascadeAction[] mapCascadeActions(CascadeType[] types) {
            EnumSet<CascadeAction> actions = EnumSet.noneOf(CascadeAction.class);
            for (CascadeType type : types) {
                switch (type) {
                    case ALL:
                        actions.addAll(EnumSet.allOf(CascadeAction.class));
                    case PERSIST:
                        actions.add(CascadeAction.SAVE);
                        break;
                    case MERGE:
                        actions.add(CascadeAction.SAVE);
                        break;
                    case REMOVE:
                        actions.add(CascadeAction.DELETE);
                        break;
                    case REFRESH:
                        break;
                }
            }
            return actions.toArray(new CascadeAction[actions.size()]);
        }
    }
}
