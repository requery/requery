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
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

class SuperType extends BaseProcessableElement<TypeElement> implements SuperDescriptor {

    private final Map<TypeElement, Set<Element>> annotatedElements;

    SuperType(TypeElement element) {
        super(element);
        annotatedElements = new LinkedHashMap<>();
    }

    void addAnnotationElement(TypeElement annotation, Element element) {
        // simply stores the elements, the Entity type will process and validate them for each
        // type that extends the superclass
        Set<Element> elements =
            annotatedElements.computeIfAbsent(annotation, key -> new LinkedHashSet<>());
        elements.add(element);
    }

    @Override
    public Set<ElementValidator> process(ProcessingEnvironment processingEnvironment) {
        // just does some basic validation
        ElementValidator validator = new ElementValidator(element(), processingEnvironment);
        if (element().getKind().isClass() && element().getModifiers().contains(Modifier.FINAL)) {
            validator.warning("Superclass annotation cannot be applied to final class");
        }
        if (element().getKind() == ElementKind.ENUM) {
            validator.error("Superclass annotation cannot be applied to an enum class");
        }
        return Collections.singleton(validator);
    }

    @Override
    public Map<TypeElement, Set<Element>> annotatedElements() {
        return annotatedElements;
    }
}
