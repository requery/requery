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
 * Indicates a field as being a column in a database table.
 *
 * @author Nikhil Purushe
 */
@Documented
@Target({FIELD, METHOD})
@Retention(RUNTIME)
public @interface Column {

    /**
     * @return Name of the column. If empty then defaults to the field name during table definition.
     */
    String name() default "";

    /**
     * @return true if this column is nullable or false otherwise. Defaults to true (no constraint).
     */
    boolean nullable() default true;

    /**
     * @return true if this column is uniquely constrained false otherwise.
     */
    boolean unique() default false;

    /**
     * @return true if this column is indexed false otherwise. Use {@link Index} to define the index
     * options.
     */
    boolean index() default false;

    /**
     * @return optional fixed length for the column.
     */
    int length() default 0;

    /**
     * @return optional default value expression for the column used during table creation. example:
     * <pre><code>
     *     &#064;Column(value = "now()")
     *     protected Timestamp timestamp
     * </code></pre>
     */
    String value() default "";

    /**
     * @return optional collation type for the column, types varies depending on the platform.
     */
    String collate() default "";

    /**
     * @return optional table column definition used during table generation.
     */
    String definition() default "";

    /**
     * @return optional foreign key constraints for this column.
     */
    ForeignKey[] foreignKey() default {};
}
