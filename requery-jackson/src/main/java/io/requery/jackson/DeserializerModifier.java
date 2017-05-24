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

import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.deser.BeanDeserializer;
import com.fasterxml.jackson.databind.deser.BeanDeserializerModifier;
import com.fasterxml.jackson.databind.introspect.BeanPropertyDefinition;
import com.fasterxml.jackson.databind.introspect.POJOPropertyBuilder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

class DeserializerModifier extends BeanDeserializerModifier {

    @Override
    public JsonDeserializer<?> modifyDeserializer(DeserializationConfig config,
                                                  BeanDescription beanDesc,
                                                  JsonDeserializer<?> deserializer) {
        if (deserializer instanceof BeanDeserializer) {
            BeanDeserializer beanDeserializer = (BeanDeserializer) deserializer;
            return new EntityBeanDeserializer(beanDeserializer, deserializer.getObjectIdReader());
        }
        return super.modifyDeserializer(config, beanDesc, deserializer);
    }

    @Override
    public List<BeanPropertyDefinition> updateProperties(DeserializationConfig config,
                                                         BeanDescription beanDesc,
                                                         List<BeanPropertyDefinition> propDefs) {
        List<BeanPropertyDefinition> definitions = super.updateProperties(config, beanDesc, propDefs);
        List<BeanPropertyDefinition> remove = new ArrayList<>();
        List<BeanPropertyDefinition> add = new ArrayList<>();
        for (BeanPropertyDefinition definition : definitions) {
            if (definition.hasGetter() &&
                    Collection.class.isAssignableFrom(definition.getGetter().getRawType())) {

                if (definition instanceof POJOPropertyBuilder) {
                    POJOPropertyBuilder builder = (POJOPropertyBuilder) definition;
                    builder = new POJOPropertyBuilder(builder, builder.getFullName()) {
                        @Override
                        public boolean hasField() {
                            return false; // forces the getter to be used on the collection
                        }
                    };
                    remove.add(definition);
                    add.add(builder);
                }
            }
        }

        definitions.removeAll(remove);
        definitions.addAll(add);

        return definitions;
    }
}
