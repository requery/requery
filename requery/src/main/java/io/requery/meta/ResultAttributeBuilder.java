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

import io.requery.proxy.ResultInitializer;
import io.requery.util.Objects;

public class ResultAttributeBuilder<T, V extends Iterable<E>, E> extends AttributeBuilder<T, V> {

    @SuppressWarnings("unchecked")
    public ResultAttributeBuilder(String name, Class<? extends Iterable> type,
                                  Class<E> elementType) {
        super(name, (Class<V>) type);
        this.elementClass = Objects.requireNotNull(elementType);
        setInitializer(new ResultInitializer<V>());
    }
}
