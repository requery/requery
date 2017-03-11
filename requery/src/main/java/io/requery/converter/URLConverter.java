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
import io.requery.PersistenceException;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * Converter for a {@link URL}
 */
public class URLConverter implements Converter<URL, String> {

    @Override
    public Class<URL> getMappedType() {
        return URL.class;
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
    public String convertToPersisted(URL value) {
        return value == null ? null : value.toString();
    }

    @Override
    public URL convertToMapped(Class<? extends URL> type, String value) {
        if (value == null) {
            return null;
        }
        try {
            return new URL(value);
        } catch (MalformedURLException e) {
            throw new PersistenceException(e);
        }
    }
}
