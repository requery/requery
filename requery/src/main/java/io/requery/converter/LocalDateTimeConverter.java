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

import java.time.LocalDateTime;

/**
 * Converts from a {@link LocalDateTime} to a {@link java.sql.Timestamp} for Java 8.
 */
public class LocalDateTimeConverter implements Converter<LocalDateTime, java.sql.Timestamp> {

    @Override
    public Class<LocalDateTime> getMappedType() {
        return LocalDateTime.class;
    }

    @Override
    public Class<java.sql.Timestamp> getPersistedType() {
        return java.sql.Timestamp.class;
    }

    @Override
    public Integer getPersistedSize() {
        return null;
    }

    @Override
    public java.sql.Timestamp convertToPersisted(LocalDateTime value) {
        if (value == null) {
            return null;
        }
        return java.sql.Timestamp.valueOf(value);
    }

    @Override
    public LocalDateTime convertToMapped(Class<? extends LocalDateTime> type,
                                         java.sql.Timestamp value) {
        if (value == null) {
            return null;
        }
        return value.toLocalDateTime();
    }
}
