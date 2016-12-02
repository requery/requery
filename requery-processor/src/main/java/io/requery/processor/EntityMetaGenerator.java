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
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import io.requery.CascadeAction;
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
import io.requery.proxy.Property;
import io.requery.proxy.PropertyState;
import io.requery.proxy.ShortProperty;
import io.requery.query.Order;
import io.requery.util.function.Function;
import io.requery.util.function.Supplier;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.tools.Diagnostic;
import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.StringJoiner;

class EntityMetaGenerator extends EntityPartGenerator {

    private static final String KOTLIN_ATTRIBUTE_DELEGATE = "io.requery.meta.AttributeDelegate";

    private final HashSet<String> attributeNames;
    private final HashSet<String> expressionNames;

    EntityMetaGenerator(ProcessingEnvironment processingEnvironment,
                        EntityGraph graph,
                        EntityDescriptor entity) {
        super(processingEnvironment, graph, entity);
        attributeNames = new HashSet<>();
        expressionNames = new HashSet<>();
    }

    void generate(TypeSpec.Builder builder) {
        boolean metadataOnly = entity.isImmutable() || entity.isUnimplementable();
        TypeName targetName = metadataOnly? ClassName.get(entity.element()) : typeName;

        List<QualifiedName> generatedEmbeddedTypes = new LinkedList<>();
        entity.attributes().values().stream()
            .filter(attribute -> !attribute.isTransient())
            .forEach(attribute -> {

            String fieldName = upperCaseUnderscoreRemovePrefixes(attribute.fieldName());

            if (attribute.isForeignKey() && attribute.cardinality() != null) {
                // generate a foreign key attribute for use in queries but not stored in the type
                graph.referencingEntity(attribute)
                    .flatMap(entity -> graph.referencingAttribute(attribute, entity))
                    .ifPresent(foreignKey -> {
                        String name = fieldName + "_ID";
                        TypeMirror mirror = foreignKey.typeMirror();
                        builder.addField(
                            generateAttribute(attribute, null, targetName, name, mirror, true) );
                        expressionNames.add(name);
                    });
            }
            if (attribute.isEmbedded()) {
                graph.embeddedDescriptorOf(attribute).ifPresent(embedded -> {
                    generateEmbeddedAttributes(attribute, embedded, builder, targetName);
                    if (!generatedEmbeddedTypes.contains(embedded.typeName())) {
                        generatedEmbeddedTypes.add(embedded.typeName());
                        generateEmbeddedEntity(embedded);
                    }
                });
            } else {
                TypeMirror mirror = attribute.typeMirror();
                builder.addField(
                    generateAttribute(attribute, null, targetName, fieldName, mirror, false) );
                attributeNames.add(fieldName);
            }
        });
        generateType(builder, targetName);
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

            // returns this class as the builder
            TypeSpec.Builder supplier = TypeSpec.anonymousClassBuilder("")
                    .addSuperinterface(parameterizedTypeName(Supplier.class, typeName));
            supplier.addMethod(CodeGeneration.overridePublicMethod("get")
                    .returns(typeName)
                    .addStatement("return new $T()", typeName).build());
            block.add(".setBuilderFactory($L)\n", supplier.build());

            MethodSpec.Builder applyMethod = CodeGeneration.overridePublicMethod("apply")
                    .addParameter(typeName, "value")
                    .returns(targetName);

            // add embedded builder calls
            entity.attributes().values().stream()
                .filter(AttributeDescriptor::isEmbedded)
                .forEach(attribute -> graph.embeddedDescriptorOf(attribute).ifPresent(embedded ->
                    embedded.builderType().ifPresent(type -> {
                        String fieldName = attribute.fieldName() + "Builder";
                        String methodName = attribute.setterName();
                        applyMethod.addStatement(
                                "value.builder.$L(value.$L.build())", methodName, fieldName);
                    })));

            applyMethod.addStatement(entity.builderType().isPresent() ?
                    "return value.builder.build()" : "return value.build()");

            TypeSpec.Builder buildFunction = TypeSpec.anonymousClassBuilder("")
                    .addSuperinterface(parameterizedTypeName(Function.class, typeName, targetName))
                    .addMethod(applyMethod.build());
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

        if (entity.tableAttributes().length > 0) {
            StringJoiner joiner = new StringJoiner(",", "new String[] {", "}");
            for (String attribute : entity.tableAttributes()) {
                joiner.add("\"" + attribute + "\"");
            }
            block.add(".setTableCreateAttributes($L)\n", joiner.toString());
        }

        if (entity.tableUniqueIndexes().length > 0) {
            StringJoiner joiner = new StringJoiner(",", "new String[] {", "}");
            for (String attribute : entity.tableUniqueIndexes()) {
                joiner.add("\"" + attribute + "\"");
            }
            block.add(".setTableUniqueIndexes($L)\n", joiner.toString());
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

    private void generateEmbeddedAttributes(AttributeDescriptor parent,
                                            EntityDescriptor embedded,
                                            TypeSpec.Builder builder,
                                            TypeName targetName) {
        // generate the embedded attributes into this type
        embedded.attributes().values().forEach(attribute -> {
            String fieldName = Names.upperCaseUnderscore(embeddedAttributeName(parent, attribute));
            TypeMirror mirror = attribute.typeMirror();
            builder.addField(
                generateAttribute(attribute, parent, targetName, fieldName, mirror, false));
            attributeNames.add(fieldName);
        });
    }

    private void generateEmbeddedEntity(EntityDescriptor embedded) {
        // generate an embedded implementation for this (the parent) entity
        try {
            new EntityGenerator(processingEnv, graph, embedded, entity).generate();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private FieldSpec generateAttribute(AttributeDescriptor attribute,
                                        AttributeDescriptor parent,
                                        TypeName targetName,
                                        String fieldName,
                                        TypeMirror mirror,
                                        boolean expression) {
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
        String attributeName = attribute.name();
        if (parent != null && parent.isEmbedded()) {
            attributeName = embeddedAttributeName(parent, attribute);
        }

        if (attribute.isIterable()) {
            typeMirror = tryFirstTypeArgument(typeMirror);
            TypeName name = nameResolver.tryGeneratedTypeName(typeMirror);
            TypeElement collection = (TypeElement) types.asElement(attribute.typeMirror());

            ParameterizedTypeName builderName = parameterizedTypeName(
                attribute.builderClass(), targetName, typeName, name);

            builder.add("\nnew $T($S, $T.class, $T.class)\n",
                builderName, attributeName, ClassName.get(collection), name);

        } else if (attribute.isMap() && attribute.cardinality() != null) {
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
                attributeName, ClassName.get(valueElement), keyName, valueName);
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
            builder.add(statement, builderName, attributeName, classType);
        }
        if (!expression) {
            generateProperties(attribute, parent, typeMirror, targetName, typeName, builder);
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
        if (!Names.isEmpty(attribute.definition())) {
            builder.add(".setDefinition($S)\n", attribute.definition());
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
                        String name =
                                upperCaseUnderscoreRemovePrefixes(referencedAttribute.fieldName());
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
                    String staticMemberName = upperCaseUnderscoreRemovePrefixes(mapped.fieldName());

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

                        String staticMemberName =
                                upperCaseUnderscoreRemovePrefixes(orderBy.fieldName());
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
        Optional<AssociativeEntityDescriptor> descriptor = attribute.associativeEntity();
        if (descriptor.isPresent()) {
            Optional<TypeMirror> mirror = descriptor.get().type();
            if (mirror.isPresent()) {
                typeName = TypeName.get(mirror.get());
            } else {
                // generate a special type for the junction table (with attributes)
                graph.referencingEntity(attribute).ifPresent(referencing -> {
                    JoinEntityGenerator generator = new JoinEntityGenerator(
                            processingEnv, nameResolver, entity, referencing, attribute);
                    try {
                        generator.generate();
                    } catch (IOException e) {
                        processingEnv.getMessager()
                                .printMessage(Diagnostic.Kind.ERROR, e.toString());
                        throw new RuntimeException(e);
                    }
                });
                typeName = nameResolver.joinEntityName(descriptor.get(), entity, referenced);
            }
        } else if (mappings.size() == 1) {
            descriptor = mappings.iterator().next().associativeEntity();
            if (descriptor.isPresent()) {
                Optional<TypeMirror> mirror = descriptor.get().type();
                if (mirror.isPresent()) {
                    typeName = TypeName.get(mirror.get());
                } else {
                    typeName = nameResolver.joinEntityName(descriptor.get(), referenced, entity);
                }
            }
        }
        return Optional.ofNullable(typeName);
    }

    private void generateProperties(AttributeDescriptor attribute,
                                    AttributeDescriptor parent,
                                    TypeMirror typeMirror,
                                    TypeName targetName,
                                    TypeName attributeName,
                                    CodeBlock.Builder block) {
        String prefix = "";
        if (parent != null) {
            prefix = parent.getterName() + "().";
        }
        // boxed get/set using Objects
        Class propertyClass = propertyClassFor(typeMirror);
        ParameterizedTypeName propertyType = propertyName(propertyClass, targetName, attributeName);

        TypeSpec.Builder builder = TypeSpec.anonymousClassBuilder("")
            .addSuperinterface(propertyType);

        boolean isNullable = typeMirror.getKind().isPrimitive() && attribute.isNullable();
        boolean useGetter = entity.isUnimplementable() || entity.isImmutable();
        boolean useSetter = entity.isUnimplementable();
        String getName = prefix + (useGetter? attribute.getterName() : attribute.fieldName());
        String setName = prefix + (useSetter? attribute.setterName() : attribute.fieldName());
        new GeneratedProperty(getName, setName, targetName, attributeName)
                .setNullable(isNullable)
                .setReadOnly(entity.isImmutable())
                .setUseMethod(useGetter)
                .build(builder);

        // additional primitive get/set if the type is primitive
        if (propertyClass != Property.class) {
            TypeName primitiveType = TypeName.get(attribute.typeMirror());
            String name = Names.upperCaseFirst(attribute.typeMirror().toString());

            new GeneratedProperty(getName, setName, targetName, primitiveType)
                .setMethodSuffix(name)
                .setReadOnly(entity.isImmutable())
                .setUseMethod(useGetter)
                .build(builder);
        }
        block.add(".setProperty($L)\n", builder.build());
        block.add(".setPropertyName($S)\n", attribute.element().getSimpleName());

        // property state get/set
        if (!entity.isStateless()) {
            ClassName stateClass = ClassName.get(PropertyState.class);
            TypeSpec.Builder stateType = TypeSpec.anonymousClassBuilder("")
                .addSuperinterface(parameterizedTypeName(Property.class, targetName, stateClass));
            String fieldName = prefix + propertyStateFieldName(attribute);
            new GeneratedProperty(fieldName, targetName, stateClass).build(stateType);
            block.add(".setPropertyState($L)\n", stateType.build());
        }

        // if immutable add setter for the builder
        if (entity.isImmutable()) {
            String propertyName = attribute.fieldName();
            TypeName builderName = typeName;
            useSetter = false;
            String parameterSuffix = null;
            Optional<TypeElement> builderType = entity.builderType();
            if (builderType.isPresent()) {
                parameterSuffix = ".builder";
                if (parent != null) {
                    parameterSuffix = "." + parent.fieldName() + "Builder";
                }
                propertyName = attribute.setterName();
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
                    .setAccessSuffix(parameterSuffix)
                    .build(builderProperty);
            if (propertyClass != Property.class) {
                TypeName primitiveType = TypeName.get(attribute.typeMirror());
                String name = Names.upperCaseFirst(attribute.typeMirror().toString());
                new GeneratedProperty(propertyName, builderName, primitiveType)
                    .setMethodSuffix(name)
                    .setAccessSuffix(parameterSuffix)
                    .setUseMethod(useSetter)
                    .setWriteOnly(true)
                    .build(builderProperty);
            }
            block.add(".setBuilderProperty($L)\n", builderProperty.build());
        }
    }

    private static ParameterizedTypeName propertyName(Class type, TypeName targetName,
                                                      TypeName fieldName) {
        return type == Property.class ?
                ParameterizedTypeName.get(ClassName.get(type), targetName, fieldName) :
                ParameterizedTypeName.get(ClassName.get(type), targetName);
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

    private static String upperCaseUnderscoreRemovePrefixes(String name) {
        return Names.upperCaseUnderscore(Names.removeMemberPrefixes(name));
    }
}
