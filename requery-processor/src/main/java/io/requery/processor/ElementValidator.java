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

import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.tools.Diagnostic;
import java.lang.annotation.Annotation;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Validation helper for a specific {@link Element}.
 *
 * @author Nikhil Purushe
 */
class ElementValidator {

    private final Element element;
    private final Messager messager;
    private boolean hasWarnings;
    private final Map<Element, String> errors;

    ElementValidator(Element element, ProcessingEnvironment processingEnvironment) {
        this.element = Objects.requireNonNull(element);
        this.messager = processingEnvironment.getMessager();
        errors = new LinkedHashMap<>();
    }

    void error(String message) {
        messager.printMessage(Diagnostic.Kind.ERROR, message, element);
        errors.put(element, message);
    }

    void error(String message, Class<? extends Annotation> annotation) {
        printMessage(annotation, Diagnostic.Kind.ERROR, message);
        errors.put(element, message);
    }

    void warning(String message) {
        hasWarnings = true;
        messager.printMessage(Diagnostic.Kind.WARNING, message, element);
    }

    void warning(String message, Class<? extends Annotation> annotation) {
        hasWarnings = true;
        printMessage(annotation, Diagnostic.Kind.WARNING, message);
    }

    private void printMessage(Class<? extends Annotation> annotation,
                              Diagnostic.Kind kind, String message) {
        String name = annotation.getName();
        Optional<? extends AnnotationMirror> mirror = Mirrors.findAnnotationMirror(element, name);
        if (mirror.isPresent()) {
            messager.printMessage(kind, message, element, mirror.get());
        } else {
            messager.printMessage(kind, message);
        }
    }

    boolean hasWarnings() {
        return hasWarnings;
    }

    boolean hasErrors() {
        return !errors.values().isEmpty();
    }

    static boolean hasErrors(Set<ElementValidator> validators) {
        for (ElementValidator validator : validators) {
            if (validator.hasErrors()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        errors.entrySet().forEach(entry ->
                sb.append(entry.getKey().getSimpleName())
                  .append(" : ")
                  .append(entry.getValue()));
        return sb.toString();
    }
}
