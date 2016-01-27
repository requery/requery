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

/**
 * Interface for converting between a java custom type to a basic java persisted type. Example a
 * conversion to store a java URI as a string. Would define a class like:
 * <pre>
 * <code>
 *     public class URIConverter implements Converter&lt;URI, String&gt; {
 *     ...
 *     }
 * </code>
 * </pre>
 * and implement the corresponding methods for converting the URI to and from a string. Then it can
 * be registered via mapping or in the entity class like so:
 *
 * <pre><code>
 *     {@literal @}Entity
 *     public class AbstractUser {
 *         {@literal @}Convert(URIConverter.class)
 *         URI homepage
 *     }
 * }
 * </code></pre>
 *
 * When used with the {@link Convert} annotation the converter must have a no argument constructor
 * or compilation will fail.
 *
 * @see Convert
 *
 * @param <A> the mapped type
 * @param <B> the stored persisted type
 *
 * @author Nikhil Purushe
 */
public interface Converter<A, B> {

    /**
     * @return the type to be converted.
     */
    Class<A> mappedType();

    /**
     * @return the persisted type.
     */
    Class<B> persistedType();

    /**
     * @return size or length of the persisted type (optional) in bytes otherwise return null.
     */
    Integer persistedSize();

    /**
     * Convert the mapped type A to the persisted type B.
     *
     * @param value to convert
     * @return converted value
     */
    B convertToPersisted(A value);

    /**
     * Convert the persisted type B to the mapped type A.
     *
     * @param type  the type to convert
     * @param value value to convert
     * @return converted value
     */
    A convertToMapped(Class<? extends A> type, B value);
}