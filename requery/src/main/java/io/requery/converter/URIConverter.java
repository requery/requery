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

import java.net.URI;

/**
 * Converter for a {@link URI}
 */
public class URIConverter implements Converter<URI, String> {

    @Override
    public Class<URI> mappedType() {
        return URI.class;
    }

    @Override
    public Class<String> persistedType() {
        return String.class;
    }

    @Override
    public Integer persistedSize() {
        return null;
    }

    @Override
    public String convertToPersisted(URI value) {
        return value == null ? null : value.toString();
    }

    @Override
    public URI convertToMapped(Class<? extends URI> type, String value) {
        return value == null ? null : URI.create(value);
    }
}
