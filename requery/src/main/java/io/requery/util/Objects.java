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

package io.requery.util;

import java.util.Arrays;
import java.util.Comparator;

/**
 * Identical to {@link java.util.Objects} except does not contain an implementation of
 * {@link java.util.Objects#deepEquals}. Added for Android compatibility since the Java 1.7
 * API is not available before Android KitKat 4.4.
 *
 * @author Nikhil Purushe
 */
public final class Objects {

    private Objects() {
    }

    public static <T> T requireNotNull(T o) {
        if (o == null) {
            throw new NullPointerException();
        }
        return o;
    }

    public static <T> T requireNotNull(T o, String message) {
        if (o == null) {
            throw new NullPointerException(message);
        }
        return o;
    }

    public static boolean equals(Object a, Object b) {
        return a == b || a != null && a.equals(b);
    }

    public static String toString(Object o, String nullString) {
        return o == null ? nullString : o.toString();
    }

    public static String toString(Object o) {
        return toString(o, "null");
    }

    public static int hash(Object... values) {
        return Arrays.hashCode(values);
    }

    public static int hashCode(Object o) {
        return o == null ? 0 : o.hashCode();
    }

    public static <T> int compare(T a, T b, Comparator<? super T> c) {
        return (a == b) ? 0 : c.compare(a, b);
    }
}
