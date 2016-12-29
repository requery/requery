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

import io.requery.Embedded;
import io.requery.Entity;
import io.requery.Factory;
import io.requery.PropertyNameStyle;
import io.requery.ReadOnly;
import io.requery.Table;
import io.requery.Transient;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.Name;
import javax.lang.model.element.NestingKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;
import javax.persistence.Cacheable;
import javax.persistence.Embeddable;
import javax.persistence.Index;
import javax.tools.Diagnostic;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Processes class level annotations on an abstract entity type.
 *
 * @author Nikhil Purushe
 */
class EntityType extends BaseProcessableElement<TypeElement> implements EntityDescriptor {

    private final ProcessingEnvironment processingEnvironment;
    private final Map<Element, AttributeDescriptor> attributes;
    private final Map<Element, ListenerMethod> listeners;
    private final String modelName;
    private final QualifiedName qualifiedName;

    EntityType(ProcessingEnvironment processingEnvironment, TypeElement typeElement) {
        super(typeElement);
        this.processingEnvironment = processingEnvironment;
        attributes = new LinkedHashMap<>();
        listeners = new LinkedHashMap<>();
        modelName = createModelName();
        qualifiedName = createQualifiedName();
    }

    @Override
    public Set<ElementValidator> process(ProcessingEnvironment processingEnvironment) {
        // create attributes for fields that have no annotations
        if (element().getKind().isInterface() || isImmutable() || isUnimplementable()) {
            ElementFilter.methodsIn(element().getEnclosedElements()).stream()
                .filter(this::isMethodProcessable)
                .forEach(this::computeAttribute);
        } else {
            // private/static/final members fields are skipped
            ElementFilter.fieldsIn(element().getEnclosedElements()).stream()
                .filter(element -> !element.getModifiers().contains(Modifier.PRIVATE) &&
                    !element.getModifiers().contains(Modifier.STATIC) &&
                    (!element.getModifiers().contains(Modifier.FINAL) || isImmutable()))
                .forEach(this::computeAttribute);
        }
        // find listener annotated methods
        ElementFilter.methodsIn(element().getEnclosedElements()).forEach(element ->
                ListenerAnnotations.all().forEach(annotation -> {
            if (element.getAnnotation(annotation) != null) {
                ListenerMethod listener = listeners.computeIfAbsent(element,
                    key -> new ListenerMethod(element));
                listener.annotations().put(annotation, element.getAnnotation(annotation));
            }
        }));

        Set<ProcessableElement<?>> elements = new LinkedHashSet<>();
        attributes().values().forEach(
            attribute -> elements.add((ProcessableElement<?>) attribute));

        elements.addAll(listeners.values());
        Set<ElementValidator> validations = new LinkedHashSet<>();
        elements.forEach(element -> validations.addAll(element.process(processingEnvironment)));

        ElementValidator validator = new ElementValidator(element(), processingEnvironment);
        Entity entity = annotationOf(Entity.class).orElse(null);
        if (entity != null &&
            !Names.isEmpty(entity.name()) && !SourceVersion.isIdentifier(entity.name())) {
            validator.error("Invalid class identifier " + entity.name(), Entity.class);
        }
        if (element().getNestingKind() == NestingKind.ANONYMOUS) {
            validator.error("Entity annotation cannot be applied to anonymous class");
        }
        if (element().getKind() == ElementKind.ENUM) {
            validator.error("Entity annotation cannot be applied to an enum class");
        }
        if (attributes.values().isEmpty()) {
            validator.warning("Entity contains no attributes");
        }
        if (!isReadOnly() && !isEmbedded() && attributes.values().size() == 1 &&
            attributes.values().iterator().next().isGenerated()) {
            validator.warning(
                "Entity contains only a single generated attribute may fail to persist");
        }
        checkReserved(tableName(), validator);
        validations.add(validator);
        return validations;
    }

    private boolean isMethodProcessable(ExecutableElement element) {
        // if an immutable type with an implementation provided skip it
        if (!isUnimplementable() && element().getKind().isClass() && isImmutable() &&
            !element.getModifiers().contains(Modifier.ABSTRACT)) {
            return false;
        }
        String name = element.getSimpleName().toString();
        // skip kotlin data class methods with component1, component2.. names
        if (isUnimplementable() &&
            name.startsWith("component") && name.length() > "component".length()) {
            return false;
        }

        TypeMirror type = element.getReturnType();
        boolean isInterface = element().getKind().isInterface();
        // must be a getter style method with no args, can't return void or itself or its builder
        return type.getKind() != TypeKind.VOID &&
               element.getParameters().isEmpty() &&
               (isImmutable() || isInterface || !element.getModifiers().contains(Modifier.FINAL)) &&
               (!isImmutable() || !type.equals(element().asType())) &&
               !type.equals(builderType().map(Element::asType).orElse(null)) &&
               !element.getModifiers().contains(Modifier.STATIC) &&
               !element.getModifiers().contains(Modifier.DEFAULT) &&
               !Mirrors.findAnnotationMirror(element(), Transient.class).isPresent() &&
               !name.equals("toString") && !name.equals("hashCode");
    }

    void addAnnotationElement(TypeElement annotationElement, Element annotatedElement) {
        String qualifiedName = annotationElement.getQualifiedName().toString();
        Class<? extends Annotation> type;
        try {
            type = Class.forName(qualifiedName).asSubclass(Annotation.class);
        } catch (ClassNotFoundException e) {
            return;
        }
        switch (annotatedElement.getKind()) {
            case CLASS:
            case INTERFACE:
                annotations().put(type, annotatedElement.getAnnotation(type));
                break;
            case FIELD:
                if(annotatedElement.getModifiers().contains(Modifier.STATIC) ||
                   annotatedElement.getModifiers().contains(Modifier.FINAL)) {
                    // check if this a requery annotation
                    String packageName = Entity.class.getPackage().getName();
                    if (annotationElement.getQualifiedName().toString().startsWith(packageName)) {
                        processingEnvironment.getMessager().printMessage(Diagnostic.Kind.ERROR,
                                annotationElement.getQualifiedName() +
                                    " not applicable to static or final member", annotatedElement);
                    }
                } else {
                    VariableElement element = (VariableElement) annotatedElement;
                    Optional<AttributeMember> attribute = computeAttribute(element);
                    Annotation annotation = annotatedElement.getAnnotation(type);
                    attribute.ifPresent(a -> a.annotations().put(type, annotation));
                }
                break;
            case METHOD:
                ExecutableElement element = (ExecutableElement) annotatedElement;
                Annotation annotation = annotatedElement.getAnnotation(type);

                if (ListenerAnnotations.all().anyMatch(a -> a.equals(type))) {
                    ListenerMethod listener = listeners.computeIfAbsent(element,
                        key -> new ListenerMethod(element));
                    listener.annotations().put(type, annotation);
                } else if (isMethodProcessable(element)) {
                    Optional<AttributeMember> attribute = computeAttribute(element);
                    attribute.ifPresent(a -> a.annotations().put(type, annotation));
                }
                break;
        }
    }

    private Optional<AttributeMember> computeAttribute(Element element) {
        return Optional.of((AttributeMember)
            attributes.computeIfAbsent(element, key -> new AttributeMember(element, this)));
    }

    void merge(EntityType from) {
        for (Map.Entry<Element, ? extends AttributeDescriptor> entry :
            from.attributes().entrySet()) {
            // add this attribute if an attribute with the same name is not already existing
            AttributeDescriptor newAttribute = entry.getValue();
            if (!attributes.values().stream().anyMatch(
                attribute -> attribute.name().equals(newAttribute.name()))) {
                attributes.put(entry.getKey(), newAttribute);
            }
        }
        from.listeners().entrySet().stream()
            .filter(entry -> entry.getValue() instanceof ListenerMethod)
            .forEach(entry -> {
                ListenerMethod method = (ListenerMethod) entry.getValue();
                if (!listeners.values().stream().anyMatch(
                    listener -> listener.element().getSimpleName()
                        .equals(method.element().getSimpleName()))) {
                    listeners.put(entry.getKey(), method);
                }
        });
    }

    @Override
    public boolean generatesAdditionalTypes() {
        return attributes.values().stream()
            .anyMatch(member -> member.associativeEntity().isPresent());
    }

    private void checkReserved(String name, ElementValidator validator) {
        if (Stream.of(ReservedKeyword.values())
                .anyMatch(keyword -> keyword.toString().equalsIgnoreCase(name))) {
            validator.warning("Table or view name " + name + " may need to be escaped");
        }
    }

    private String createModelName() {
        // it's important that the AnnotationMirror is used here since the model name needs to be
        // known before process() is called
        if (Mirrors.findAnnotationMirror(element(), Entity.class).isPresent()) {
            return Mirrors.findAnnotationMirror(element(), Entity.class)
                .flatMap(mirror -> Mirrors.findAnnotationValue(mirror, "model"))
                .map(value -> value.getValue().toString())
                .filter(name -> !Names.isEmpty(name))
                .orElse("default");
        } else if (Mirrors.findAnnotationMirror(element(),
            javax.persistence.Entity.class).isPresent()) {
            Elements elements = processingEnvironment.getElementUtils();
            Name packageName = elements.getPackageOf(element()).getQualifiedName();
            String[] parts = packageName.toString().split("\\.");
            return parts[parts.length - 1];
        }
        return "";
    }

    private QualifiedName createQualifiedName() {

        String entityName = Stream.of(
            Mirrors.findAnnotationMirror(element(), Entity.class),
            Mirrors.findAnnotationMirror(element(), javax.persistence.Entity.class))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .map(mirror -> Mirrors.findAnnotationValue(mirror, "name"))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .map(value -> value.getValue().toString())
            .filter(name -> !Names.isEmpty(name))
            .findAny().orElse("");

        Elements elements = processingEnvironment.getElementUtils();
        String packageName = elements.getPackageOf(element()).getQualifiedName().toString();
        // if set in the annotation just use that
        if (!Names.isEmpty(entityName)) {
            return new QualifiedName(packageName, entityName);
        }
        String typeName = element().getSimpleName().toString();
        if (element().getKind().isInterface()) {
            // maybe I<Something> style
            if (typeName.startsWith("I") && Character.isUpperCase(typeName.charAt(1))) {
                entityName = typeName.substring(1);
            } else {
                entityName = typeName + "Entity";
            }
        } else {
            entityName = Names.removeClassPrefixes(typeName);
            if (entityName.equals(typeName)) {
                entityName = typeName + (isImmutable() || isUnimplementable() ? "Type" : "Entity");
            }
        }
        return new QualifiedName(packageName, entityName);
    }

    @Override
    public Map<Element, ? extends AttributeDescriptor> attributes() {
        return attributes;
    }

    @Override
    public Map<Element, ? extends ListenerDescriptor> listeners() {
        return listeners;
    }

    @Override
    public String modelName() {
        return modelName;
    }

    @Override
    public QualifiedName typeName() {
        return qualifiedName;
    }

    @Override
    public PropertyNameStyle propertyNameStyle() {
        return annotationOf(Entity.class)
            .map(Entity::propertyNameStyle)
            .orElse(PropertyNameStyle.BEAN);
    }

    @Override
    public String tableName() {
        return annotationOf(Table.class).map(Table::name).orElse(
               annotationOf(javax.persistence.Table.class)
                   .map(javax.persistence.Table::name).orElse(
            element().getKind().isInterface() || isImmutable() ?
                element().getSimpleName().toString() :
                Names.removeClassPrefixes(element().getSimpleName())));
    }

    @Override
    public String[] tableAttributes() {
        return annotationOf(Table.class).map(Table::createAttributes).orElse(new String[]{});
    }

    @Override
    public String[] tableUniqueIndexes() {
        if (annotationOf(javax.persistence.Table.class).isPresent()) {
            Index[] indexes = annotationOf(javax.persistence.Table.class)
                    .map(javax.persistence.Table::indexes)
                    .orElse(new Index[0]);
            Set<String> names = Stream.of(indexes).filter(Index::unique)
                    .map(Index::name).collect(Collectors.toSet());
            return names.toArray(new String[names.size()]);
        }
        return annotationOf(Table.class).map(Table::uniqueIndexes).orElse(new String[]{});
    }

    @Override
    public boolean isCacheable() {
        return annotationOf(Entity.class).map(Entity::cacheable)
            .orElse( annotationOf(Cacheable.class).map(Cacheable::value).orElse(true));
    }

    @Override
    public boolean isCopyable() {
        return annotationOf(Entity.class).map(Entity::copyable).orElse(false);
    }

    @Override
    public boolean isReadOnly() {
        return annotationOf(ReadOnly.class).isPresent();
    }

    @Override
    public boolean isStateless() {
        return isImmutable() || isUnimplementable() ||
            annotationOf(Entity.class).map(Entity::stateless).orElse(false);
    }

    @Override
    public boolean isImmutable() {
        // check known immutable type annotations then check the annotation value
        return Stream.of("com.google.auto.value.AutoValue",
                         "auto.parcel.AutoParcel",
                         "org.immutables.value.Value.Immutable")
            .anyMatch(type -> Mirrors.findAnnotationMirror(element(), type).isPresent()) ||
            isUnimplementable() ||
            annotationOf(Entity.class).map(Entity::immutable).orElse(false);
    }

    @Override
    public boolean isUnimplementable() {
        boolean extendable = annotationOf(Entity.class).map(Entity::extendable).orElse(true);
        return !extendable || (element().getKind().isClass() &&
            element().getModifiers().contains(Modifier.FINAL));
    }

    @Override
    public boolean isEmbedded() {
        return annotationOf(Embedded.class).isPresent() ||
            annotationOf(Embeddable.class).isPresent();
    }

    @Override
    public Optional<TypeElement> builderType() {
        Optional<Entity> entityAnnotation = annotationOf(Entity.class);
        if (entityAnnotation.isPresent()) {
            Entity entity = entityAnnotation.get();
            try {
                entity.builder(); // easiest way to get the class TypeMirror
            } catch (MirroredTypeException typeException) {
                TypeMirror mirror = typeException.getTypeMirror();
                Elements elements = processingEnvironment.getElementUtils();
                TypeElement element = elements.getTypeElement(mirror.toString());
                if (element != null) {
                    return Optional.of(element);
                }
            }
        }
        return ElementFilter.typesIn(element().getEnclosedElements()).stream()
            .filter(element -> element.getSimpleName().toString().equals("Builder"))
            .findFirst();
    }

    @Override
    public Optional<ExecutableElement> builderFactoryMethod() {
        return ElementFilter.methodsIn(element().getEnclosedElements()).stream()
            .filter(element -> element.getModifiers().contains(Modifier.STATIC))
            .filter(element -> element.getSimpleName().toString().equals("builder"))
            .findFirst();
    }

    @Override
    public Optional<ExecutableElement> factoryMethod() {
        Optional<ExecutableElement> staticFactory =
            ElementFilter.methodsIn(element().getEnclosedElements()).stream()
            .filter(element -> element.getModifiers().contains(Modifier.STATIC))
            .filter(element -> element.getSimpleName().toString().equalsIgnoreCase("create"))
            .filter(element -> element.getParameters().size() > 0)
            .filter(element -> element.getReturnType().equals(element().asType()))
            .findAny();
        Optional<ExecutableElement> constructor =
            ElementFilter.constructorsIn(element().getEnclosedElements()).stream()
            .filter(element -> element.getParameters().size() > 0)
            .findAny();
        return staticFactory.isPresent() ? staticFactory : constructor;
    }

    @Override
    public List<String> factoryArguments() {
        List<String> names = new ArrayList<>();
        ExecutableElement method = factoryMethod().orElseThrow(IllegalStateException::new);
        // TODO need more validation here
        // now match the builder fields to the parameters...
        Map<Element, AttributeDescriptor> map = new LinkedHashMap<>(attributes);
        for (VariableElement parameter : method.getParameters()) {
            // straight forward case type and name are the same
            Element matched = null;
            for (Map.Entry<Element, AttributeDescriptor> entry : map.entrySet()) {
                AttributeDescriptor attribute = entry.getValue();
                String fieldName = attribute.fieldName();
                if (fieldName.equalsIgnoreCase(parameter.getSimpleName().toString())) {
                    names.add(fieldName);
                    matched = entry.getKey();
                }
            }
            if (matched != null) {
                map.remove(matched);
            }
        }

        // didn't work likely because the parameter names are missing
        if (names.isEmpty()) {
            // for kotlin data classes add processable element field names in order
            if (isUnimplementable()) {
                ElementFilter.methodsIn(element().getEnclosedElements()).stream()
                        .filter(this::isMethodProcessable)
                        .forEach(getter ->
                                names.addAll(map.entrySet().stream()
                                        .filter(entry -> entry.getKey().equals(getter))
                                        .map(entry -> entry.getValue().fieldName())
                                        .collect(Collectors.toList())));
            } else {
                for (Map.Entry<Element, AttributeDescriptor> entry : map.entrySet()) {
                    names.add(0, entry.getValue().fieldName());
                }
            }
        }
        return names;
    }

    @Override
    public String classFactoryName() {
        // use mirror to avoid loading classes not generated yet
        return Mirrors.findAnnotationMirror(element(), Factory.class)
            .flatMap(Mirrors::findAnnotationValue)
            .map(value -> value.getValue().toString()).orElse(null);
    }
}
