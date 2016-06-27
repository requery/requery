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

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import io.requery.CascadeAction;
import io.requery.Entity;
import io.requery.Persistable;
import io.requery.PropertyNameStyle;
import io.requery.ReferentialAction;
import io.requery.meta.Attribute;
import io.requery.meta.Cardinality;
import io.requery.meta.QueryAttribute;
import io.requery.meta.QueryExpression;
import io.requery.meta.Type;
import io.requery.meta.TypeBuilder;
import io.requery.proxy.BooleanProperty;
import io.requery.proxy.ByteProperty;
import io.requery.proxy.DoubleProperty;
import io.requery.proxy.EntityProxy;
import io.requery.proxy.FloatProperty;
import io.requery.proxy.IntProperty;
import io.requery.proxy.LongProperty;
import io.requery.proxy.PreInsertListener;
import io.requery.proxy.Property;
import io.requery.proxy.PropertyState;
import io.requery.proxy.ShortProperty;
import io.requery.query.Order;
import io.requery.util.function.Function;
import io.requery.util.function.Supplier;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.StringJoiner;

/**
 * Generates a java class file from an abstract class marked with the {@link Entity} annotation.
 * This class contains metadata about the table/column mapping that was annotated in the super
 * class. Also contains generated getter/setters for fields in the super class that will be
 * persisted.
 *
 * @author Nikhil Purushe
 */
class EntityGenerator implements SourceGenerator {

    private static final String KOTLIN_ATTRIBUTE_DELEGATE = "io.requery.meta.AttributeDelegate";
    private static final String PROXY_NAME = "$proxy";
    static final String TYPE_NAME = "$TYPE";

    private final EntityDescriptor entity;
    private final EntityDescriptor parent;
    private final ProcessingEnvironment processingEnvironment;
    private final Elements elements;
    private final Types types;
    private final HashSet<String> attributeNames;
    private final HashSet<String> expressionNames;
    private final TypeElement typeElement;
    private final ClassName typeName;
    private final EntityGraph graph;
    private final EntityNameResolver nameResolver;
    private final Set<TypeGenerationExtension> typeExtensions;
    private final Set<PropertyGenerationExtension> memberExtensions;

    EntityGenerator(ProcessingEnvironment processingEnvironment,
                    EntityGraph graph,
                    EntityDescriptor entity,
                    EntityDescriptor parent) {
        this.processingEnvironment = processingEnvironment;
        this.graph = graph;
        this.entity = entity;
        this.parent = parent;
        this.elements = processingEnvironment.getElementUtils();
        this.types = processingEnvironment.getTypeUtils();
        nameResolver = new EntityNameResolver(graph);
        attributeNames = new HashSet<>();
        expressionNames = new HashSet<>();
        typeElement = entity.element();
        typeName = nameResolver.typeNameOf(entity);
        typeExtensions = new HashSet<>();
        memberExtensions = new HashSet<>();
        // android extensions
        typeExtensions.add(new AndroidParcelableExtension(types));
        AndroidObservableExtension observable =
            new AndroidObservableExtension(entity, processingEnvironment);
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
        CodeGeneration.addGeneratedAnnotation(processingEnvironment, builder);
        if (!entity.isEmbedded()) {
            generateStaticMetadata(builder, metadataOnly);
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
        } else {
            // private constructor
            builder.addMethod(MethodSpec.constructorBuilder()
                   .addModifiers(Modifier.PRIVATE).build());
            generateMembers(builder); // members for builder if needed
            generateImmutableTypeBuildMethod(builder);
        }
        typeExtensions.forEach(extension -> extension.generate(entity, builder));
        CodeGeneration.writeType(processingEnvironment, typeName.packageName(), builder.build());
    }

    private void generateMembers(TypeSpec.Builder builder) {
        Modifier memberVisibility = entity.isEmbedded() ? Modifier.PROTECTED : Modifier.PRIVATE;
        // generate property states
        if (!entity.isStateless()) {
            for (AttributeDescriptor attribute : entity.attributes().values()) {
                TypeName stateType = ClassName.get(PropertyState.class);
                builder.addField(FieldSpec
                    .builder(stateType, propertyStateFieldName(attribute), memberVisibility)
                    .build());
            }
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
                    TypeName fieldTypeName;
                    if (attribute.isIterable()) {
                        fieldTypeName = parameterizedCollectionName(typeMirror);
                    } else if (attribute.isOptional()) {
                        typeMirror = tryFirstTypeArgument(attribute.typeMirror());
                        fieldTypeName = TypeName.get(typeMirror);
                    } else {
                        fieldTypeName = nameResolver.tryGeneratedTypeName(typeMirror);
                    }
                    builder.addField(FieldSpec
                        .builder(fieldTypeName, attribute.fieldName(), memberVisibility)
                        .build());
                }
            }
        }
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
                fieldName = nameResolver.typeNameOf(parent) + "." + fieldName;
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
                String paramName = Names.lowerCaseFirst(Names.removeMemberPrefixes(attributeName));
                MethodSpec.Builder setter = MethodSpec.methodBuilder(setterName)
                        .addModifiers(Modifier.PUBLIC)
                        .addParameter(unboxedTypeName, paramName);
                if (useField) {
                    setter.addStatement("this.$L = $L", attributeName, paramName);
                } else {
                    setter.addStatement("$L.set($L, $L)", PROXY_NAME, fieldName, paramName);
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
        }
        generateListeners(constructor);
        // initialize the generated embedded entities
        entity.attributes().values().stream()
            .filter(AttributeDescriptor::isEmbedded)
            .forEach(attribute -> graph.embeddedDescriptorOf(attribute).ifPresent(embedded -> {
                ClassName embeddedName = nameResolver.embeddedTypeNameOf(embedded, entity);
                constructor.addStatement("$L = new $T($L)",
                    attribute.fieldName(), embeddedName, PROXY_NAME);
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

    private void generateType(TypeSpec.Builder builder, TypeName targetName) {

        CodeBlock.Builder block = CodeBlock.builder().add("new $T<$T>($T.class, $S)\n",
            TypeBuilder.class, targetName, targetName, entity.tableName());

        block.add(".setBaseType($T.class)\n", ClassName.get(typeElement))
            .add(".setCacheable($L)\n", entity.isCacheable())
            .add(".setImmutable($L)\n", entity.isImmutable())
            .add(".setReadOnly($L)\n", entity.isReadOnly())
            .add(".setStateless($L)\n", entity.isStateless());
        String factoryName = entity.classFactoryName();
        if (!Names.isEmpty(factoryName)) {
            block.add(".setFactory(new $L())\n", ClassName.bestGuess(factoryName));
        } else if (entity.isImmutable()) {

            // the builder name (if there is no builder than this class is the builder)
            TypeName builderName = entity.builderType().map(
                element -> TypeName.get(element.asType())).orElse(typeName);

            TypeSpec.Builder typeFactory = TypeSpec.anonymousClassBuilder("")
                .addSuperinterface(parameterizedTypeName(Supplier.class, builderName));
            MethodSpec.Builder buildMethod =
                CodeGeneration.overridePublicMethod("get").returns(builderName);

            if (entity.builderFactoryMethod().isPresent()) {
                buildMethod.addStatement("return $T.$L()", targetName,
                    entity.builderFactoryMethod()
                        .map(method -> method.getSimpleName().toString()).orElse(""));
            } else {
                buildMethod.addStatement("return new $T()", builderName);
            }
            typeFactory.addMethod(buildMethod.build());
            block.add(".setBuilderFactory($L)\n", typeFactory.build());

            TypeSpec.Builder buildFunction = TypeSpec.anonymousClassBuilder("")
                .addSuperinterface(parameterizedTypeName(Function.class, builderName, targetName))
                .addMethod(
                    CodeGeneration.overridePublicMethod("apply")
                        .addParameter(builderName, "value")
                        .addStatement("return value.build()")
                        .returns(targetName)
                        .build());
            block.add(".setBuilderFunction($L)\n", buildFunction.build());
        } else {
            TypeSpec.Builder typeFactory = TypeSpec.anonymousClassBuilder("")
                .addSuperinterface(parameterizedTypeName(Supplier.class, targetName))
                .addMethod(
                    CodeGeneration.overridePublicMethod("get")
                        .addStatement("return new $T()", targetName)
                        .returns(targetName)
                        .build());
            block.add(".setFactory($L)\n", typeFactory.build());
        }

        ParameterizedTypeName proxyType = parameterizedTypeName(EntityProxy.class, targetName);
        TypeSpec.Builder proxyProvider = TypeSpec.anonymousClassBuilder("")
                .addSuperinterface(parameterizedTypeName(Function.class, targetName, proxyType));
        MethodSpec.Builder proxyFunction = CodeGeneration.overridePublicMethod("apply")
            .addParameter(targetName, "entity")
            .returns(proxyType);
        if (entity.isImmutable() || entity.isUnimplementable()) {
            proxyFunction.addStatement("return new $T(entity, $L)", proxyType, TYPE_NAME);
        } else {
            proxyFunction.addStatement("return entity.$L", PROXY_NAME);
        }
        proxyProvider.addMethod(proxyFunction.build());

        block.add(".setProxyProvider($L)\n", proxyProvider.build());

        if (entity.tableAttributes() != null && entity.tableAttributes().length > 0) {
            StringJoiner joiner = new StringJoiner(",", "new String[] {", "}");
            for (String attribute : entity.tableAttributes()) {
                joiner.add("\"" + attribute + "\"");
            }
            block.add(".setTableCreateAttributes($L)\n", joiner.toString());
        }
        attributeNames.forEach(name -> block.add(".addAttribute($L)\n", name));
        expressionNames.forEach(name -> block.add(".addExpression($L)\n", name));
        block.add(".build()");
        ParameterizedTypeName type = parameterizedTypeName(Type.class, targetName);
        builder.addField(
            FieldSpec.builder(type, TYPE_NAME,
                Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                .initializer("$L", block.build())
                .build());
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

    private void generateStaticMetadata(TypeSpec.Builder builder, boolean metadataOnly) {
        TypeName targetName = metadataOnly? ClassName.get(entity.element()) : typeName;

        entity.attributes().values().stream()
            .filter(attribute -> !attribute.isTransient())
            .forEach(attribute -> {

            String fieldName = Names.upperCaseUnderscore(
                Names.removeMemberPrefixes(attribute.fieldName()));

            if (attribute.isForeignKey() && attribute.cardinality() != null) {
                // generate a foreign key attribute for use in queries but not stored in the type
                graph.referencingEntity(attribute)
                    .flatMap(entity -> graph.referencingAttribute(attribute, entity))
                    .ifPresent(foreignKey -> {
                        String name = fieldName + "_ID";
                        TypeMirror mirror = foreignKey.typeMirror();
                        builder.addField(
                            generateAttribute(attribute, targetName, name, mirror, true, null) );
                        expressionNames.add(name);
                    });
            }
            if (attribute.isEmbedded()) {
                graph.embeddedDescriptorOf(attribute).ifPresent(embedded ->
                    generateEmbedded(attribute, embedded, builder, targetName));
            } else {
                TypeMirror mirror = attribute.typeMirror();
                builder.addField(
                    generateAttribute(attribute, targetName, fieldName, mirror, false, null) );
                attributeNames.add(fieldName);
            }
        });
        generateType(builder, targetName);
    }

    private void generateEmbedded(AttributeDescriptor parent,
                                  EntityDescriptor embedded,
                                  TypeSpec.Builder builder,
                                  TypeName targetName) {
        // generate the embedded attributes into this type
        embedded.attributes().values().forEach(attribute -> {
            String fieldName = Names.upperCaseUnderscore(
                Names.removeMemberPrefixes(attribute.fieldName()));
            TypeMirror mirror = attribute.typeMirror();
            builder.addField(
                generateAttribute(attribute, targetName, fieldName, mirror, false, parent));
            attributeNames.add(fieldName);
        });
        // generate an embedded implementation for this (the parent) entity
        try {
            new EntityGenerator(processingEnvironment, graph, embedded, entity).generate();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private FieldSpec generateAttribute(AttributeDescriptor attribute,
                                        TypeName targetName,
                                        String fieldName,
                                        TypeMirror mirror,
                                        boolean expression,
                                        AttributeDescriptor parent) {
        TypeMirror typeMirror = mirror;
        TypeName typeName;
        if (attribute.isIterable()) {
            typeMirror = tryFirstTypeArgument(typeMirror);
            typeName = parameterizedCollectionName(attribute.typeMirror());
        } else if (attribute.isOptional()) {
            typeMirror = tryFirstTypeArgument(typeMirror);
            typeName = TypeName.get(typeMirror);
        } else {
            typeName = nameResolver.generatedTypeNameOf(typeMirror).orElse(null);
        }
        if (typeName == null) {
            typeName = boxedTypeName(typeMirror);
        }
        ParameterizedTypeName type;
        ClassName attributeType = null;
        boolean useKotlinDelegate = false;
        if (expression) {
            type = parameterizedTypeName(QueryExpression.class, typeName);
        } else {
            // if it's an association don't make it available as a query attribute
            boolean isQueryable = attribute.cardinality() == null || attribute.isForeignKey();
            Class<?> attributeClass = isQueryable ? QueryAttribute.class : Attribute.class;
            attributeType = ClassName.get(attributeClass);
            if (isQueryable && SourceLanguage.of(entity.element()) == SourceLanguage.KOTLIN) {
                TypeElement delegateType = elements.getTypeElement(KOTLIN_ATTRIBUTE_DELEGATE);
                if (delegateType != null) {
                    attributeType = ClassName.get(delegateType);
                    useKotlinDelegate = true;
                }
            }
            type = ParameterizedTypeName.get(attributeType, targetName, typeName);
        }

        CodeBlock.Builder builder = CodeBlock.builder();

        if (attribute.isIterable()) {
            typeMirror = tryFirstTypeArgument(typeMirror);
            TypeName name = nameResolver.tryGeneratedTypeName(typeMirror);
            TypeElement collectionElement = (TypeElement) types.asElement(attribute.typeMirror());

            ParameterizedTypeName builderName = parameterizedTypeName(
                attribute.builderClass(), targetName, typeName, name);

            builder.add("\nnew $T($S, $T.class, $T.class)\n",
                builderName, attribute.name(), ClassName.get(collectionElement), name);

        } else if (attribute.isMap()) {
            List<TypeMirror> parameters = Mirrors.listGenericTypeArguments(typeMirror);
            // key type
            TypeName keyName = TypeName.get(parameters.get(0));
            // value type
            typeMirror = parameters.get(1);
            TypeName valueName = nameResolver.tryGeneratedTypeName(typeMirror);
            TypeElement valueElement = (TypeElement) types.asElement(attribute.typeMirror());
            ParameterizedTypeName builderName = parameterizedTypeName(
                attribute.builderClass(), targetName, typeName, keyName, valueName);

            builder.add("\nnew $T($S, $T.class, $T.class, $T.class)\n", builderName,
                attribute.name(), ClassName.get(valueElement), keyName, valueName);
        } else {
            ParameterizedTypeName builderName = parameterizedTypeName(
                attribute.builderClass(), targetName, typeName);
            TypeName classType = typeName;
            if (typeMirror.getKind().isPrimitive()) {
                // if primitive just use the primitive class not the boxed version
                classType = TypeName.get(typeMirror);
            }
            String statement;
            if (Mirrors.listGenericTypeArguments(typeMirror).size() > 0) {
                // use the erased type and cast to class
                classType = TypeName.get(types.erasure(typeMirror));
                statement = "\nnew $T($S, (Class)$T.class)\n";
            } else {
                statement ="\nnew $T($S, $T.class)\n";
            }
            builder.add(statement, builderName, attribute.name(), classType);
        }
        if (!expression) {
            String prefix = "";
            if (parent != null) {
                prefix = parent.getterName() + "().";
            }
            generateProperties(attribute, typeMirror, targetName, typeName, builder, prefix);
        }
        // attribute builder properties
        if (attribute.isKey()) {
            builder.add(".setKey(true)\n");
        }
        builder.add(".setGenerated($L)\n", attribute.isGenerated());
        builder.add(".setLazy($L)\n", attribute.isLazy());
        builder.add(".setNullable($L)\n", attribute.isNullable());
        builder.add(".setUnique($L)\n", attribute.isUnique());
        if (!Names.isEmpty(attribute.defaultValue())) {
            builder.add(".setDefaultValue($S)\n", attribute.defaultValue());
        }
        if (!Names.isEmpty(attribute.collate())) {
            builder.add(".setCollate($S)\n", attribute.collate());
        }
        if (attribute.columnLength() != null) {
            builder.add(".setLength($L)\n", attribute.columnLength());
        }
        if (attribute.isVersion()) {
            builder.add(".setVersion($L)\n", attribute.isVersion());
        }
        if (attribute.converterName() != null) {
            builder.add(".setConverter(new $L())\n", attribute.converterName());
        }
        if (attribute.isForeignKey()) {
            builder.add(".setForeignKey($L)\n", attribute.isForeignKey());

            Optional<EntityDescriptor> referencedType = graph.referencingEntity(attribute);
            referencedType.ifPresent(referenced -> {

                builder.add(".setReferencedClass($T.class)\n", referenced.isImmutable() ?
                        TypeName.get(referenced.element().asType()) :
                        nameResolver.typeNameOf(referenced));

                graph.referencingAttribute(attribute, referenced).ifPresent(
                    referencedAttribute -> {
                        String name = Names.upperCaseUnderscore(referencedAttribute.fieldName());
                        TypeSpec provider = CodeGeneration.createAnonymousSupplier(
                            ClassName.get(Attribute.class),
                            CodeBlock.builder().addStatement("return $T.$L",
                                nameResolver.typeNameOf(referenced), name).build());

                        builder.add(".setReferencedAttribute($L)\n", provider);
                    });
            });
        }
        if (attribute.isIndexed()) {
            builder.add(".setIndexed($L)\n", attribute.isIndexed());
            if (!attribute.indexNames().isEmpty()) {
                StringJoiner joiner = new StringJoiner(",");
                attribute.indexNames().forEach(name -> joiner.add("$S"));
                builder.add(".setIndexNames(" + joiner + ")\n", attribute.indexNames().toArray());
            }
        }
        if (attribute.deleteAction() != null) {
            builder.add(".setDeleteAction($T.$L)\n",
                ClassName.get(ReferentialAction.class), attribute.deleteAction());
        }
        if (attribute.updateAction() != null) {
            builder.add(".setUpdateAction($T.$L)\n",
                ClassName.get(ReferentialAction.class), attribute.updateAction());
        }
        if (!attribute.cascadeActions().isEmpty()) {
            StringJoiner joiner = new StringJoiner(",");
            attribute.cascadeActions().forEach(action -> joiner.add("$T.$L"));
            int index = 0;
            ClassName cascadeClass = ClassName.get(CascadeAction.class);
            Object[] args = new Object[attribute.cascadeActions().size()*2];
            for (CascadeAction action : attribute.cascadeActions()) {
                args[index++] = cascadeClass;
                args[index++] = action;
            }
            builder.add(".setCascadeAction(" + joiner +  ")\n", args);
        }
        if (attribute.cardinality() != null) {
            if (!expression) {
                builder.add(".setCardinality($T.$L)\n",
                        ClassName.get(Cardinality.class), attribute.cardinality());
            }

            graph.referencingEntity(attribute).ifPresent(referenced -> {
                Set<AttributeDescriptor> mappings =
                    graph.mappedAttributes(entity, attribute, referenced);

                if (attribute.cardinality() == Cardinality.MANY_TO_MANY) {
                    generateJunctionType(attribute, referenced, mappings)
                        .ifPresent(name -> builder.add(".setReferencedClass($T.class)\n", name));
                }
                if (mappings.size() == 1) {
                    AttributeDescriptor mapped = mappings.iterator().next();
                    String staticMemberName = Names.upperCaseUnderscore(mapped.fieldName());

                    TypeSpec provider = CodeGeneration.createAnonymousSupplier(
                        ClassName.get(Attribute.class),
                        CodeBlock.builder().addStatement("return $T.$L",
                            nameResolver.typeNameOf(referenced), staticMemberName).build());
                    builder.add(".setMappedAttribute($L)\n", provider);
                }

                if (attribute.orderBy() != null) {
                    referenced.attributes().values().stream()
                        .filter(entry -> entry.name().equals(attribute.orderBy()))
                        .findFirst().ifPresent(orderBy -> {

                        String staticMemberName = Names.upperCaseUnderscore(orderBy.fieldName());
                        TypeSpec provider = CodeGeneration.createAnonymousSupplier(
                            ClassName.get(Attribute.class),
                            CodeBlock.builder().addStatement("return $T.$L",
                                nameResolver.typeNameOf(referenced), staticMemberName).build());
                        builder.add(".setOrderByAttribute($L)\n", provider);
                        builder.add(".setOrderByDirection($T.$L)\n",
                            ClassName.get(Order.class), attribute.orderByDirection());
                    });
                }
            });
        }
        builder.add(".build()");
        FieldSpec.Builder field = FieldSpec.builder(type, fieldName,
            Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL);
        if (useKotlinDelegate) {
            return field.initializer("new $T($L)", attributeType, builder.build()).build();
        } else {
            return field.initializer("$L", builder.build()).build();
        }
    }

    private Optional<TypeName> generateJunctionType(AttributeDescriptor attribute,
                                                    EntityDescriptor referenced,
                                                    Set<AttributeDescriptor> mappings) {
        TypeName typeName = null;
        Optional<AssociativeEntityDescriptor> joinEntity = attribute.associativeEntity();
        if (joinEntity.isPresent()) {
            AssociativeEntityDescriptor descriptor = joinEntity.get();
            Optional<TypeMirror> mirror = descriptor.type();
            if (mirror.isPresent()) {
                typeName = TypeName.get(mirror.get());
            } else {
                // generate a special type for the junction table (with attributes)
                graph.referencingEntity(attribute).ifPresent(referencing -> {
                    JoinEntityGenerator generator = new JoinEntityGenerator(
                            processingEnvironment, nameResolver, entity, referencing, attribute);
                    try {
                        generator.generate();
                    } catch (IOException e) {
                        processingEnvironment.getMessager()
                                .printMessage(Diagnostic.Kind.ERROR, e.toString());
                        throw new RuntimeException(e);
                    }
                });
                typeName = nameResolver.joinEntityName(descriptor, entity, referenced);
            }
        } else if (mappings.size() == 1) {
            AttributeDescriptor mapped = mappings.iterator().next();
            return mapped.associativeEntity()
                .map(e -> nameResolver.joinEntityName(e, referenced, entity));
        }
        return Optional.ofNullable(typeName);
    }

    private void generateProperties(AttributeDescriptor attribute,
                                    TypeMirror typeMirror,
                                    TypeName targetName,
                                    TypeName attributeName,
                                    CodeBlock.Builder builder,
                                    String accessPrefix) {
        // boxed get/set using Objects
        Class propertyClass = propertyClassFor(typeMirror);
        ParameterizedTypeName propertyType = propertyName(propertyClass, targetName, attributeName);

        TypeSpec.Builder propertyBuilder = TypeSpec.anonymousClassBuilder("")
            .addSuperinterface(propertyType);

        boolean isNullable = typeMirror.getKind().isPrimitive() && attribute.isNullable();
        boolean useGetter = entity.isUnimplementable() || entity.isImmutable();
        boolean useSetter = entity.isUnimplementable();
        String getName = accessPrefix + (useGetter? attribute.getterName() : attribute.fieldName());
        String setName = accessPrefix + (useSetter? attribute.setterName() : attribute.fieldName());
        new GeneratedProperty(getName, setName, targetName, attributeName)
                .setNullable(isNullable)
                .setReadOnly(entity.isImmutable())
                .setUseMethod(useGetter)
                .build(propertyBuilder);

        // additional primitive get/set if the type is primitive
        if (propertyClass != Property.class) {
            TypeName primitiveType = TypeName.get(attribute.typeMirror());
            String name = Names.upperCaseFirst(attribute.typeMirror().toString());

            new GeneratedProperty(getName, setName, targetName, primitiveType)
                .setSuffix(name)
                .setReadOnly(entity.isImmutable())
                .setUseMethod(useGetter)
                .build(propertyBuilder);
        }
        builder.add(".setProperty($L)\n", propertyBuilder.build());
        builder.add(".setPropertyName($S)\n", attribute.element().getSimpleName());

        // property state get/set
        if (!entity.isStateless()) {
            ClassName stateClass = ClassName.get(PropertyState.class);
            TypeSpec.Builder stateType = TypeSpec.anonymousClassBuilder("")
                .addSuperinterface(parameterizedTypeName(Property.class, targetName, stateClass));
            String fieldName = accessPrefix + propertyStateFieldName(attribute);
            new GeneratedProperty(fieldName, targetName, stateClass).build(stateType);
            builder.add(".setPropertyState($L)\n", stateType.build());
        }

        // if immutable add setter for the builder
        if (entity.isImmutable()) {
            String propertyName = attribute.fieldName();
            TypeName builderName = typeName;
            useSetter = false;
            Optional<TypeElement> builderType = entity.builderType();
            if (builderType.isPresent()) {
                propertyName = attribute.setterName();
                builderName = TypeName.get(builderType.get().asType());
                useSetter = true;
                for (ExecutableElement method :
                    ElementFilter.methodsIn(builderType.get().getEnclosedElements())) {
                    List<? extends VariableElement> parameters = method.getParameters();
                    String name = Names.removeMethodPrefixes(method.getSimpleName());
                    // probable setter for this attribute
                    // (some builders have with<Property> setters so strip that
                    if ((name.startsWith("with") && name.length() > 4 &&
                        Character.isUpperCase(name.charAt(4))) ||
                        name.equalsIgnoreCase(attribute.fieldName()) && parameters.size() == 1) {
                        propertyName = method.getSimpleName().toString();
                        break;
                    }
                }
            }
            propertyType = propertyName(propertyClass, builderName, attributeName);
            TypeSpec.Builder builderProperty = TypeSpec.anonymousClassBuilder("")
                .addSuperinterface(propertyType);

            new GeneratedProperty(propertyName, builderName, attributeName)
                    .setWriteOnly(true)
                    .setUseMethod(useSetter)
                    .build(builderProperty);
            if (propertyClass != Property.class) {
                TypeName primitiveType = TypeName.get(attribute.typeMirror());
                String name = Names.upperCaseFirst(attribute.typeMirror().toString());
                new GeneratedProperty(propertyName, builderName, primitiveType)
                    .setSuffix(name)
                    .setUseMethod(useSetter)
                    .setWriteOnly(true)
                    .build(builderProperty);
            }
            builder.add(".setBuilderProperty($L)\n", builderProperty.build());
        }
    }

    private static ParameterizedTypeName propertyName(Class type,
                                                      TypeName targetName, TypeName fieldName) {
        return type == Property.class ?
            parameterizedTypeName(type, targetName, fieldName) :
            parameterizedTypeName(type, targetName);
    }

    private static Class propertyClassFor(TypeMirror typeMirror) {
        if (typeMirror.getKind().isPrimitive()) {
            switch (typeMirror.getKind()) {
                case BOOLEAN:
                    return BooleanProperty.class;
                case BYTE:
                    return ByteProperty.class;
                case SHORT:
                    return ShortProperty.class;
                case INT:
                    return IntProperty.class;
                case LONG:
                    return LongProperty.class;
                case FLOAT:
                    return FloatProperty.class;
                case DOUBLE:
                    return DoubleProperty.class;
            }
        }
        return Property.class;
    }

    private static String propertyStateFieldName(AttributeDescriptor attribute) {
        return "$" + attribute.fieldName() + "_state";
    }

    private TypeName boxedTypeName(TypeMirror typeMirror) {
        if (typeMirror.getKind().isPrimitive()) {
            return TypeName.get(types.boxedClass((PrimitiveType) typeMirror).asType());
        }
        return TypeName.get(typeMirror);
    }

    private ParameterizedTypeName parameterizedCollectionName(TypeMirror typeMirror) {
        TypeMirror genericType = tryFirstTypeArgument(typeMirror);
        TypeName elementName = nameResolver.tryGeneratedTypeName(genericType);
        TypeElement collectionElement = (TypeElement) types.asElement(typeMirror);
        ClassName collectionName = ClassName.get(collectionElement);
        return ParameterizedTypeName.get(collectionName, elementName);
    }

    private static TypeMirror tryFirstTypeArgument(TypeMirror typeMirror) {
        List<TypeMirror> args = Mirrors.listGenericTypeArguments(typeMirror);
        return args.isEmpty() ? typeMirror : args.get(0);
    }

    private static ParameterizedTypeName parameterizedTypeName(Class<?> rawType,
                                                               TypeName... typeArguments) {
        return ParameterizedTypeName.get(ClassName.get(rawType), typeArguments);
    }
}
