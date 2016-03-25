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

import com.squareup.javapoet.TypeName;

/**
 * Contains various properties describing how generate to a {@link io.requery.proxy.Property}.
 */
class GeneratedProperty {

    private final String readName;
    private final String writeName;
    private final String methodSuffix;
    private final TypeName targetName;
    private final TypeName propertyTypeName;
    private final boolean useMethod;
    private final boolean isNullable;
    private final boolean isReadOnly;
    private final boolean isWriteOnly;

    private GeneratedProperty(String readName,
                              String writeName,
                              TypeName targetName,
                              TypeName propertyTypeName,
                              String methodSuffix,
                              boolean useMethod,
                              boolean isNullable,
                              boolean isReadOnly,
                              boolean isWriteOnly) {
        this.readName = readName;
        this.writeName = writeName;
        this.targetName = targetName;
        this.propertyTypeName = propertyTypeName;
        this.methodSuffix = methodSuffix;
        this.useMethod = useMethod;
        this.isNullable = isNullable;
        this.isReadOnly = isReadOnly;
        this.isWriteOnly = isWriteOnly;
    }

    String readName() {
        return readName;
    }

    String writeName() {
        return writeName;
    }

    String methodSuffix() {
        return methodSuffix;
    }

    boolean useMethod() {
        return useMethod;
    }

    TypeName targetName() {
        return targetName;
    }

    TypeName propertyTypeName() {
        return propertyTypeName;
    }

    boolean isNullable() {
        return isNullable;
    }

    boolean isReadOnly() {
        return isReadOnly;
    }

    boolean isWriteOnly() {
        return isWriteOnly;
    }

    static class Builder {

        private String readName;
        private String writeName;
        private String methodSuffix;
        private TypeName targetName;
        private TypeName propertyTypeName;
        private boolean useMethod;
        private boolean isNullable;
        private boolean isReadOnly;
        private boolean isWriteOnly;

        Builder(String propertyName, TypeName targetName, TypeName propertyTypeName) {
            this(propertyName, propertyName, targetName, propertyTypeName);
        }

        Builder(String readName,
                String writeName,
                TypeName targetName,
                TypeName propertyTypeName) {
            this.methodSuffix = "";
            this.readName = readName;
            this.writeName = writeName;
            this.targetName = targetName;
            this.propertyTypeName = propertyTypeName;
        }

        Builder setUseMethod(boolean useMethod) {
            this.useMethod = useMethod;
            return this;
        }

        Builder setSuffix(String suffix) {
            this.methodSuffix = suffix;
            return this;
        }

        Builder setNullable(boolean nullable) {
            this.isNullable = nullable;
            return this;
        }

        Builder setReadOnly(boolean readOnly) {
            this.isReadOnly = readOnly;
            return this;
        }

        Builder setWriteOnly(boolean writeOnly) {
            this.isWriteOnly = writeOnly;
            return this;
        }

        GeneratedProperty build() {
            return new GeneratedProperty(
                readName,
                writeName,
                targetName,
                propertyTypeName,
                methodSuffix,
                useMethod,
                isNullable,
                isReadOnly,
                isWriteOnly);
        }
    }
}
