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

package io.requery.proxy;

import io.requery.meta.Attribute;
import io.requery.query.Expression;
import io.requery.query.MutableTuple;

import java.util.Map;

/**
 * Represents a composite key of multiple values.
 *
 * @param <T> entity type
 *
 * @author Nikhil Purushe
 */
public class CompositeKey<T> extends MutableTuple {

    /**
     * Creates a new composite key instance.
     *
     * @param values a map of key {@link Attribute} to their corresponding values.
     */
    public CompositeKey(Map<? extends Attribute<T, ?>, ?> values) {
        super(values.size());
        if (values.isEmpty()) {
            throw new IllegalArgumentException();
        }
        int index = 0;
        for (Map.Entry<? extends Attribute<T, ?>, ?> entry : values.entrySet()) {
            set(index++, (Expression<?>) entry.getKey(), entry.getValue());
        }
    }
}
