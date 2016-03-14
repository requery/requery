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

public interface Gettable<E> {

    /**
     * get the current value for an attribute through this proxy.
     *
     * @param attribute to get
     * @param <V>       type of the attribute
     * @return the current value of that attribute using {@link PropertyLoader} instance to
     * retrieve it if required.
     */
    <V> V get(Attribute<E, V> attribute);

    /**
     * get the current value for an attribute through this proxy.
     *
     * @param attribute to get
     * @param fetch     true to fetch the value through the {@link PropertyLoader} if set
     * @param <V>       type fo the attribute
     * @return the current value of that attribute using {@link PropertyLoader} instance to
     * retrieve it if fetch = true.
     */
    <V> V get(Attribute<E, V> attribute, boolean fetch);

    boolean getBoolean(Attribute<E, Boolean> attribute);

    byte getByte(Attribute<E, Byte> attribute);

    short getShort(Attribute<E, Short> attribute);

    int getInt(Attribute<E, Integer> attribute);

    long getLong(Attribute<E, Long> attribute);

    float getFloat(Attribute<E, Float> attribute);

    double getDouble(Attribute<E, Double> attribute);
}
