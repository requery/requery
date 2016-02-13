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

import java.util.Set;

/**
 * The top level meta information for a collection of entity classes in a persistence model.
 *
 * @author Nikhil Purushe
 */
public interface EntityModel {

    /**
     * @return the name of the model (if given) otherwise an empty string.
     */
    String name();

    /**
     * Retrieves the meta {@link Type} information for the given entity class.
     *
     * @param entityClass entity class
     * @param <T>         entity type
     * @return The {@link Type} representing the given entity class.
     */
    <T> Type<T> typeOf(Class<? extends T> entityClass) throws NotMappedException;

    /**
     * @return Read only collection of all {@link Type} elements in this model.
     */
    Set<Type<?>> allTypes();
}
