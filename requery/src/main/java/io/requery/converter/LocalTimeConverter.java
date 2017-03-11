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

import java.time.LocalTime;

/**
 * Converts from a {@link LocalTime} to a {@link java.sql.Time} for Java 8.
 */
public class LocalTimeConverter implements Converter<LocalTime, java.sql.Time> {

    @Override
    public Class<LocalTime> getMappedType() {
        return LocalTime.class;
    }

    @Override
    public Class<java.sql.Time> getPersistedType() {
        return java.sql.Time.class;
    }

    @Override
    public Integer getPersistedSize() {
        return null;
    }

    @Override
    public java.sql.Time convertToPersisted(LocalTime value) {
        if (value == null) {
            return null;
        }
        return java.sql.Time.valueOf(value);
    }

    @Override
    public LocalTime convertToMapped(Class<? extends LocalTime> type, java.sql.Time value) {
        if (value == null) {
            return null;
        }
        return value.toLocalTime();
    }
}
