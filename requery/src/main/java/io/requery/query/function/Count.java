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

package io.requery.query.function;

import io.requery.meta.Attribute;

public class Count extends Function<Integer> {

    private Attribute<?,?>[] attributes;
    private Class<?> entityClass;

    private Count(Attribute<?,?>[] attributes) {
        super("count", Integer.class);
        this.attributes = attributes;
    }

    private Count(Class<?> entityClass) {
        super("count", Integer.class);
        this.entityClass = entityClass;
    }

    public static Count count(Attribute<?,?>... attributes) {
        return new Count(attributes);
    }

    public static Count count(Class<?> entityClass) {
        return new Count(entityClass);
    }

    public Attribute<?, ?>[] getAttributes() {
        return attributes;
    }

    @Override
    public Object[] arguments() {
        if(entityClass != null) {
            return new Object[] { entityClass };
        } else {
            return attributes;
        }
    }
}
