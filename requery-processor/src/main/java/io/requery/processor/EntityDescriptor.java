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

import io.requery.PropertyNameStyle;

import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import java.util.Map;

/**
 * Definition of an entity to be processed.
 *
 * @author Nikhil Purushe
 */
interface EntityDescriptor {

    TypeElement element();

    QualifiedName typeName();

    String staticTypeName();

    String modelName();

    String tableName();

    String classFactoryName();

    String[] tableAttributes();

    PropertyNameStyle propertyNameStyle();

    boolean isCacheable();

    boolean isReadOnly();

    boolean isStateless();

    Map<Element, ? extends AttributeDescriptor> attributes();

    Map<Element, ? extends ListenerDescriptor> listeners();
}
