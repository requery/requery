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

import java.nio.ByteBuffer;
import java.util.UUID;

/**
 * Converts between a java UUID instance and a 16 byte stored value.
 */
public class UUIDConverter implements Converter<UUID, byte[]> {

    @Override
    public Class<UUID> mappedType() {
        return UUID.class;
    }

    @Override
    public Class<byte[]> persistedType() {
        return byte[].class;
    }

    @Override
    public Integer persistedSize() {
        return 16;
    }

    @Override
    public byte[] convertToPersisted(UUID value) {
        if (value == null) {
            return null;
        }
        byte[] bytes = new byte[16];
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        buffer.putLong(value.getMostSignificantBits());
        buffer.putLong(value.getLeastSignificantBits());
        return bytes;
    }

    @Override
    public UUID convertToMapped(Class<? extends UUID> type, byte[] value) {
        if (value == null) {
            return null;
        }
        return UUID.nameUUIDFromBytes(value);
    }
}
