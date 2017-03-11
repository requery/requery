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

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.requery.EntityStore;
import io.requery.meta.EntityModel;

/**
 * Provides a preconfigured ObjectMapper that can be used to serialize and deserialize requery
 * entity objects using Jackson Databind.
 *
 * @author Nikhil Purushe
 */
public class EntityMapper extends ObjectMapper {

    /**
     * Creates a new mapper instance.
     * @param model entity model being serialized
     * @param store entity store instance
     */
    public EntityMapper(EntityModel model, final EntityStore store) {
        if (model == null) {
            throw new IllegalArgumentException("model cannot be null");
        }
        if (store == null) {
            throw new IllegalArgumentException("store cannot be null");
        }
        enable(MapperFeature.USE_GETTERS_AS_SETTERS);
        registerModule(new RequeryModule(model));
        setHandlerInstantiator(new ResolverInstantiator(store));
    }
}
