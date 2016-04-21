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

import android.os.Parcel;
import android.os.Parcelable;
import io.requery.Converter;

/**
 * Base {@link Converter} for persisting {@link Parcelable} Android objects. For example if you
 * wanted to store a custom parcelable as a SQLite BLOB you would extend this class
 * as follows:
 * <pre><code>
 * public class MyParcelableConverter<MyParcelable> extends ParcelConverter<MyParcelable> {
 *
 *    public MyParcelableConverter() {
 *        super(MyParcelable.class, MyParcelable.CREATOR);
 *    }
 * }
 * </code></pre>
 * <p>
 * and specify the class in the {@link Converter} annotation on the field to persist. Note be
 * careful when using with Android Parcelable classes as the internal format of the object may
 * change between versions.
 *
 * @author Nikhil Purushe
 */
public class ParcelConverter<T extends Parcelable> implements Converter<T, byte[]> {

    private final Class<T> type;
    private final Parcelable.Creator<T> creator;

    public ParcelConverter(Class<T> type, Parcelable.Creator<T> creator) {
        if (type == null || creator == null) {
            throw new IllegalArgumentException();
        }
        this.type = type;
        this.creator = creator;
    }

    @Override
    public Class<T> mappedType() {
        return type;
    }

    @Override
    public Class<byte[]> persistedType() {
        return byte[].class;
    }

    @Override
    public Integer persistedSize() {
        return null;
    }

    @Override
    public byte[] convertToPersisted(T value) {
        if (value != null) {
            Parcel parcel = Parcel.obtain();
            value.writeToParcel(parcel, 0);
            return parcel.marshall();
        }
        return null;
    }

    @Override
    public T convertToMapped(Class<? extends T> type, byte[] value) {
        if (value == null) {
            return null;
        }
        Parcel parcel = Parcel.obtain();
        parcel.unmarshall(value, 0, value.length);
        return creator.createFromParcel(parcel);
    }
}
