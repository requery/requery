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
    private boolean hasErrors;
    private boolean hasWarnings;

    ElementValidator(Element element, ProcessingEnvironment processingEnvironment) {
        this.element = element;
        this.messager = processingEnvironment.getMessager();
    }

    void error(String message) {
        hasErrors = true;
        messager.printMessage(Diagnostic.Kind.ERROR, message, element);
    }

    void error(String message, Class<? extends Annotation> annotation) {
        hasErrors = true;
        String name = annotation.getName();
        Optional<? extends AnnotationMirror> mirror = Mirrors.findAnnotationMirror(element, name);
        if (mirror.isPresent()) {
            messager.printMessage(Diagnostic.Kind.ERROR, message, element, mirror.get());
        } else {
            messager.printMessage(Diagnostic.Kind.ERROR, message);
        }
    }

    void warning(String message) {
        hasWarnings = true;
        messager.printMessage(Diagnostic.Kind.WARNING, message, element);
    }

    void warning(String message, Class<? extends Annotation> annotation) {
        hasWarnings = true;
        String name = annotation.getName();
        Optional<? extends AnnotationMirror> mirror = Mirrors.findAnnotationMirror(element, name);
        if (mirror.isPresent()) {
            messager.printMessage(Diagnostic.Kind.WARNING, message, element, mirror.get());
        } else {
            messager.printMessage(Diagnostic.Kind.WARNING, message);
        }
    }

    boolean hasWarnings() {
        return hasWarnings;
    }

    boolean hasErrors() {
        return hasErrors;
    }

    static boolean hasErrors(Set<ElementValidator> validators) {
        for (ElementValidator validator : validators) {
            if (validator.hasErrors()) {
                return true;
            }
        }
        return false;
    }
}
