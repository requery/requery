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

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

/**
 * Converts from a {@link LocalDateTime} to a {@link java.sql.Timestamp} for Java 8. Note that
 * when converting between the time type and the database type all times will be converted to the
 * UTC zone offset.
 */
public class ZonedDateTimeConverter implements Converter<ZonedDateTime, java.sql.Timestamp> {

    @Override
    public Class<ZonedDateTime> getMappedType() {
        return ZonedDateTime.class;
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
    public java.sql.Timestamp convertToPersisted(ZonedDateTime value) {
        if (value == null) {
            return null;
        }
        Instant instant = value.toInstant();
        return java.sql.Timestamp.from(instant);
    }

    @Override
    public ZonedDateTime convertToMapped(Class<? extends ZonedDateTime> type,
                                         java.sql.Timestamp value) {
        if (value == null) {
            return null;
        }
        Instant instant = value.toInstant();
        return ZonedDateTime.ofInstant(instant, ZoneOffset.UTC);
    }
}
