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

import java.util.Map;

public class MapAttributeBuilder<T, M extends Map<K, V>, K, V> extends AttributeBuilder<T, M> {

    @SuppressWarnings("unchecked")
    public MapAttributeBuilder(String name, Class<? extends Map> type,
                               Class<K> keyType, Class<V> valueType) {
        super(name, (Class<M>) type);
        this.mapKeyClass = Objects.requireNotNull(keyType);
        this.elementClass = Objects.requireNotNull(valueType);
    }
}
