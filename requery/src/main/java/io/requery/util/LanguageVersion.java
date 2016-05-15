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

/**
 * Similar to {@link javax.lang.model.SourceVersion} but also works on Android.
 *
 * @author Nikhil Purushe
 */
public enum LanguageVersion {

    JAVA_1_5,
    JAVA_1_6,
    JAVA_1_7,
    JAVA_1_8,
    JAVA_1_9;

    private static LanguageVersion version;

    static {
        try {
            String specificationVersion = System.getProperty("java.specification.version");
            switch (specificationVersion) {
                case "0.9":
                    // version returned by Android which can be treated as 1.7 language level as of
                    // KitKat
                    version = JAVA_1_7;
                    break;
                case "1.5":
                    version = JAVA_1_5;
                    break;
                case "1.6":
                    version = JAVA_1_6;
                    break;
                case "1.7":
                    version = JAVA_1_7;
                    break;
                case "1.8":
                    version = JAVA_1_8;
                    break;
                case "1.9":
                    version = JAVA_1_9;
                    break;
                default:
                    // assume latest (anything below 1.5 is not supported anyway)
                    version = JAVA_1_8;
            }
        } catch (SecurityException se) {
            version = JAVA_1_7; // lowest supported
        }
    }

    public static LanguageVersion current() {
        return version;
    }

    public boolean atLeast(LanguageVersion version) {
        return ordinal() >= version.ordinal();
    }
}
