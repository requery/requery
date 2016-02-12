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

import javax.lang.model.element.Element;
import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Holds a target {@link Element} part of class type to be processed (either, fields, members or
 * other elements). Provides methods to retrieve annotations that appear on the target element.
 *
 * @author Nikhil Purushe
 */
abstract class BaseProcessableElement<E extends Element> implements ProcessableElement<E> {

    private final E element;
    private final Map<Class<? extends Annotation>, Annotation> annotations;

    BaseProcessableElement(E element) {
        this.element = Objects.requireNonNull(element);
        annotations = new HashMap<>();
    }

    @Override
    public E element() {
        return element;
    }

    @Override
    public Map<Class<? extends Annotation>, Annotation> annotations() {
        return annotations;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ProcessableElement) {
            ProcessableElement other = (ProcessableElement) obj;
            return element.equals(other.element());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(element);
    }

    @Override
    public String toString() {
        return element().getSimpleName().toString();
    }
}
