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

package io.requery;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Defines a 1-1 relationship for an entity field.
 */
@Documented
@Target({FIELD, METHOD})
@Retention(RUNTIME)
public @interface OneToOne {

    /**
     * @return the property name in the type this relation is mapped to. Optional normally this can
     * be discovered by the entity processor.
     */
    String mappedBy() default "";

    /**
     * @return the set of {@link CascadeAction} actions to take when the entity containing this
     * reference is persisted/deleted. Defaults to {@link CascadeAction#SAVE}.
     */
    CascadeAction[] cascade() default CascadeAction.SAVE;
}
