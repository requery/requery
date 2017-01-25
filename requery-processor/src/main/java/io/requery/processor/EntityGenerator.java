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


import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.WildcardTypeName;
import io.requery.Entity;
import io.requery.Persistable;
import io.requery.PropertyNameStyle;
import io.requery.meta.Attribute;
import io.requery.proxy.EntityProxy;
import io.requery.proxy.PreInsertListener;
import io.requery.proxy.PropertyState;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.StringJoiner;
import java.util.stream.Collectors;

/**
 * Generates a java class file from an abstract class marked with the {@link Entity} annotation.
 * This class contains metadata about the table/column mapping that was annotated in the super
 * class. Also contains generated getter/setters for fields in the super class that will be
 * persisted.
 *
 * @author Nikhil Purushe
 */
class EntityGenerator extends EntityPartGenerator implements SourceGenerator {

    private final EntityDescriptor parent;
    private final Set<TypeGenerationExtension> typeExtensions;
    private final Set<PropertyGenerationExtension> memberExtensions;

    EntityGenerator(ProcessingEnvironment processingEnv,
                    EntityGraph graph,
                    EntityDescriptor entity,
                    EntityDescriptor parent) {
        super(processingEnv, graph, entity);
        this.parent = parent;
        typeExtensions = new HashSet<>();
        memberExtensions = new HashSet<>();
        // android extensions
        typeExtensions.add(new AndroidParcelableExtension(types));
        AndroidObservableExtension observable =
            new AndroidObservableExtension(entity, processingEnv);
        typeExtensions.add(observable);
        memberExtensions.add(observable);
    }

    @Override
    public void generate() throws IOException {
        ClassName entityTypeName = entity.isEmbedded() ?
            nameResolver.embeddedTypeNameOf(entity, parent) : typeName;
        TypeSpec.Builder builder = TypeSpec.classBuilder(entityTypeName)
            .addModifiers(Modifier.PUBLIC)
            .addOriginatingElement(typeElement);
        boolean metadataOnly = entity.isImmutable() || entity.isUnimplementable();
        if (typeElement.getKind().isInterface()) {
            builder.addSuperinterface(ClassName.get(typeElement));
            builder.addSuperinterface(ClassName.get(Persistable.class));

        } else if (!metadataOnly) {
            builder.superclass(ClassName.get(typeElement));
            builder.addSuperinterface(ClassName.get(Persistable.class));
        }
        CodeGeneration.addGeneratedAnnotation(processingEnv, builder);
        if (!entity.isEmbedded()) {
            EntityMetaGenerator meta = new EntityMetaGenerator(processingEnv, graph, entity);
            meta.generate(builder);
        }
        if (!metadataOnly) {
            generateConstructors(builder);
            generateMembers(builder);
            generateProxyMethods(builder);
            if (!entity.isEmbedded()) {
                generateEquals(builder);
                generateHashCode(builder);
                generateToString(builder);
            }
            if (entity.isCopyable()) {
                generateCopy(builder);
            }
        } else {
            // private constructor
            builder.addMethod(MethodSpec.constructorBuilder()
                   .addModifiers(Modifier.PRIVATE).build());
            generateMembers(builder); // members for builder if needed
            generateImmutableTypeBuildMethod(builder);
        }
        typeExtensions.forEach(extension -> extension.generate(entity, builder));
        CodeGeneration.writeType(processingEnv, typeName.packageName(), builder.build());
    }

    private void generateMembers(TypeSpec.Builder builder) {
        Modifier visibility = entity.isEmbedded() ? Modifier.PROTECTED : Modifier.PRIVATE;
        // generate property states
        if (!entity.isStateless()) {
            entity.attributes().values().stream()
                    .filter(attribute -> !attribute.isTransient())
                    .forEach(attribute -> {
                TypeName stateType = ClassName.get(PropertyState.class);
                builder.addField(FieldSpec
                        .builder(stateType, propertyStateFieldName(attribute), visibility)
                        .build());
            });
        }
        if (entity.isEmbedded() && !(entity.isImmutable() || entity.isUnimplementable())) {
            entity.attributes().values().stream()
                    .filter(attribute -> !attribute.isTransient())
                    .forEach(attribute -> {
                        ParameterizedTypeName attributeType = ParameterizedTypeName.get(
                                ClassName.get(Attribute.class), nameResolver.typeNameOf(parent),
                                resolveAttributeType(attribute));
                        builder.addField(FieldSpec
                                .builder(attributeType, attributeFieldName(attribute),
                                        Modifier.PRIVATE, Modifier.FINAL)
                                .build());
                    });
        }
        // only generate for interfaces or if the entity is immutable but has no builder
        boolean generateMembers = typeElement.getKind().isInterface() ||
            (entity.isImmutable() && !entity.builderType().isPresent());
        if (generateMembers) {
            for (Map.Entry<Element, ? extends AttributeDescriptor> entry :
                entity.attributes().entrySet()) {
                Element element = entry.getKey();
                AttributeDescriptor attribute = entry.getValue();
                if (element.getKind() == ElementKind.METHOD) {
                    ExecutableElement methodElement = (ExecutableElement) element;
                    TypeMirror typeMirror = methodElement.getReturnType();
                    TypeName fieldName;
                    if (attribute.isIterable()) {
                        fieldName = parameterizedCollectionName(typeMirror);
                    } else if (attribute.isOptional()) {
                        typeMirror = tryFirstTypeArgument(attribute.typeMirror());
                        fieldName = TypeName.get(typeMirror);
                    } else {
                        fieldName = nameResolver.tryGeneratedTypeName(typeMirror);
                    }
                    builder.addField(FieldSpec
                        .builder(fieldName, attribute.fieldName(), visibility)
                        .build());
                }
            }
        } else if (entity.isImmutable()) {
            TypeName builderName = entity.builderType().map(
                    element -> TypeName.get(element.asType())).orElse(typeName);
            builder.addField(initializeBuilder(entity, builderName, "builder"));

            entity.attributes().values().stream()
                .filter(AttributeDescriptor::isEmbedded)
                .forEach(attribute -> graph.embeddedDescriptorOf(attribute).ifPresent(embedded ->
                        embedded.builderType().ifPresent(type -> {
                    TypeName embedName = TypeName.get(type.asType());
                    String fieldName = attribute.fieldName() + "Builder";
                    builder.addField(initializeBuilder(embedded, embedName, fieldName));
                })));
        }
    }

    private FieldSpec initializeBuilder(EntityDescriptor entity, TypeName name, String fieldName) {
        FieldSpec.Builder builder = FieldSpec.builder(name, fieldName, Modifier.PROTECTED);
        if (entity.builderFactoryMethod().isPresent()) {
            TypeName baseName = TypeName.get(entity.element().asType());
            builder.initializer("$T.$L()", baseName, entity.builderFactoryMethod()
                            .map(method -> method.getSimpleName().toString()).orElse(""));
        } else {
            builder.initializer("new $T()", name);
        }
        return builder.build();
    }

    private void generateConstructors(TypeSpec.Builder builder) {
        // copy the existing constructors
        for (ExecutableElement constructor : ElementFilter.constructorsIn(
                typeElement.getEnclosedElements())) {
            // constructor params
            List<? extends VariableElement> parameters = constructor.getParameters();

            if (!parameters.isEmpty()) {

                MethodSpec.Builder constructorBuilder = MethodSpec.constructorBuilder();
                constructorBuilder.addModifiers(constructor.getModifiers());

                List<String> parameterNames = new ArrayList<>();
                for (VariableElement parameter : parameters) {
                    Modifier[] modifiers = parameter.getModifiers().toArray(
                            new Modifier[parameter.getModifiers().size()]);
                    String parameterName = parameter.getSimpleName().toString();
                    parameterNames.add(parameterName);
                    ParameterSpec.Builder parameterBuilder = ParameterSpec.builder(
                            TypeName.get(parameter.asType()), parameterName, modifiers);
                    constructorBuilder.addParameter(parameterBuilder.build());
                }
                // super parameter/arguments
                StringJoiner joiner = new StringJoiner(",", "(", ")");
                parameterNames.forEach(joiner::add);
                constructorBuilder.addStatement("super" + joiner.toString());
                builder.addMethod(constructorBuilder.build());
            }
        }
    }

    private void generateProxyMethods(TypeSpec.Builder builder) {
        // add proxy field
        TypeName entityType = entity.isEmbedded() ? nameResolver.typeNameOf(parent) : typeName;
        TypeName proxyName = parameterizedTypeName(EntityProxy.class, entityType);
        FieldSpec.Builder proxyField = FieldSpec.builder(proxyName, PROXY_NAME,
                Modifier.PRIVATE, Modifier.FINAL, Modifier.TRANSIENT);
        if (!entity.isEmbedded()) {
            proxyField.initializer("new $T(this, $L)", proxyName, TYPE_NAME);
        }
        builder.addField(proxyField.build());

        for (AttributeDescriptor attribute : entity.attributes().values()) {

            boolean useField = attribute.isTransient() || attribute.isEmbedded();
            TypeMirror typeMirror = attribute.typeMirror();
            TypeName unboxedTypeName;
            if (attribute.isIterable()) {
                unboxedTypeName = parameterizedCollectionName(typeMirror);
            } else if (attribute.isOptional()) {
                unboxedTypeName = TypeName.get(tryFirstTypeArgument(attribute.typeMirror()));
            } else if (attribute.isEmbedded()) {
                EntityDescriptor embedded = graph.embeddedDescriptorOf(attribute)
                    .orElseThrow(IllegalStateException::new);
                unboxedTypeName = nameResolver.embeddedTypeNameOf(embedded, entity);
            } else {
                unboxedTypeName = nameResolver.tryGeneratedTypeName(typeMirror);
            }

            String attributeName = attribute.fieldName();
            String getterName = attribute.getterName();
            String fieldName = Names.upperCaseUnderscore(Names.removeMemberPrefixes(attributeName));
            if (entity.isEmbedded()) {
                fieldName = attributeFieldName(attribute);
            }
            // getter
            MethodSpec.Builder getter = MethodSpec.methodBuilder(getterName)
                    .addModifiers(Modifier.PUBLIC)
                    .returns(attribute.isOptional() ? TypeName.get(typeMirror) : unboxedTypeName);
            if (Mirrors.overridesMethod(types, typeElement, getterName)) {
                getter.addAnnotation(Override.class);
            }
            memberExtensions.forEach(extension -> extension.addToGetter(attribute, getter));
            if (useField) {
                if (attribute.isEmbedded()) {
                    // have to cast to embedded type
                    getter.addStatement("return ($T)this.$L", unboxedTypeName, attributeName);
                } else {
                    getter.addStatement("return this.$L", attributeName);
                }
            } else if (attribute.isOptional()) {
                getter.addStatement("return $T.ofNullable($L.get($L))",
                    Optional.class, PROXY_NAME, fieldName);
            } else {
                getter.addStatement("return $L.get($L)", PROXY_NAME, fieldName);
            }
            builder.addMethod(getter.build());

            // setter
            String setterName = attribute.setterName();
            // if read only don't generate a public setter
            boolean readOnly = entity.isReadOnly() || attribute.isReadOnly();
            // edge case check if it's interface and we need to implement the setter
            if (entity.element().getKind().isInterface() &&
                ElementFilter.methodsIn(entity.element().getEnclosedElements()).stream()
                .anyMatch(element -> element.getSimpleName().toString().equals(setterName))) {
                readOnly = false;
            }
            if (!readOnly) {

                TypeName setTypeName = unboxedTypeName;
                boolean castType = false;

                // use wildcard generic collection type if necessary
                if (setTypeName instanceof ParameterizedTypeName) {
                    ParameterizedTypeName parameterizedName = (ParameterizedTypeName) setTypeName;
                    List<TypeName> arguments = parameterizedName.typeArguments;
                    List<TypeName> wildcards = new ArrayList<>();
                    for (TypeName argument : arguments) {
                        if (!(argument instanceof WildcardTypeName)) {
                            Elements elements = processingEnv.getElementUtils();
                            TypeElement element = elements.getTypeElement(argument.toString());
                            if (element != null && element.getKind() == ElementKind.INTERFACE) {
                                wildcards.add(WildcardTypeName.subtypeOf(argument));
                            } else {
                                wildcards.add(argument);
                            }
                        } else {
                            wildcards.add(argument);
                        }
                    }
                    TypeName[] array = new TypeName[wildcards.size()];
                    setTypeName = ParameterizedTypeName.get(parameterizedName.rawType,
                            wildcards.toArray(array));
                    castType = true;
                }

                String paramName = Names.lowerCaseFirst(Names.removeMemberPrefixes(attributeName));
                MethodSpec.Builder setter = MethodSpec.methodBuilder(setterName)
                        .addModifiers(Modifier.PUBLIC)
                        .addParameter(setTypeName, paramName);
                if (useField) {
                    setter.addStatement("this.$L = $L", attributeName, paramName);
                } else {
                    if (castType) {
                        setter.addAnnotation(AnnotationSpec.builder(SuppressWarnings.class)
                                .addMember("value", "$S", "unchecked").build());
                        setter.addStatement("$L.set($L, ($T)$L)",
                                PROXY_NAME, fieldName, unboxedTypeName, paramName);
                    } else {
                        setter.addStatement("$L.set($L, $L)", PROXY_NAME, fieldName, paramName);
                    }
                }
                memberExtensions.forEach(extension -> extension.addToSetter(attribute, setter));

                PropertyNameStyle style = entity.propertyNameStyle();
                if (style == PropertyNameStyle.FLUENT || style == PropertyNameStyle.FLUENT_BEAN) {
                    setter.addStatement("return this");
                    setter.returns(typeName);
                }
                builder.addMethod(setter.build());
            }
        }

        MethodSpec.Builder constructor = MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC);
        if (entity.isEmbedded()) {
            constructor.addParameter(ParameterSpec.builder(proxyName, "proxy").build());
            constructor.addStatement("this.$L = proxy", PROXY_NAME);
            entity.attributes().values().stream()
                    .filter(attribute -> !attribute.isTransient())
                    .forEach(attribute -> {
                        ParameterizedTypeName attributeType = ParameterizedTypeName.get(
                                ClassName.get(Attribute.class), nameResolver.typeNameOf(parent),
                                resolveAttributeType(attribute));
                        constructor.addParameter(ParameterSpec
                                .builder(attributeType, attribute.name()).build());
                        constructor.addStatement("this.$L = $L",
                                attributeFieldName(attribute), attribute.name());
                    });
        }
        generateListeners(constructor);
        // initialize the generated embedded entities
        entity.attributes().values().stream()
            .filter(AttributeDescriptor::isEmbedded)
            .forEach(attribute -> graph.embeddedDescriptorOf(attribute).ifPresent(embedded -> {
                ClassName embeddedName = nameResolver.embeddedTypeNameOf(embedded, entity);
                String format = embedded.attributes().values().stream().map(attr ->
                        Names.upperCaseUnderscore(embeddedAttributeName(attribute, attr)))
                        .collect(Collectors.joining(", ", "$L = new $T($L, ", ")"));
                constructor.addStatement(format, attribute.fieldName(), embeddedName, PROXY_NAME);
            }));
        builder.addMethod(constructor.build());
    }

    private void generateEquals(TypeSpec.Builder builder) {
        boolean overridesEquals = Mirrors.overridesMethod(types, typeElement, "equals");
        if (!overridesEquals) {
            MethodSpec.Builder equals = CodeGeneration.overridePublicMethod("equals")
                .addParameter(TypeName.OBJECT, "obj")
                .returns(TypeName.BOOLEAN)
                .addStatement("return obj instanceof $T && (($T)obj).$L.equals(this.$L)",
                        typeName, typeName, PROXY_NAME, PROXY_NAME);
            builder.addMethod(equals.build());
        }
    }

    private void generateHashCode(TypeSpec.Builder builder) {
        if (!Mirrors.overridesMethod(types, typeElement, "hashCode")) {
            MethodSpec.Builder hashCode = CodeGeneration.overridePublicMethod("hashCode")
                    .returns(TypeName.INT)
                    .addStatement("return $L.hashCode()", PROXY_NAME);
            builder.addMethod(hashCode.build());
        }
    }

    private void generateToString(TypeSpec.Builder builder) {
        if (!Mirrors.overridesMethod(types, typeElement, "toString")) {
            MethodSpec.Builder equals = CodeGeneration.overridePublicMethod("toString")
                    .returns(String.class)
                    .addStatement("return $L.toString()", PROXY_NAME);
            builder.addMethod(equals.build());
        }
    }

    private void generateCopy(TypeSpec.Builder builder) {
        if (!Mirrors.overridesMethod(types, typeElement, "copy")) {
            MethodSpec.Builder copy = MethodSpec.methodBuilder("copy")
                    .addModifiers(Modifier.PUBLIC)
                    .returns(typeName)
                    .addStatement("return $L.copy()", PROXY_NAME);
            builder.addMethod(copy.build());
        }
    }

    private void generateListeners(MethodSpec.Builder constructor) {
        for (Map.Entry<Element, ? extends ListenerDescriptor> entry :
            entity.listeners().entrySet()) {

            for (Annotation annotation : entry.getValue().listenerAnnotations()) {
                String annotationName = annotation.annotationType().getSimpleName()
                    .replace("Persist", "Insert").replace("Remove", "Delete");
                String methodName = Names.lowerCaseFirst(annotationName);
                // avoid hardcoding the package name here
                Element listener = elements.getTypeElement(
                        PreInsertListener.class.getCanonicalName());
                PackageElement packageElement = elements.getPackageOf(listener);

                // generate the listener name
                String packageName = packageElement.getQualifiedName().toString();
                ClassName listenerName = ClassName.get(packageName, annotationName + "Listener");
                ParameterizedTypeName getterType =
                    ParameterizedTypeName.get(listenerName, typeName);

                TypeSpec.Builder listenerBuilder = TypeSpec.anonymousClassBuilder("")
                    .addSuperinterface(getterType)
                    .addMethod(CodeGeneration.overridePublicMethod(methodName)
                            .addParameter(typeName, "entity")
                            .addStatement("$L()", entry.getKey().getSimpleName())
                            .build());

                constructor.addStatement("$L.modifyListeners().add$L($L)", PROXY_NAME,
                        annotationName + "Listener", listenerBuilder.build());
            }
        }
    }

    private void generateImmutableTypeBuildMethod(TypeSpec.Builder builder) {
        if (entity.isImmutable() && !entity.builderType().isPresent() &&
            entity.factoryMethod().isPresent()) {

            String methodName = entity.factoryMethod()
                .map(element -> element.getSimpleName().toString()).orElse("");
            List<String> argumentNames = entity.factoryArguments();

            StringJoiner joiner = new StringJoiner(",");
            argumentNames.forEach(name -> joiner.add("$L"));
            MethodSpec.Builder build = MethodSpec.methodBuilder("build")
                .returns(ClassName.get(entity.element()));
            if (methodName.equals("<init>")) {
                Object[] args = new Object[1 + argumentNames.size()];
                args[0] = ClassName.get(entity.element());
                System.arraycopy(argumentNames.toArray(), 0, args, 1, argumentNames.size());
                build.addStatement("return new $T(" + joiner.toString() + ")", args).build();
            } else {
                Object[] args = new Object[2 + argumentNames.size()];
                args[0] = ClassName.get(entity.element());
                args[1] = methodName;
                System.arraycopy(argumentNames.toArray(), 0, args, 2, argumentNames.size());
                build.addStatement("return $T.$L(" + joiner.toString() + ")", args).build();
            }
            builder.addMethod(build.build());
        }
    }
}
