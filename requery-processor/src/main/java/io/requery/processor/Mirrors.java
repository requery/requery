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

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.SimpleTypeVisitor6;
import javax.lang.model.util.Types;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Helper class for working with {@link java.lang.annotation.Annotation) and
 * {@link javax.lang.model.element.AnnotationMirror}.
 *
 * @author Nikhil Purushe
 */
final class Mirrors {

    private Mirrors() {
    }

    static Optional<? extends AnnotationMirror> findAnnotationMirror(
            Element element, Class<? extends Annotation> annotation) {
        return findAnnotationMirror(element, annotation.getName());
    }

    static Optional<? extends AnnotationMirror> findAnnotationMirror(
            Element element, String qualifiedName) {
        return element.getAnnotationMirrors().stream()
            .filter(mirror ->
                namesEqual((TypeElement) mirror.getAnnotationType().asElement(), qualifiedName))
            .findFirst();
    }

    static Optional<AnnotationValue> findAnnotationValue(AnnotationMirror mirror) {
        return findAnnotationValue(mirror, "value");
    }

    static Optional<AnnotationValue> findAnnotationValue(AnnotationMirror mirror, String name) {

        return mirror.getElementValues() == null ? Optional.empty() :
               mirror.getElementValues().entrySet().stream()
                .filter(entry -> entry.getKey().getSimpleName().contentEquals(name))
                .map(entry -> (AnnotationValue)entry.getValue()).findFirst();
    }

    static List<TypeMirror> listGenericTypeArguments(TypeMirror typeMirror) {
        final List<TypeMirror> typeArguments = new ArrayList<>();
        typeMirror.accept(new SimpleTypeVisitor6<Void, Void>() {
            @Override
            public Void visitDeclared(DeclaredType declaredType, Void v) {
                if (!declaredType.getTypeArguments().isEmpty()) {
                    typeArguments.addAll(declaredType.getTypeArguments());
                }
                return null;
            }

            @Override
            protected Void defaultAction(TypeMirror typeMirror, Void v) {
                return null;
            }
        }, null);
        return typeArguments;
    }

    static boolean isInstance(Types types, TypeElement element, Class<?> type) {
        if (element == null) {
            return false;
        }
        String className = type.getCanonicalName();
        if (type.isInterface()) {
            return implementsInterface(types, element, className);
        } else {
            return namesEqual(element, className) || extendsClass(types, element, className);
        }
    }

    static boolean isInstance(Types types, TypeElement element, String className) {
        if (element == null) {
            return false;
        }
        // check name
        if (namesEqual(element, className)) {
            return true;
        }
        // check interfaces then super types
        return implementsInterface(types, element, className) ||
                extendsClass(types, element, className);
    }

    private static boolean implementsInterface(Types types, TypeElement element, String interfaceName) {
        // check name or interfaces
        if (namesEqual(element, interfaceName)) {
            return true;
        }
        List<? extends TypeMirror> interfaces = element.getInterfaces();
        for (TypeMirror interfaceType : interfaces) {
            interfaceType = types.erasure(interfaceType);
            TypeElement typeElement = (TypeElement) types.asElement(interfaceType);
            if (typeElement != null && implementsInterface(types, typeElement, interfaceName)) {
                return true;
            }
        }
        return false;
    }

    private static boolean extendsClass(Types types, TypeElement element, String className) {
        if (namesEqual(element, className)) {
            return true;
        }
        // check super types
        TypeMirror superType = element.getSuperclass();
        while (superType != null && superType.getKind() != TypeKind.NONE) {
            TypeElement superTypeElement = (TypeElement) types.asElement(superType);
            if (namesEqual(superTypeElement, className)) {
                return true;
            }
            superType = superTypeElement.getSuperclass();
        }
        return false;
    }

    static boolean overridesMethod(Types types, TypeElement element, String methodName) {
        while (element != null) {
            if (ElementFilter.methodsIn(element.getEnclosedElements()).stream()
                .anyMatch(method -> method.getSimpleName().contentEquals(methodName))) {
                return true;
            }
            TypeMirror superType = element.getSuperclass();
            if (superType.getKind() == TypeKind.NONE) {
                break;
            } else {
                element = (TypeElement) types.asElement(superType);
            }
            if (namesEqual(element, Object.class.getCanonicalName())) {
                break;
            }
        }
        return false;
    }

    private static boolean namesEqual(TypeElement element, String qualifiedName) {
        return element != null && element.getQualifiedName().contentEquals(qualifiedName);
    }
}
