/*
 * Copyright 2018 requery.io
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

package io.requery.android

import android.net.Uri
import io.requery.Converter

/**
 * Converter for persisting a [android.net.Uri] in an entity.
 *
 * @author Nikhil Purushe
 */
class UriConverter : Converter<Uri, String> {

    override fun getMappedType(): Class<Uri> {
        return Uri::class.java
    }

    override fun getPersistedType(): Class<String> {
        return String::class.java
    }

    override fun getPersistedSize(): Int? {
        return null
    }

    override fun convertToPersisted(value: Uri?): String? {
        return value?.toString()
    }

    override fun convertToMapped(type: Class<out Uri>, value: String?): Uri? {
        return if (value == null) null else Uri.parse(value)
    }
}
