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

package io.requery.meta;

import io.requery.util.Objects;

import java.util.HashSet;
import java.util.Set;

/**
 * Builds information about an entity model by defining the specific {@link Type}s that define it.
 *
 * @author Nikhil Purushe
 */
public class EntityModelBuilder {

    private final String name;
    private final Set<Type<?>> types = new HashSet<>();

    /**
     * Creates a new {@link EntityModel} instance.
     *
     * @param name of the model
     */
    public EntityModelBuilder(String name) {
        this.name = name;
    }

    /**
     * Adds a {@link Type} instance to the model.
     * @param type type to add
     * @return the builder instance.
     */
    public EntityModelBuilder addType(Type<?> type) {
        types.add(Objects.requireNotNull(type));
        return this;
    }

    /**
     * @return An immutable {@link EntityModel} instance with the defined types.
     */
    public EntityModel build() {
        return new ImmutableEntityModel(name, types);
    }
}
