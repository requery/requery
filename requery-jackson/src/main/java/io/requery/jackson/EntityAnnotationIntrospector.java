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

package io.requery.jackson;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.AnnotationIntrospector;
import com.fasterxml.jackson.databind.PropertyName;
import com.fasterxml.jackson.databind.introspect.Annotated;
import com.fasterxml.jackson.databind.introspect.AnnotatedClass;
import com.fasterxml.jackson.databind.introspect.AnnotatedMember;
import com.fasterxml.jackson.databind.introspect.ObjectIdInfo;
import io.requery.Column;
import io.requery.Table;
import io.requery.Transient;
import io.requery.View;
import io.requery.meta.Attribute;
import io.requery.meta.EntityModel;
import io.requery.meta.Type;
import io.requery.proxy.EntityProxy;
import io.requery.proxy.PropertyState;

import java.lang.reflect.Field;

/**
 * Implementation of {@link AnnotationIntrospector} that creates Object id type and reference
 * information for entity types annotated with requery annotations.
 *
 * @author Nikhil Purushe
 */
class EntityAnnotationIntrospector extends AnnotationIntrospector {

    private final EntityModel model;
    private final Version version;
    private boolean useTableNames;

    EntityAnnotationIntrospector(EntityModel model, Version version) {
        this.model = model;
        this.version = version;
        this.useTableNames = false;
    }

    @Override
    public Version version() {
        return version;
    }

    @Override
    public ObjectIdInfo findObjectIdInfo(Annotated annotated) {
        Class<?> rawClass = annotated.getType().getRawClass();
        for (Type<?> type : model.getTypes()) {
            if (type.getClassType() == rawClass && type.getSingleKeyAttribute() != null) {
                Attribute<?, ?> attribute = type.getSingleKeyAttribute();
                String name = attribute.getPropertyName();
                if (useTableNames) {
                    name = attribute.getName();
                }

                // if the name is overridden use that
                Class<?> superClass = rawClass.getSuperclass();
                while (superClass != Object.class && superClass != null) {
                    try {
                        Field field = superClass.getDeclaredField(attribute.getPropertyName());
                        JsonProperty jsonProperty = field.getAnnotation(JsonProperty.class);
                        if (jsonProperty != null) {
                            name = jsonProperty.value();
                            break;
                        }
                    } catch (NoSuchFieldException ignored) {
                    }
                    superClass = superClass.getSuperclass();
                }

                return new ObjectIdInfo(new PropertyName(name), rawClass,
                        ObjectIdGenerators.PropertyGenerator.class,
                        EntityStoreResolver.class);
            }
        }
        return super.findObjectIdInfo(annotated);
    }

    @Override
    public PropertyName findNameForDeserialization(Annotated annotated) {
        if (useTableNames) {
            return getMappedName(annotated);
        } else {
            return super.findNameForDeserialization(annotated);
        }
    }

    @Override
    public PropertyName findNameForSerialization(Annotated annotated) {
        if (useTableNames) {
            return getMappedName(annotated);
        } else {
            return super.findNameForSerialization(annotated);
        }
    }

    private PropertyName getMappedName(Annotated annotated) {
        if (annotated.hasAnnotation(Table.class)) {
            Table table = annotated.getAnnotation(Table.class);
            return new PropertyName(table.name());
        } if (annotated.hasAnnotation(View.class)) {
            View view = annotated.getAnnotation(View.class);
            return new PropertyName(view.name());
        } else if (annotated.hasAnnotation(Column.class)) {
            Column column = annotated.getAnnotation(Column.class);
            return new PropertyName(column.name());
        } else {
            return null;
        }
    }

    @Override
    public boolean hasIgnoreMarker(AnnotatedMember member) {
        return member.hasAnnotation(Transient.class);
    }

    @Override
    public Boolean isIgnorableType(AnnotatedClass annotatedClass) {
        return PropertyState.class.isAssignableFrom(annotatedClass.getAnnotated()) ||
               EntityProxy.class.isAssignableFrom(annotatedClass.getAnnotated());
    }
}
