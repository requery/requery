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

package io.requery.meta;

/**
 * Represents a primitive type kind.
 *
 * @author Nikhil Purushe
 */
public enum PrimitiveKind {

    INT,
    LONG,
    SHORT,
    BOOLEAN,
    FLOAT,
    DOUBLE,
    CHAR,
    BYTE;

    static PrimitiveKind fromClass(Class type) {
        if (type.isPrimitive()) {
            if (type == int.class) {
                return INT;
            } else if (type == long.class) {
                return LONG;
            } else if (type == short.class) {
                return SHORT;
            } else if (type == float.class) {
                return FLOAT;
            } else if (type == double.class) {
                return DOUBLE;
            } else if (type == boolean.class) {
                return BOOLEAN;
            } else if (type == char.class) {
                return CHAR;
            } else if (type == byte.class) {
                return BYTE;
            }
        }
        return null;
    }
}
