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
import java.lang.annotation.Annotation;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Wraps an element that contains annotations that are to be processed by the annotation processor.
 *
 * @param <E> the type of the element being processed
 *
 * @author Nikhil Purushe
 */
interface ProcessableElement<E extends Element> {

    E element();

    Set<ElementValidator> process(ProcessingEnvironment processingEnvironment);

    default <A extends Annotation> Optional<A> annotationOf(Class<? extends A> type) {
        A value = type.cast(annotations().get(type));
        return Optional.ofNullable(value == null ? element().getAnnotation(type) : value);
    }

    Map<Class<? extends Annotation>, Annotation> annotations();
}
