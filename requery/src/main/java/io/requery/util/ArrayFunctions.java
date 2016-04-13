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

import io.requery.util.function.Consumer;

/**
 * Runs a {@link Consumer} on primitive and object array types. Can be replaced with Java 8
 * streams in a future version.
 */
public class ArrayFunctions {

    private ArrayFunctions() {
    }

    public static void forEach(boolean[] array, Consumer<? super Boolean> consumer) {
        for (boolean i : array) {
            consumer.accept(i);
        }
    }

    public static void forEach(byte[] array, Consumer<? super Byte> consumer) {
        for (byte i : array) {
            consumer.accept(i);
        }
    }

    public static void forEach(char[] array, Consumer<? super Character> consumer) {
        for (char i : array) {
            consumer.accept(i);
        }
    }

    public static void forEach(short[] array, Consumer<? super Short> consumer) {
        for (short i : array) {
            consumer.accept(i);
        }
    }

    public static void forEach(int[] array, Consumer<? super Integer> consumer) {
        for (int i : array) {
            consumer.accept(i);
        }
    }

    public static void forEach(long[] array, Consumer<? super Long> consumer) {
        for (long i : array) {
            consumer.accept(i);
        }
    }

    public static void forEach(double[] array, Consumer<? super Double> consumer) {
        for (double i : array) {
            consumer.accept(i);
        }
    }

    public static void forEach(float[] array, Consumer<? super Float> consumer) {
        for (float i : array) {
            consumer.accept(i);
        }
    }

    public static void forEach(Object[] array, Consumer<? super Object> consumer) {
        for (Object i : array) {
            consumer.accept(i);
        }
    }
}
