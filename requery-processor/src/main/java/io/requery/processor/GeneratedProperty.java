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

    private final String attributeName;
    private final String methodSuffix;
    private final TypeName targetName;
    private final TypeName propertyTypeName;
    private final boolean isSetter;
    private final boolean isNullable;
    private final boolean isReadOnly;
    private final boolean isWriteOnly;

    private GeneratedProperty(String attributeName,
                              TypeName targetName,
                              TypeName propertyTypeName,
                              String methodSuffix,
                              boolean isSetter,
                              boolean isNullable,
                              boolean isReadOnly,
                              boolean isWriteOnly) {
        this.attributeName = attributeName;
        this.targetName = targetName;
        this.propertyTypeName = propertyTypeName;
        this.methodSuffix = methodSuffix;
        this.isSetter = isSetter;
        this.isNullable = isNullable;
        this.isReadOnly = isReadOnly;
        this.isWriteOnly = isWriteOnly;
    }

    public String attributeName() {
        return attributeName;
    }

    public String methodSuffix() {
        return methodSuffix;
    }

    public boolean isSetter() {
        return isSetter;
    }

    public TypeName targetName() {
        return targetName;
    }

    public TypeName propertyTypeName() {
        return propertyTypeName;
    }

    public boolean isNullable() {
        return isNullable;
    }

    public boolean isReadOnly() {
        return isReadOnly;
    }

    public boolean isWriteOnly() {
        return isWriteOnly;
    }

    static class Builder {

        private String attributeName;
        private String methodSuffix;
        private TypeName targetName;
        private TypeName propertyTypeName;
        private boolean isSetter;
        private boolean isNullable;
        private boolean isReadOnly;
        private boolean isWriteOnly;

        Builder(String attributeName,
                TypeName targetName,
                TypeName propertyTypeName) {
            this.methodSuffix = "";
            this.attributeName = attributeName;
            this.targetName = targetName;
            this.propertyTypeName = propertyTypeName;
        }

        Builder setSetter(boolean setter) {
            this.isSetter = setter;
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
                attributeName,
                targetName,
                propertyTypeName,
                methodSuffix,
                isSetter,
                isNullable,
                isReadOnly,
                isWriteOnly);
        }
    }
}
