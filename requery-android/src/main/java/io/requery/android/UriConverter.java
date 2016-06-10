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

package io.requery.android;

import android.net.Uri;
import io.requery.Converter;

/**
 * Converter for persisting a {@link android.net.Uri} in an entity.
 *
 * @author Nikhil Purushe
 */
public class UriConverter implements Converter<Uri, String> {

    @Override
    public Class<Uri> getMappedType() {
        return Uri.class;
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
    public String convertToPersisted(Uri value) {
        return value == null ? null : value.toString();
    }

    @Override
    public Uri convertToMapped(Class<? extends Uri> type, String value) {
        return value == null ? null : Uri.parse(value);
    }
}
