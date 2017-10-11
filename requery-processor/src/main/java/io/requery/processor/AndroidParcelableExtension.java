/*
 * Copyright 2017 requery.io
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

import com.squareup.javapoet.ArrayTypeName;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Types;

/**
 * Handler for Android's Parcelable objects. If the target entity object implements Parcelable but
 * doesn't provide it's own implementation than a implementation will be provided in the generated
 * entity using {@link io.requery.android.EntityParceler}
 *
 * @author Nikhil Purushe
 */
@SuppressWarnings("JavadocReference")
class AndroidParcelableExtension implements TypeGenerationExtension {

    private static final String PACKAGE_ANDROID_OS = "android.os";
    private static final String PACKAGE_PARCELER = "io.requery.android";

    private final Types types;

    AndroidParcelableExtension(Types types) {
        this.types = types;
    }

    @Override
    public void generate(EntityDescriptor entity, TypeSpec.Builder builder) {
        // if not parcelable or the class implements itself don't implement it
        TypeElement typeElement = entity.element();
        if (entity.isImmutable() ||
            !Mirrors.isInstance(types, typeElement, PACKAGE_ANDROID_OS + ".Parcelable") ||
            Mirrors.overridesMethod(types, typeElement, "writeToParcel")) {
            return;
        }
        ClassName className = ClassName.bestGuess(entity.typeName().toString());
        // implement the parcelable interface
        TypeName creatorType = ParameterizedTypeName.get(
                ClassName.get(PACKAGE_ANDROID_OS, "Parcelable.Creator"), className);
        ClassName parcelName = ClassName.get(PACKAGE_ANDROID_OS, "Parcel");
        ClassName parcelableName = ClassName.get(PACKAGE_ANDROID_OS, "Parcelable");
        builder.addSuperinterface(parcelableName);

        TypeSpec.Builder creatorBuilder = TypeSpec.anonymousClassBuilder("")
            .addSuperinterface(creatorType)
            .addMethod(CodeGeneration.overridePublicMethod("createFromParcel")
                .addParameter(ParameterSpec.builder(parcelName, "source").build())
                .addStatement("return PARCELER.readFromParcel(source)")
                .returns(className).build())
            .addMethod(CodeGeneration.overridePublicMethod("newArray")
                .addParameter(TypeName.INT, "size")
                .addStatement("return new $T[size]", className)
                .returns(ArrayTypeName.of(className)).build());

        builder.addField(
            FieldSpec.builder(creatorType, "CREATOR",
                Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                .initializer("$L", creatorBuilder.build()).build());

        builder.addMethod(
            CodeGeneration.overridePublicMethod("describeContents")
                .returns(TypeName.INT)
                .addStatement("return 0").build());

        builder.addMethod(
            CodeGeneration.overridePublicMethod("writeToParcel")
                .returns(TypeName.VOID)
                .addParameter(parcelName, "dest")
                .addParameter(TypeName.INT, "flags")
                .addStatement("PARCELER.writeToParcel(this, dest)").build());

        // add the parceler instance which uses the proxy to parcel the field data
        TypeName parcelerType = ParameterizedTypeName.get(
                ClassName.get(PACKAGE_PARCELER, "EntityParceler"), className);
        builder.addField(
                FieldSpec.builder(parcelerType, "PARCELER", Modifier.STATIC, Modifier.FINAL)
                    .initializer("new $T($L)", parcelerType,
                        EntityGenerator.TYPE_NAME).build());
    }
}
