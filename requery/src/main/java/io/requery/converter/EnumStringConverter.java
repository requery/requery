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
 * Converter which persists an {@link Enum} using its {@link Enum#name()} representation.
 *
 * @param <E> type of enum
 */
public class EnumStringConverter<E extends Enum> implements Converter<E, String> {

    private final Class<E> enumClass;

    public EnumStringConverter(Class<E> enumClass) {
        this.enumClass = enumClass;
    }

    @Override
    public Class<E> getMappedType() {
        return enumClass;
    }

    @Override
    public Class<String> getPersistedType() {
        return String.class;
    }

    @Override
    public Integer getPersistedSize() {
        return null;
    }

    @Override
    public String convertToPersisted(Enum value) {
        return value == null ? null : value.name();
    }

    @Override
    public E convertToMapped(Class<? extends E> type, String value) {
        if (value == null) {
            return null;
        }
        return type.cast(Enum.valueOf(type, value));
    }
}
