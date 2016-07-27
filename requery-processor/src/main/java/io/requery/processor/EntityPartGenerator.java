package io.requery.processor;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import java.util.HashSet;
import java.util.List;

abstract class EntityPartGenerator {

    static final String PROXY_NAME = "$proxy";
    static final String TYPE_NAME = "$TYPE";

    final EntityDescriptor entity;
    final ProcessingEnvironment processingEnv;
    final Elements elements;
    final Types types;
    final HashSet<String> attributeNames;
    final HashSet<String> expressionNames;
    final TypeElement typeElement;
    final ClassName typeName;
    final EntityGraph graph;
    final EntityNameResolver nameResolver;

    EntityPartGenerator(ProcessingEnvironment processingEnv,
                        EntityGraph graph,
                        EntityDescriptor entity) {
        this.processingEnv = processingEnv;
        this.graph = graph;
        this.entity = entity;
        this.elements = processingEnv.getElementUtils();
        this.types = processingEnv.getTypeUtils();
        nameResolver = new EntityNameResolver(graph);
        attributeNames = new HashSet<>();
        expressionNames = new HashSet<>();
        typeElement = entity.element();
        typeName = nameResolver.typeNameOf(entity);
    }

    static String propertyStateFieldName(AttributeDescriptor attribute) {
        return "$" + attribute.fieldName() + "_state";
    }

    TypeName boxedTypeName(TypeMirror typeMirror) {
        if (typeMirror.getKind().isPrimitive()) {
            return TypeName.get(types.boxedClass((PrimitiveType) typeMirror).asType());
        }
        return TypeName.get(typeMirror);
    }

    ParameterizedTypeName parameterizedCollectionName(TypeMirror typeMirror) {
        TypeMirror genericType = tryFirstTypeArgument(typeMirror);
        TypeName elementName = nameResolver.tryGeneratedTypeName(genericType);
        TypeElement collectionElement = (TypeElement) types.asElement(typeMirror);
        ClassName collectionName = ClassName.get(collectionElement);
        return ParameterizedTypeName.get(collectionName, elementName);
    }

    static TypeMirror tryFirstTypeArgument(TypeMirror typeMirror) {
        List<TypeMirror> args = Mirrors.listGenericTypeArguments(typeMirror);
        return args.isEmpty() ? typeMirror : args.get(0);
    }

    static ParameterizedTypeName parameterizedTypeName(Class<?> rawType,
                                                               TypeName... typeArguments) {
        return ParameterizedTypeName.get(ClassName.get(rawType), typeArguments);
    }
}
