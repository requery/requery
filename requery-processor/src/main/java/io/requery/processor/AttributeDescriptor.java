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

import io.requery.CascadeAction;
import io.requery.ReferentialAction;
import io.requery.meta.AttributeBuilder;
import io.requery.meta.Cardinality;

import javax.lang.model.element.Element;
import javax.lang.model.type.TypeMirror;
import java.util.Set;

/**
 * Defines a persistable attribute of an {@link EntityDescriptor} to be processed.
 *
 * @author Nikhil Purushe
 */
interface AttributeDescriptor {

    Element element();

    TypeMirror typeMirror();

    String fieldName();

    String getterName();

    String setterName();

    String name();

    String defaultValue();

    String collate();

    boolean isKey();

    boolean isTransient();

    boolean isNullable();

    boolean isUnique();

    boolean isGenerated();

    boolean isLazy();

    boolean isForeignKey();

    boolean isReadOnly();

    boolean isVersion();

    boolean isIndexed();

    boolean isMap();

    boolean isIterable();

    Class<? extends AttributeBuilder> builderClass();

    Integer columnLength();

    Cardinality cardinality();

    ReferentialAction referentialAction();

    Set<CascadeAction> cascadeActions();

    String referencedColumn();

    String referencedType();

    String mappedBy();

    String converterName();

    String indexName();

    AssociativeEntityDescriptor associativeEntity();
}
