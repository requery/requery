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
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import io.requery.CascadeAction;
import io.requery.Entity;
import io.requery.ForeignKey;
import io.requery.Key;
import io.requery.Persistable;
import io.requery.PropertyNameStyle;
import io.requery.ReferentialAction;
import io.requery.Table;
import io.requery.meta.Attribute;
import io.requery.meta.Cardinality;
import io.requery.meta.QueryAttribute;
import io.requery.meta.Type;
import io.requery.meta.TypeBuilder;
import io.requery.proxy.EntityProxy;
import io.requery.proxy.Getter;
import io.requery.proxy.PreInsertListener;
import io.requery.proxy.PropertyState;
import io.requery.proxy.Setter;
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
import java.io.IOException;
import java.io.Serializable;
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

    private static final String PROXY_NAME = "$proxy";

    private final ProcessingEnvironment processingEnvironment;
    private final Elements elements;
    private final Types types;
    private final EntityDescriptor entity;
    private final HashSet<String> fieldNames;
    private final TypeElement typeElement;
    private final ClassName typeName;
    private final EntityGraph graph;
    private final EntityNameResolver nameResolver;
    private final Set<TypeGenerationExtension> typeExtensions;
    private final Set<PropertyGenerationExtension> memberExtensions;

    public EntityGenerator(ProcessingEnvironment processingEnvironment,
                           EntityGraph graph,
                           EntityDescriptor entity) {
        this.entity = entity;
        this.processingEnvironment = processingEnvironment;
        this.elements = processingEnvironment.getElementUtils();
        this.types = processingEnvironment.getTypeUtils();
        this.graph = graph;
        nameResolver = new EntityNameResolver(graph);
        fieldNames = new HashSet<>();
        typeElement = entity.element();
        typeName = (ClassName) nameResolver.typeNameOf(entity);
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
        TypeSpec.Builder typeBuilder = TypeSpec.classBuilder(typeName.simpleName())
                .addModifiers(Modifier.PUBLIC);
        typeBuilder.addOriginatingElement(typeElement);
        if (typeElement.getKind().isInterface()) {
            typeBuilder.addSuperinterface(ClassName.get(typeElement));
        } else {
            typeBuilder.superclass(ClassName.get(typeElement));
        }
        typeBuilder.addSuperinterface(ClassName.get(Persistable.class));
        CodeGeneration.addGeneratedAnnotation(processingEnvironment, typeBuilder);
        generateStaticMetadata(typeBuilder);
        generateConstructors(typeBuilder);
        generateMembers(typeBuilder);
        generateBody(typeBuilder);
        generateEquals(typeBuilder);
        generateHashCode(typeBuilder);
        generateToString(typeBuilder);
        for (TypeGenerationExtension typePart : typeExtensions) {
            typePart.generate(entity, typeBuilder);
        }
        CodeGeneration.writeType(
            processingEnvironment, typeName.packageName(), typeBuilder.build());
    }

    private void generateMembers(TypeSpec.Builder typeBuilder) {

        // generate property states
        if (!entity.isStateless()) {
            for (Map.Entry<Element, ? extends AttributeDescriptor> entry :
                entity.attributes().entrySet()) {

                AttributeDescriptor attribute = entry.getValue();
                TypeName stateType = ClassName.get(PropertyState.class);
                FieldSpec field = FieldSpec
                    .builder(stateType, propertyStateFieldName(attribute), Modifier.PRIVATE)
                    .build();
                typeBuilder.addField(field);
            }
        }
        if (!typeElement.getKind().isInterface()) {
            return; //only used when generating from a interface
        }
        for (Map.Entry<Element, ? extends AttributeDescriptor> entry :
            entity.attributes().entrySet()) {
            Element element = entry.getKey();
            AttributeDescriptor attribute = entry.getValue();
            if (element.getKind() == ElementKind.METHOD) {
                ExecutableElement methodElement = (ExecutableElement) element;
                TypeMirror fieldType = methodElement.getReturnType();
                if (attribute.isOptional()) {
                    fieldType = tryFirstTypeArgument(fieldType);
                }
                FieldSpec field = FieldSpec
                    .builder(TypeName.get(fieldType), attribute.fieldName(), Modifier.PRIVATE)
                    .build();
                typeBuilder.addField(field);
            }
        }
    }

    private void generateConstructors(TypeSpec.Builder typeBuilder) {
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
                StringBuilder superParameters = new StringBuilder();
                int index = 0;
                for (String parameterName : parameterNames) {
                    if (index > 0) {
                        superParameters.append(",");
                    }
                    superParameters.append(parameterName);
                    index++;
                }
                constructorBuilder.addCode(CodeBlock.builder()
                        .addStatement("super(" + superParameters.toString() + ")").build());
                typeBuilder.addMethod(constructorBuilder.build());
            }
        }
    }

    private void generateBody(TypeSpec.Builder typeBuilder) {
        // add proxy field
        TypeName proxyName = parameterizedTypeName(EntityProxy.class, typeName);
        FieldSpec.Builder proxyField = FieldSpec.builder(proxyName, PROXY_NAME,
                Modifier.PRIVATE, Modifier.FINAL, Modifier.TRANSIENT);
        proxyField.initializer("new $T(this, $L)", proxyName, entity.staticTypeName());
        typeBuilder.addField(proxyField.build());

        for (Map.Entry<Element, ? extends AttributeDescriptor> entry :
            entity.attributes().entrySet()) {

            AttributeDescriptor attribute = entry.getValue();
            boolean isTransient = attribute.isTransient();

            TypeMirror typeMirror = attribute.typeMirror();
            TypeName unboxedTypeName;
            if (attribute.isIterable()) {
                unboxedTypeName = getParameterizedCollectionName(typeMirror);
            } else if (attribute.isOptional()) {
                unboxedTypeName = TypeName.get(tryFirstTypeArgument(attribute.typeMirror()));
            } else {
                unboxedTypeName = nameResolver.tryGeneratedTypeName(typeMirror);
            }

            String attributeName = attribute.fieldName();
            String getterName = entry.getValue().getterName();
            String fieldName = Names.upperCaseUnderscore(Names.removeMemberPrefixes(attributeName));
            // getter
            MethodSpec.Builder getter = MethodSpec.methodBuilder(getterName)
                    .addModifiers(Modifier.PUBLIC)
                    .returns(attribute.isOptional() ?
                        TypeName.get(attribute.typeMirror()) : unboxedTypeName);
            if (Mirrors.overridesMethod(types, typeElement, getterName)) {
                getter.addAnnotation(Override.class);
            }
            for (PropertyGenerationExtension snippet : memberExtensions) {
                snippet.addToGetter(attribute, getter);
            }
            if (isTransient) {
                getter.addStatement("return this.$L", attributeName);
            } else if (attribute.isOptional()) {
                getter.addStatement("return $T.ofNullable($L.get($L))",
                    Optional.class, PROXY_NAME, fieldName);
            } else {
                getter.addStatement("return $L.get($L)", PROXY_NAME, fieldName);
            }
            typeBuilder.addMethod(getter.build());

            // setter
            String setterName = entry.getValue().setterName();
            // if read only don't generate a public setter
            boolean readOnly = entity.isReadOnly() || attribute.isReadOnly();
            if (!readOnly) {
                String argumentName =
                    Names.lowerCaseFirst(Names.removeMemberPrefixes(attributeName));
                MethodSpec.Builder setter = MethodSpec.methodBuilder(setterName)
                        .addModifiers(Modifier.PUBLIC)
                        .addParameter(unboxedTypeName, argumentName);
                if (isTransient) {
                    setter.addStatement("this.$L = $L", attributeName, argumentName);
                } else {
                    setter.addStatement("$L.set($L, $L)", PROXY_NAME, fieldName, argumentName);
                }
                for (PropertyGenerationExtension snippet : memberExtensions) {
                    snippet.addToSetter(attribute, setter);
                }

                PropertyNameStyle style = entity.propertyNameStyle();
                if (style == PropertyNameStyle.FLUENT || style == PropertyNameStyle.FLUENT_BEAN) {
                    setter.addStatement("return this");
                    setter.returns(typeName);
                }
                typeBuilder.addMethod(setter.build());
            }
        }

        MethodSpec.Builder constructor = MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC);
        generateListeners(constructor);
        typeBuilder.addMethod(constructor.build());
    }

    private void generateEquals(TypeSpec.Builder typeBuilder) {
        boolean overridesEquals = Mirrors.overridesMethod(types, typeElement, "equals");
        if (!overridesEquals) {
            MethodSpec.Builder equals = CodeGeneration.overridePublicMethod("equals")
                .addParameter(TypeName.OBJECT, "obj")
                .returns(TypeName.BOOLEAN)
                .addStatement("return obj instanceof $T && (($T)obj).$L.equals(this.$L)",
                        typeName, typeName, PROXY_NAME, PROXY_NAME);
            typeBuilder.addMethod(equals.build());
        }
    }

    private void generateHashCode(TypeSpec.Builder typeBuilder) {
        if (!Mirrors.overridesMethod(types, typeElement, "hashCode")) {
            MethodSpec.Builder equals = CodeGeneration.overridePublicMethod("hashCode")
                    .returns(TypeName.INT)
                    .addStatement("return $L.hashCode()", PROXY_NAME);
            typeBuilder.addMethod(equals.build());
        }
    }

    private void generateToString(TypeSpec.Builder typeBuilder) {
        if (!Mirrors.overridesMethod(types, typeElement, "toString")) {
            MethodSpec.Builder equals = CodeGeneration.overridePublicMethod("toString")
                    .returns(String.class)
                    .addStatement("return $L.toString()", PROXY_NAME);
            typeBuilder.addMethod(equals.build());
        }
    }

    private void generateListeners(MethodSpec.Builder constructor) {

        for (Map.Entry<Element, ? extends ListenerDescriptor> entry :
            entity.listeners().entrySet()) {

            for (Annotation annotation : entry.getValue().listenerAnnotations()) {
                String annotationName = annotation.annotationType().getSimpleName();
                annotationName =
                    annotationName.replace("Persist", "Insert").replace("Remove", "Delete");
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
                    .addMethod(
                        CodeGeneration.overridePublicMethod(methodName)
                            .addParameter(typeName, "entity")
                            .addStatement("$L()", entry.getKey().getSimpleName())
                            .build());

                constructor.addStatement("$L.modifyListeners().add$L($L)", PROXY_NAME,
                        annotationName + "Listener", listenerBuilder.build());
            }
        }
    }

    private void generateType(TypeSpec.Builder typeSpecBuilder) {

        ClassName schemaName = ClassName.get(TypeBuilder.class);
        ParameterizedTypeName type = parameterizedTypeName(Type.class, typeName);
        FieldSpec.Builder schemaFieldBuilder = FieldSpec.builder(type, entity.staticTypeName(),
                Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL);

        CodeBlock.Builder typeBuilder = CodeBlock.builder()
                .add("new $T<$T>($T.class, $S)\n",
                    schemaName, typeName, typeName, entity.tableName());

        typeBuilder.add(".setBaseType($T.class)\n", ClassName.get(typeElement))
            .add(".setReadOnly($L)\n", entity.isReadOnly())
            .add(".setCacheable($L)\n", entity.isCacheable())
            .add(".setStateless($L)\n", entity.isStateless());
        String factoryName = entity.classFactoryName();
        if (factoryName != null) {
            typeBuilder.add(".setFactory(new $L())\n", ClassName.bestGuess(factoryName));
        } else {
            TypeSpec.Builder typeFactory = TypeSpec.anonymousClassBuilder("")
                .addSuperinterface(parameterizedTypeName(Supplier.class, typeName))
                .addMethod(
                    CodeGeneration.overridePublicMethod("get")
                        .addStatement("return new $T()", typeName)
                        .returns(typeName)
                        .build());
            typeBuilder.add(".setFactory($L)\n", typeFactory.build());
        }

        TypeSpec.Builder proxyProvider = TypeSpec.anonymousClassBuilder("")
                .addSuperinterface(parameterizedTypeName(Function.class, typeName,
                    parameterizedTypeName(EntityProxy.class, typeName)))
                .addMethod(
                    CodeGeneration.overridePublicMethod("apply")
                        .addParameter(typeName, "entity")
                        .addStatement("return entity.$L", PROXY_NAME)
                        .returns(parameterizedTypeName(EntityProxy.class, typeName))
                        .build());
        typeBuilder.add(".setProxyProvider($L)\n", proxyProvider.build());

        if (entity.tableAttributes() != null && entity.tableAttributes().length > 0) {
            StringJoiner joiner = new StringJoiner(",", "new String[] {", "}");
            for (String attribute : entity.tableAttributes()) {
                joiner.add("\"" + attribute + "\"");
            }
            typeBuilder.add(".setTableCreateAttributes($L)\n", joiner.toString());
        }

        fieldNames.forEach(name -> typeBuilder.add(".addAttribute($L)\n", name));
        typeBuilder.add(".build()");
        schemaFieldBuilder.initializer("$L", typeBuilder.build());
        typeSpecBuilder.addField(schemaFieldBuilder.build());
    }

    private void generateStaticMetadata(TypeSpec.Builder typeBuilder) throws IOException {
        // attributes
        for (Map.Entry<Element, ? extends AttributeDescriptor> entry :
            entity.attributes().entrySet()) {

            AttributeDescriptor attribute = entry.getValue();
            if (attribute.isTransient()) {
                continue;
            }
            TypeMirror typeMirror = attribute.typeMirror();
            TypeName attributeTypeName;
            if (attribute.isIterable()) {
                typeMirror = tryFirstTypeArgument(attribute.typeMirror());
                attributeTypeName = getParameterizedCollectionName(attribute.typeMirror());
            } else if (attribute.isOptional()) {
                typeMirror = tryFirstTypeArgument(attribute.typeMirror());
                attributeTypeName = TypeName.get(typeMirror);
            } else {
                attributeTypeName = nameResolver.generatedTypeNameOf(typeMirror).orElse(null);
            }
            if (attributeTypeName == null) {
                attributeTypeName = boxedTypeName(typeMirror);
            }

            // if it's an association don't make it available as a query attribute
            boolean isAssociation = attribute.cardinality() != null;
            Class<?> attributeType = isAssociation ? Attribute.class : QueryAttribute.class;

            ParameterizedTypeName type =
                parameterizedTypeName(attributeType, typeName, attributeTypeName);
            String fieldName = Names.upperCaseUnderscore(
                Names.removeMemberPrefixes(attribute.fieldName()));
            fieldNames.add(fieldName);

            FieldSpec.Builder fieldBuilder = FieldSpec.builder(type, fieldName,
                    Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL);

            CodeBlock.Builder builder = CodeBlock.builder();

            if (attribute.isIterable()) {
                typeMirror = tryFirstTypeArgument(typeMirror);
                TypeName name = nameResolver.tryGeneratedTypeName(typeMirror);
                TypeElement collectionElement =
                    (TypeElement) types.asElement(attribute.typeMirror());

                ParameterizedTypeName builderName = parameterizedTypeName(
                    attribute.builderClass(), typeName, attributeTypeName, name);

                builder.add("\nnew $T($S, $T.class, $T.class)\n",
                        builderName, attribute.name(), ClassName.get(collectionElement),
                        name);

            } else if (attribute.isMap()) {
                List<TypeMirror> parameters = Mirrors.listGenericTypeArguments(typeMirror);
                // key type
                TypeName keyName = TypeName.get(parameters.get(0));
                // value type
                typeMirror = parameters.get(1);
                TypeName valueName = nameResolver.tryGeneratedTypeName(typeMirror);

                TypeElement valueElement = (TypeElement) types.asElement(attribute.typeMirror());
                ParameterizedTypeName builderName = parameterizedTypeName(
                    attribute.builderClass(), typeName, attributeTypeName, keyName, valueName);

                builder.add("\nnew $T($S, $T.class, $T.class, $T.class)\n", builderName,
                        attribute.name(),
                        ClassName.get(valueElement), keyName, valueName);
            } else {
                ParameterizedTypeName builderName = parameterizedTypeName(
                        attribute.builderClass(), typeName, attributeTypeName);
                TypeName classType = attributeTypeName;
                if (typeMirror.getKind().isPrimitive()) {
                    classType = TypeName.get(typeMirror);
                }
                builder.add("\nnew $T($S, $T.class)\n",
                        builderName, attribute.name(), classType);
            }

            // getter proxy
            String attributeName = attribute.fieldName();
            ParameterizedTypeName getterType =
                parameterizedTypeName(Getter.class, typeName, attributeTypeName);

            TypeSpec.Builder getterBuilder = TypeSpec.anonymousClassBuilder("")
                    .addSuperinterface(getterType)
                    .addMethod(CodeGeneration.overridePublicMethod("get")
                        .addParameter(typeName, "entity")
                        .addStatement("return entity.$L", attributeName)
                        .returns(attributeTypeName)
                        .build());

            builder.add(".setGetter($L)\n", getterBuilder.build());

            // state getter proxy
            ClassName stateClass = ClassName.get(PropertyState.class);
            if (!entity.isStateless()) {
                TypeSpec.Builder stateGetterBuilder = TypeSpec.anonymousClassBuilder("")
                    .addSuperinterface(parameterizedTypeName(Getter.class, typeName, stateClass))
                    .addMethod(CodeGeneration.overridePublicMethod("get")
                        .addParameter(typeName, "entity")
                        .addStatement("return entity.$L", propertyStateFieldName(attribute))
                        .returns(stateClass)
                        .build());

                builder.add(".setStateGetter($L)\n", stateGetterBuilder.build());
            }
            // setter proxy
            ParameterizedTypeName setterType =
                parameterizedTypeName(Setter.class, typeName, attributeTypeName);
            TypeSpec.Builder setterBuilder = TypeSpec.anonymousClassBuilder("")
                    .addSuperinterface(setterType);
            MethodSpec.Builder setterMethod = CodeGeneration.overridePublicMethod("set")
                    .addParameter(typeName, "entity")
                    .addParameter(attributeTypeName, "value");
            if (typeMirror.getKind().isPrimitive() && attribute.isNullable()) {
                CodeBlock setterBlock = CodeBlock.builder()
                        .beginControlFlow("if(value != null)")
                        .addStatement("entity.$L = value", attributeName)
                        .endControlFlow().build();
                setterMethod.addCode(setterBlock);
            } else {
                setterMethod.addStatement("entity.$L = value", attributeName);
            }
            setterBuilder.addMethod(setterMethod.build());
            builder.add(".setSetter($L)\n", setterBuilder.build());

            // state setter proxy
            if (!entity.isStateless()) {
                TypeSpec.Builder stateSetterBuilder = TypeSpec.anonymousClassBuilder("")
                    .addSuperinterface(parameterizedTypeName(Setter.class, typeName, stateClass));
                stateSetterBuilder.addMethod(CodeGeneration.overridePublicMethod("set")
                    .addParameter(typeName, "entity")
                    .addParameter(stateClass, "value")
                    .addStatement("entity.$L = value", propertyStateFieldName(attribute)).build());

                builder.add(".setStateSetter($L)\n", stateSetterBuilder.build());
            }

            // attribute properties
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
                if (attribute.referencedType() != null) {
                    ClassName referencedName = ClassName.bestGuess(attribute.referencedType());
                    builder.add(".setReferencedClass($T.class)\n", referencedName);
                }
                Optional<EntityDescriptor> referenced = graph.referencingEntity(attribute);
                if (referenced.isPresent()) {
                    Optional<? extends AttributeDescriptor> referencedElement =
                        graph.referencingAttribute(attribute, referenced.get());

                    if (referencedElement.isPresent()) {

                        String name = Names.upperCaseUnderscore(
                            referencedElement.get().fieldName());
                        TypeSpec provider = createAnonymousSupplier(
                            ClassName.get(Attribute.class),
                            CodeBlock.builder().addStatement("return $T.$L",
                                nameResolver.typeNameOf(referenced.get()), name).build());

                        builder.add(".setReferencedAttribute($L)\n", provider);
                    }
                }
            }
            if (attribute.isIndexed()) {
                builder.add(".setIndexed($L)\n", attribute.isIndexed());
                if (!Names.isEmpty(attribute.indexName())) {
                    builder.add(".setIndexName($S)\n", attribute.indexName());
                }
            }
            if (attribute.referentialAction() != null) {
                builder.add(".setReferentialAction($T.$L)\n",
                        ClassName.get(ReferentialAction.class), attribute.referentialAction());
            }
            if (!attribute.cascadeActions().isEmpty()) {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < attribute.cascadeActions().size(); i++) {
                    if (i > 0) {
                        sb.append(", ");
                    }
                    sb.append("$T.$L");
                }
                int index = 0;
                ClassName cascadeClass = ClassName.get(CascadeAction.class);
                Object[] args = new Object[attribute.cascadeActions().size()*2];
                for (CascadeAction action : attribute.cascadeActions()) {
                    args[index++] = cascadeClass;
                    args[index++] = action;
                }
                builder.add(".setCascadeAction(" + sb +  ")\n", args);
            }
            if (attribute.cardinality() != null) {
                builder.add(".setCardinality($T.$L)\n",
                        ClassName.get(Cardinality.class), attribute.cardinality());

                Optional<EntityDescriptor> referencingEntity = graph.referencingEntity(attribute);

                if (referencingEntity.isPresent()) {
                    EntityDescriptor referenced = referencingEntity.get();
                    Set<AttributeDescriptor> mappings =
                        graph.mappedAttributes(entity, attribute, referenced);

                    if (attribute.cardinality() == Cardinality.MANY_TO_MANY) {
                        String junctionType = null;
                        if (attribute.associativeEntity() != null) {
                            // generate a special type for the junction table (with attributes)
                            generateJunctionType(attribute);
                            junctionType = getJunctionTypeName(false, entity, referenced);
                        } else if ( mappings.size() == 1) {
                            junctionType = getJunctionTypeName(false, referenced, entity);
                        }
                        if (junctionType != null) {
                            builder.add(".setReferencedClass($T.class)\n",
                                ClassName.get(typeName.packageName(), junctionType));
                        }
                    }
                    if (mappings.size() == 1) {
                        AttributeDescriptor mapped = mappings.iterator().next();
                        String staticMemberName =
                            Names.upperCaseUnderscore(mapped.fieldName());

                        TypeSpec provider = createAnonymousSupplier(
                            ClassName.get(Attribute.class),
                            CodeBlock.builder().addStatement("return $T.$L",
                                    nameResolver.typeNameOf(referenced), staticMemberName)
                                .build());
                        builder.add(".setMappedAttribute($L)\n", provider);
                    }
                }
            }
            builder.add(".build()");
            fieldBuilder.initializer("$L", builder.build());

            typeBuilder.addField(fieldBuilder.build());
        }
        generateType(typeBuilder);
    }

    private String propertyStateFieldName(AttributeDescriptor attribute) {
        return "$" + attribute.fieldName() + "_state";
    }

    private static String getJunctionTypeName(boolean isAbstract,
                                              EntityDescriptor a, EntityDescriptor b) {
        String prefix = isAbstract? "Abstract" : "";
        return prefix + a.typeName().className() + "_" + b.typeName().className();
    }

    private void generateJunctionType(AttributeDescriptor attribute) throws IOException {

        Optional<EntityDescriptor> otherEntity = graph.referencingEntity(attribute);
        if (!otherEntity.isPresent()) {
            return;
        }
        String name = attribute.associativeEntity().name();
        if (Names.isEmpty(name)) {
            // create junction table name with TableA_TableB
            name = entity.tableName() + "_" + otherEntity.get().tableName();
        }
        String className = getJunctionTypeName(true, entity, otherEntity.get());

        TypeSpec.Builder junctionType = TypeSpec.classBuilder(className)
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .addSuperinterface(Serializable.class)
                .addAnnotation(AnnotationSpec.builder(Entity.class)
                        .addMember("model", "$S", entity.modelName()).build())
                .addAnnotation(AnnotationSpec.builder(Table.class)
                        .addMember("name", "$S", name).build());
        CodeGeneration.addGeneratedAnnotation(processingEnvironment, junctionType);

        Set<AssociativeReference> references = attribute.associativeEntity().columns();
        EntityDescriptor[] types = new EntityDescriptor[] { entity, otherEntity.get() };
        if (references.isEmpty()) {
            // generate with defaults
            for (EntityDescriptor type : types) {
                AssociativeReference reference = new AssociativeReference(
                    type.tableName() + "Id",
                    ReferentialAction.CASCADE,
                    type.element());
                references.add(reference);
            }
        }

        int typeIndex = 0;
        for (AssociativeReference reference : references) {
            AnnotationSpec.Builder key = AnnotationSpec.builder(ForeignKey.class)
                    .addMember("action", "$T.$L",
                            ClassName.get(ReferentialAction.class),
                        reference.referentialAction().toString());

            TypeElement referenceElement = reference.referencedType();
            if (referenceElement == null && typeIndex < types.length) {
                referenceElement = types[typeIndex++].element();
            }

            if (referenceElement != null) {
                key.addMember("references", "$L.class",
                    nameResolver.generatedTypeNameOf(referenceElement).get());
            }
            AnnotationSpec.Builder id = AnnotationSpec.builder(Key.class);
            FieldSpec.Builder field = FieldSpec.builder(Integer.class, reference.name(),
                    Modifier.PROTECTED)
                    .addAnnotation(key.build())
                    .addAnnotation(id.build());
            junctionType.addField(field.build());
        }
        CodeGeneration.writeType(processingEnvironment,
            typeName.packageName(), junctionType.build());
    }

    private static TypeSpec createAnonymousSupplier(TypeName type, CodeBlock block) {
        TypeSpec.Builder typeFactory = TypeSpec.anonymousClassBuilder("")
            .addSuperinterface(parameterizedTypeName(Supplier.class, type))
            .addMethod(CodeGeneration.overridePublicMethod("get")
                .addCode(block)
                .returns(type)
                .build());
        return typeFactory.build();
    }

    private TypeName boxedTypeName(TypeMirror typeMirror) {
        if (typeMirror.getKind().isPrimitive()) {
            TypeElement boxed = types.boxedClass((PrimitiveType) typeMirror);
            return TypeName.get(boxed.asType());
        }
        return TypeName.get(typeMirror);
    }

    private ParameterizedTypeName getParameterizedCollectionName(TypeMirror typeMirror) {
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
