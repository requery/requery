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

public interface Settable<E> {

    /**
     * Set a value through this proxy and change it's corresponding {@link PropertyState} to
     * {@link PropertyState#MODIFIED}.
     *
     * @param attribute attribute to change
     * @param value     new property value
     * @param <V>       type of the value
     */
    <V> void set(Attribute<E, V> attribute, V value);

    /**
     * Set a value through this proxy and change it's corresponding {@link PropertyState}.
     *
     * @param attribute attribute to change
     * @param value     new property value
     * @param state     new property state
     * @param <V>       type of the value
     */
    <V> void set(Attribute<E, V> attribute, V value, PropertyState state);

    /**
     * Unchecked version of set, use only when the the attribute type is not known.
     *
     * @param attribute attribute to change
     * @param value     new property value
     * @param state     new property state
     */
    void setObject(Attribute<E, ?> attribute, Object value, PropertyState state);

    void setBoolean(Attribute<E, Boolean> attribute, boolean value, PropertyState state);

    void setByte(Attribute<E, Byte> attribute, byte value, PropertyState state);

    void setShort(Attribute<E, Short> attribute, short value, PropertyState state);

    void setInt(Attribute<E, Integer> attribute, int value, PropertyState state);

    void setLong(Attribute<E, Long> attribute, long value, PropertyState state);

    void setDouble(Attribute<E, Double> attribute, double value, PropertyState state);

    void setFloat(Attribute<E, Float> attribute, float value, PropertyState state);
}
