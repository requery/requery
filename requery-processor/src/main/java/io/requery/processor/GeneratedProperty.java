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

import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

/**
 * Contains various properties describing how generate to a {@link io.requery.proxy.Property}.
 */
class GeneratedProperty {

    private final String readName;
    private final String writeName;
    private final TypeName targetName;
    private final TypeName propertyTypeName;
    private String methodSuffix;
    private boolean useMethod;
    private boolean isNullable;
    private boolean isReadOnly;
    private boolean isWriteOnly;

    GeneratedProperty(String propertyName, TypeName targetName, TypeName propertyTypeName) {
        this(propertyName, propertyName, targetName, propertyTypeName);
    }

    GeneratedProperty(String readName,
                      String writeName,
                      TypeName targetName,
                      TypeName propertyTypeName) {
        this.methodSuffix = "";
        this.readName = readName;
        this.writeName = writeName;
        this.targetName = targetName;
        this.propertyTypeName = propertyTypeName;
    }

    GeneratedProperty setUseMethod(boolean useMethod) {
        this.useMethod = useMethod;
        return this;
    }

    GeneratedProperty setSuffix(String suffix) {
        this.methodSuffix = suffix;
        return this;
    }

    GeneratedProperty setNullable(boolean nullable) {
        this.isNullable = nullable;
        return this;
    }

    GeneratedProperty setReadOnly(boolean readOnly) {
        this.isReadOnly = readOnly;
        return this;
    }

    GeneratedProperty setWriteOnly(boolean writeOnly) {
        this.isWriteOnly = writeOnly;
        return this;
    }

    void build(TypeSpec.Builder builder) {
        addPropertyMethods(builder);
    }

    private void addPropertyMethods(TypeSpec.Builder builder) {
        String suffix = methodSuffix;
        // get
        MethodSpec.Builder getMethod = CodeGeneration.overridePublicMethod("get" + suffix)
            .addParameter(targetName, "entity")
            .returns(propertyTypeName);
        if (isWriteOnly) {
            getMethod.addStatement("throw new UnsupportedOperationException()");
        } else {
            getMethod.addStatement(useMethod ?
                "return entity.$L()" : "return entity.$L", readName);
        }
        // set
        MethodSpec.Builder setMethod = CodeGeneration.overridePublicMethod("set" + suffix)
            .addParameter(targetName, "entity")
            .addParameter(propertyTypeName, "value");
        if (isReadOnly) {
            setMethod.addStatement("throw new UnsupportedOperationException()");
        } else {
            CodeBlock.Builder setterBlock = CodeBlock.builder();
            if (isNullable) {
                setterBlock.beginControlFlow("if(value != null)");
            }
            setterBlock.addStatement(useMethod ?
                "entity.$L(value)" : "entity.$L = value", writeName);
            if (isNullable) {
                setterBlock.endControlFlow();
            }
            setMethod.addCode(setterBlock.build());
        }
        builder.addMethod(getMethod.build());
        builder.addMethod(setMethod.build());
    }
}
