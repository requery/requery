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
import javax.lang.model.element.Name;

/**
 * Represents the fully qualified name of a class or interface.
 *
 * @author Nikhil Purushe
 */
class QualifiedName implements Name {

    private final String packageName;
    private final String className;

    QualifiedName(String packageName, String className) {
        if (Names.isEmpty(className)) {
            throw new IllegalArgumentException("Empty class name");
        }
        if (!SourceVersion.isIdentifier(className)) {
            throw new IllegalArgumentException("Invalid class name identifier: " + className);
        }
        this.packageName = packageName;
        this.className = className;
    }

    QualifiedName(String qualifiedName) {
        String[] parts = qualifiedName.split("\\.");
        String className = "";
        StringBuilder sb = new StringBuilder();
        if (parts.length != 0) {
            for (String part : parts) {
                if (Character.isLowerCase(part.charAt(0))) {
                    sb.append(part);
                    sb.append(".");
                } else {
                    className = part;
                }
            }
        } else {
            className = qualifiedName;
        }
        if (sb.length() > 0 && sb.charAt(sb.length() - 1) == '.') {
            sb.deleteCharAt(sb.length() - 1);
        }
        if (Names.isEmpty(className)) {
            throw new IllegalArgumentException("Empty class name");
        }
        if (!SourceVersion.isIdentifier(className)) {
            throw new IllegalArgumentException("Invalid class name identifier: " + className);
        }
        this.packageName = sb.toString();
        this.className = className;
    }

    String packageName() {
        return packageName;
    }

    String className() {
        return className;
    }

    @Override
    public boolean contentEquals(CharSequence cs) {
        return toString().equals(cs.toString());
    }

    @Override
    public int length() {
        return toString().length();
    }

    @Override
    public char charAt(int index) {
        return toString().charAt(index);
    }

    @Override
    public CharSequence subSequence(int start, int end) {
        return toString().subSequence(start, end);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Name) {
            Name other = (Name) obj;
            return other.contentEquals(toString());
        }
        return false;
    }

    @SuppressWarnings("NullableProblems")
    @Override
    public String toString() {
        return Names.isEmpty(packageName) ? className : packageName + "." + className;
    }
}
