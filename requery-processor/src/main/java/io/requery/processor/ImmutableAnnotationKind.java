/*
 * Copyright 2017 requery.io
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

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

enum ImmutableAnnotationKind {
    AUTOVALUE("com.google.auto.value.AutoValue"),
    AUTOPARCEL("auto.parcel.AutoParcel"),
    IMMUTABLE("org.immutables.value.Value.Immutable") {
        @Override
        Set<String> getMemberAnnotations() {
            return Collections.singleton("org.immutables.value.Value.Default");
        }
    };

    static Optional<ImmutableAnnotationKind> of(Element element) {
        return Stream.of(ImmutableAnnotationKind.values())
                .filter(type -> type.isPresent(element)).findFirst();
    }

    private final String annotationName;

    ImmutableAnnotationKind(String name) {
        annotationName = name;
    }

    String getAnnotationName() {
        return annotationName;
    }

    Set<String> getMemberAnnotations() {
        return Collections.emptySet();
    }

    boolean isPresent(Element element) {
        return Mirrors.findAnnotationMirror(element, getAnnotationName()).isPresent();
    }

    boolean hasAnyMemberAnnotation(ExecutableElement element) {
        return getMemberAnnotations().stream()
                .map(name -> Mirrors.findAnnotationMirror(element, name))
                .anyMatch(Optional::isPresent);
    }
}
