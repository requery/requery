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

package io.requery.converter;

import io.requery.Converter;

/**
 * Converter which persists an {@link Enum} using its {@link Enum#ordinal} representation.
 *
 * @param <E> type of enum
 */
public class EnumOrdinalConverter<E extends Enum> implements Converter<E, Integer> {

    private final Class<E> enumClass;

    public EnumOrdinalConverter(Class<E> enumClass) {
        this.enumClass = enumClass;
    }

    @Override
    public Class<E> getMappedType() {
        return enumClass;
    }

    @Override
    public Class<Integer> getPersistedType() {
        return Integer.class;
    }

    @Override
    public Integer getPersistedSize() {
        return null;
    }

    @Override
    public Integer convertToPersisted(E value) {
        return value == null ? null : value.ordinal();
    }

    @Override
    public E convertToMapped(Class<? extends E> type, Integer value) {
        if (value == null) {
            return null;
        }
        return getMappedType().getEnumConstants()[value];
    }
}
