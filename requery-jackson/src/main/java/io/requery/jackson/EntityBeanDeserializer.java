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

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonStreamContext;
import com.fasterxml.jackson.core.JsonTokenId;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.BeanDeserializer;
import com.fasterxml.jackson.databind.deser.BeanDeserializerBase;
import com.fasterxml.jackson.databind.deser.SettableBeanProperty;
import com.fasterxml.jackson.databind.deser.impl.ObjectIdReader;
import com.fasterxml.jackson.databind.deser.impl.ReadableObjectId;
import io.requery.util.ClassMap;

import java.io.IOException;
import java.lang.reflect.Method;

/**
 * Attempts to read the id property from the json stream first, lookup the existing bean with
 * that id and serialize the content into that bean. Unfortunately doesn't seem to be possible
 * to do this with the current Jackson api so handle it ourselves.
 */
class EntityBeanDeserializer extends BeanDeserializer {

    private final ClassMap<Method> embeddedGetters = new ClassMap<>();

    EntityBeanDeserializer(BeanDeserializerBase source, ObjectIdReader reader) {
        super(source, reader);
    }

    @Override
    public Object deserializeFromObject(JsonParser p, DeserializationContext ctxt) throws IOException {
        if (_nonStandardCreation || _needViewProcesing) {
            return super.deserializeFromObject(p, ctxt);
        }

        Object bean = null;

        if (p.hasTokenId(JsonTokenId.ID_FIELD_NAME)) {
            String propertyName = p.getCurrentName();
            do {
                p.nextToken();
                SettableBeanProperty property = _beanProperties.find(propertyName);

                if (property == null) {
                    handleUnknownVanilla(p, ctxt, bean, propertyName);
                    continue;
                }

                // lazily create the bean, the id property must be the first property
                if (bean == null) {
                    if (propertyName.equals(_objectIdReader.propertyName.getSimpleName())) {

                        // deserialize id
                        Object id = property.deserialize(p, ctxt);

                        ReadableObjectId objectId = ctxt.findObjectId(id,
                                _objectIdReader.generator, _objectIdReader.resolver);

                        bean = objectId == null ? null : objectId.resolve();
                        if (bean == null) {
                            bean = _valueInstantiator.createUsingDefault(ctxt);
                            property.set(bean, id);
                        }
                    } else {
                        bean = _valueInstantiator.createUsingDefault(ctxt);
                    }
                    p.setCurrentValue(bean);
                }
                property.deserializeAndSet(p, ctxt, bean);

            } while ((propertyName = p.nextFieldName()) != null);
        }

        return bean;
    }

    @Override
    protected Object deserializeFromObjectUsingNonDefault(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonStreamContext parent = p.getParsingContext().getParent();
        // handle embedded types
        if (parent != null && parent.getCurrentValue() != null) {

            Object value = parent.getCurrentValue();
            Class<?> parentClass = value.getClass();
            Method method = embeddedGetters.get(parentClass);

            if (method == null) {
                Class<?> target = getValueType().getRawClass();
                for (Method m : parentClass.getDeclaredMethods()) {
                    if (target.isAssignableFrom(m.getReturnType()) && m.getParameterTypes().length == 0) {
                        embeddedGetters.put(parentClass, m);
                        method = m;
                        break;
                    }
                }
            }
            if (method != null) {
                try {
                    return method.invoke(value);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }

        return super.deserializeFromObjectUsingNonDefault(p, ctxt);
    }
}
