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

package io.requery;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Annotation for defining view mapping information for an entity. Depending on the store type
 * a view entity maybe be queryable only and not updateable or insertable. View types are not
 * included in schema generation.
 */
@Documented
@Target(TYPE)
@Retention(RUNTIME)
public @interface View {

    /**
     * @return name of the view the object should be mapped to, defaults to the entity class name.
     */
    String name() default "";
}
