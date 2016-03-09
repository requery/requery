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

import io.requery.util.function.Supplier;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Defines a {@link Supplier} used to create a particular {@link Entity} class. By default the
 * persistence processor will generate a factory that creates the type using the default zero
 * argument constructor, however it may be desirable to have an entity with constructor
 * parameters. When that this the case this annotation must be specified on the entity defining a
 * {@link Supplier} class that will instantiate the entity. Example:
 * <pre><code>
 *     &#064;Entity
 *     &#064;Factory(PhoneFactory.class)
 *     public class AbstractPhone {
 *         String phone;
 *         AbstractPhone(String phone) {
 *             this.phone = phone
 *         }
 *     }
 *     ...
 *     public class PhoneFactory implements Factory&lt;Phone&gt; {
 *         &#064;Override
 *         public Phone create() {
 *            return new Phone("555-5555");
 *         }
 *     }
 * </code></pre>
 *
 * @author Nikhil Purushe
 */
@Documented
@Target(TYPE)
@Retention(RUNTIME)
public @interface Factory {

    /**
     * @return {@link Supplier} class used to provide new instances of the
     * annotated element. This class must have zero argument constructor.
     */
    Class<? extends Supplier<?>> value();
}

