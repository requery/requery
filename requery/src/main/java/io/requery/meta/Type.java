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

import io.requery.Table;
import io.requery.proxy.EntityProxy;
import io.requery.query.Expression;
import io.requery.util.function.Function;
import io.requery.util.function.Supplier;

import java.util.Set;

/**
 * Represents type information about an entity class.
 *
 * @param <T> entity java type
 *
 * @author Nikhil Purushe
 */
public interface Type<T> extends Expression<T> {

    /**
     * @return the type name, note this can be different from the class name.
     */
    @Override
    String name();

    /**
     * @return the Java {@link Class} for which this type information is for.
     */
    @Override
    Class<T> classType();

    /**
     * @return the base type this entity is representing if any.
     */
    Class<?> baseType();

    /**
     * @return true if instances of this type can be cached for reuse, false otherwise.
     */
    boolean isCacheable();

    /**
     * @return true if instances of this type are read only and can't be modified, false otherwise.
     */
    boolean isReadOnly();

    /**
     * @return true if the type has all state tracking disabled, false otherwise.
     */
    boolean isStateless();

    /**
     * @return true if a builder class is used to construct instances of the type when reading from
     * a store or query. Generally used for immutable objects.
     */
    boolean isBuildable();

    /**
     * @param <B> builder type
     * @return if {@link #isBuildable()} returns the factory that instantiates the builder instance,
     * otherwise null.
     */
    <B> Supplier<B> builderFactory();

    /**
     * @param <B> builder type
     * @return if {@link #isBuildable()} returns the function that builds the final type from
     * a builder instance.
     */
    <B> Function<B, T> buildFunction();

    /**
     * @return A readonly collection of {@link Attribute}s that this type represents. (includes
     * all attributes)
     */
    Set<Attribute<T, ?>> attributes();

    /**
     * @return A readonly collection of {@link Attribute}s that are the key for this type.
     */
    Set<Attribute<T, ?>> keyAttributes();

    /**
     * @return If there is only a single key that key's attribute, otherwise null.
     */
    Attribute<T, ?> singleKeyAttribute();

    /**
     * @return {@link Supplier} instance used to provide new instances of {@link #classType()}.
     */
    Supplier<T> factory();

    /**
     * @return {@link Function} provider for retrieving the {@link EntityProxy} for a
     * given entity instance.
     */
    Function<T, EntityProxy<T>> proxyProvider();

    /**
     * @return optional table creation attributes {@link Table#createAttributes()}
     */
    String[] tableCreateAttributes();
}
