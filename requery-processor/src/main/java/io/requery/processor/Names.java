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

package io.requery.processor;

import javax.lang.model.SourceVersion;

/**
 * Naming utility class.
 *
 * @author Nikhil Purushe
 */
final class Names {

    private Names() {
    }

    public static boolean isEmpty(CharSequence name) {
        return name == null || name.toString().isEmpty();
    }

    public static String lowerCaseFirst(CharSequence name) {
        StringBuilder sb = new StringBuilder(name);
        sb.setCharAt(0, Character.toLowerCase(sb.charAt(0)));
        return sb.toString();
    }

    public static String upperCaseFirst(CharSequence name) {
        StringBuilder sb = new StringBuilder(name);
        sb.setCharAt(0, Character.toUpperCase(sb.charAt(0)));
        return sb.toString();
    }

    public static String upperCaseUnderscore(CharSequence name) {
        StringBuilder sb = new StringBuilder(name);
        boolean wasLower = false;
        for (int i = 0; i < sb.length(); i++) {
            char c = sb.charAt(i);
            if (Character.isUpperCase(c) && wasLower) {
                sb.insert(i, "_");
                wasLower = false;
            } else {
                wasLower = Character.isLowerCase(c);
            }
        }
        return sb.toString().toUpperCase();
    }

    public static boolean isAllUpper(CharSequence name) {
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (!Character.isUpperCase(c)) {
                return false;
            }
        }
        return true;
    }

    public static String removeMemberPrefixes(CharSequence name) {
        String string = name.toString();
        if (string.startsWith("_")) {
            return string.substring(1);
        }
        // detect mSomething names, which are common in Android apps
        if (string.length() > 1 && string.startsWith("m") && Character.isUpperCase(string.charAt(1))) {
            return string.substring(1);
        }
        return string;
    }

    public static String removeMethodPrefixes(CharSequence name) {
        String string = name.toString();
        // getSomething/setSomething -> Something
        if (string.startsWith("get") || string.startsWith("set")) {
            return string.substring(3);
        }
        // isSomething() -> Something
        if (string.startsWith("is") && string.length() > 2 && Character.isUpperCase(string.charAt(2))) {
            return string.substring(2);
        }
        return string;
    }

    public static String removeClassPrefixes(CharSequence name) {
        String typeName = name.toString();
        if (typeName.startsWith("Abstract")) {
            return typeName.replaceFirst("Abstract", "");
        }
        if (typeName.startsWith("Base")) {
            return typeName.replaceFirst("Base", "");
        }
        return typeName;
    }

    public static String checkIfAttributeNameNotForbidden(CharSequence newName, CharSequence fallback) {
        return SourceVersion.isName(newName) ? newName.toString() : fallback.toString();
    }
}
