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
 * Indicates a field as being an indexed column in a database table.
 *
 * Example:
 * <pre><code>
 *    {@literal @}Entity
 *     public class AbstractPerson {
 *         ...
 *        {@literal @}Index(value = "email_index") String email;
 *         ...
 *     }
 * </code></pre>
 */
@Documented
@Target({FIELD, METHOD})
@Retention(RUNTIME)
public @interface Index {

    /**
     * @return name(s) of the index this column belongs to. If empty a generic name will be created
     * for the index. If the multiple columns in the same entity have same index name they will be
     * created as one multi column index.
     */
    String[] value() default "";
}
