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
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/**
 * Converts from a {@link OffsetDateTime} to a {@link java.sql.Timestamp} for Java 8. Note that
 * when converting between the time type and the database type all times will be converted to the
 * UTC zone offset.
 */
public class OffsetDateTimeConverter implements Converter<OffsetDateTime, java.sql.Timestamp> {

    @Override
    public Class<OffsetDateTime> getMappedType() {
        return OffsetDateTime.class;
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
    public java.sql.Timestamp convertToPersisted(OffsetDateTime value) {
        if (value == null) {
            return null;
        }
        Instant instant = value.toInstant();
        return java.sql.Timestamp.from(instant);
    }

    @Override
    public OffsetDateTime convertToMapped(Class<? extends OffsetDateTime> type,
                                          java.sql.Timestamp value) {
        if (value == null) {
            return null;
        }
        return OffsetDateTime.ofInstant(value.toInstant(), ZoneOffset.UTC);
    }
}
