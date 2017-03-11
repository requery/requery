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

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;
import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Processes listener callback annotations on an entity type.
 *
 * @author Nikhil Purushe
 */
class ListenerMethod extends BaseProcessableElement<ExecutableElement> implements
    ListenerDescriptor {

    private List<Annotation> annotations;

    ListenerMethod(ExecutableElement element) {
        super(element);
    }

    @Override
    public Set<ElementValidator> process(ProcessingEnvironment processingEnvironment) {
        ElementValidator validator = new ElementValidator(element(), processingEnvironment);

        annotations = ListenerAnnotations.all()
            .map(this::annotationOf)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.toList());

        if (!annotations.isEmpty()) {
            ExecutableElement element = element();
            if (!element.getParameters().isEmpty()) {
                validator.error("Callback method cannot have arguments");
            }
            if (element.getModifiers().contains(Modifier.PRIVATE)) {
                validator.error("Callback method cannot be private");
            }
            if (element.getModifiers().contains(Modifier.ABSTRACT)) {
                validator.error("Callback method cannot be abstract");
            }
            if (!element.getThrownTypes().isEmpty()) {
                Types types = processingEnvironment.getTypeUtils();
                for (TypeMirror mirror : element.getThrownTypes()) {
                    Element exceptionElement = types.asElement(mirror);
                    if (exceptionElement != null &&
                        exceptionElement.getKind() == ElementKind.CLASS) {
                        TypeElement typeElement = (TypeElement) exceptionElement;
                        if (!Mirrors.isInstance(types, typeElement, RuntimeException.class)) {
                            validator.error("Callback method cannot throw checked exception(s)");
                        }
                    }
                }
            }
            annotations.stream().filter(
                    annotation -> element().getSimpleName().contentEquals(
                            Names.lowerCaseFirst(annotation.getClass().getSimpleName())))
                    .forEach(annotation -> validator.error(
                            "Callback method cannot have the same name as the listener method"));
        }
        return Collections.singleton(validator);
    }

    @Override
    public Collection<Annotation> listenerAnnotations() {
        return annotations;
    }
}
