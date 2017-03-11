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
    private final TypeName entity;
    private final TypeName typeName;
    private String methodSuffix;
    private String accessSuffix;
    private boolean useMethod;
    private boolean isNullable;
    private boolean isReadOnly;
    private boolean isWriteOnly;

    GeneratedProperty(String propertyName, TypeName entity, TypeName typeName) {
        this(propertyName, propertyName, entity, typeName);
    }

    GeneratedProperty(String readName, String writeName, TypeName entity, TypeName typeName) {
        this.readName = readName;
        this.writeName = writeName;
        this.entity = entity;
        this.typeName = typeName;
        this.methodSuffix = "";
    }

    GeneratedProperty setUseMethod(boolean useMethod) {
        this.useMethod = useMethod;
        return this;
    }

    GeneratedProperty setMethodSuffix(String suffix) {
        this.methodSuffix = suffix;
        return this;
    }

    GeneratedProperty setAccessSuffix(String suffix) {
        this.accessSuffix = suffix;
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
        // get
        MethodSpec.Builder getMethod = CodeGeneration.overridePublicMethod("get" + methodSuffix)
            .addParameter(entity, "entity")
            .returns(typeName);
        final String accessName = "entity" + (accessSuffix == null ? "" : accessSuffix);
        if (isWriteOnly) {
            getMethod.addStatement("throw new UnsupportedOperationException()");
        } else {
            getMethod.addStatement(useMethod?
                "return $L.$L()" : "return $L.$L", accessName, readName);
        }
        // set
        MethodSpec.Builder setMethod = CodeGeneration.overridePublicMethod("set" + methodSuffix)
            .addParameter(entity, "entity")
            .addParameter(typeName, "value");
        if (isReadOnly) {
            setMethod.addStatement("throw new UnsupportedOperationException()");
        } else {
            CodeBlock.Builder block = CodeBlock.builder();
            if (isNullable) {
                block.beginControlFlow("if(value != null)");
            }
            block.addStatement(useMethod? "$L.$L(value)" : "$L.$L = value", accessName, writeName);
            if (isNullable) {
                block.endControlFlow();
            }
            setMethod.addCode(block.build());
        }
        builder.addMethod(getMethod.build());
        builder.addMethod(setMethod.build());
    }
}
