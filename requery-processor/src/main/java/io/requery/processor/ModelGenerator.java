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

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import io.requery.Entity;
import io.requery.meta.EntityModel;
import io.requery.meta.EntityModelBuilder;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Modifier;
import java.io.IOException;
import java.util.*;

/**
 * Generates a java class file containing one or more {@link EntityModel} information for classes
 * that were processed by the annotation processor. The models can be used to group together
 * persistable classes that may belong to different databases through the {@link Entity#model()}
 * field.
 * @author Nikhil Purushe
 */
class ModelGenerator implements SourceGenerator {

    private final String packageName;
    private final ProcessingEnvironment processingEnvironment;
    private final Collection<? extends EntityDescriptor> entities;

    ModelGenerator(ProcessingEnvironment processingEnvironment,
                   String packageName,
                   Collection<? extends EntityDescriptor> entities) {
        this.processingEnvironment = processingEnvironment;
        this.packageName = packageName;
        this.entities = entities;
    }

    @Override
    public void generate() throws IOException {
        ClassName typeName = ClassName.get(packageName, "Models");
        TypeSpec.Builder type = TypeSpec.classBuilder(typeName)
                .addModifiers(Modifier.PUBLIC)
                .addMethod(MethodSpec.constructorBuilder()
                        .addModifiers(Modifier.PRIVATE).build());
        CodeGeneration.addGeneratedAnnotation(processingEnvironment, type);

        Map<String, Set<EntityDescriptor>> models = new HashMap<>();
        entities.forEach(entity ->
                models.computeIfAbsent(entity.modelName(), key -> new HashSet<>()).add(entity));

        for (String model : models.keySet()) {
            List<EntityDescriptor> types = new ArrayList<>(models.get(model));
            // Sort types by name to avoid arbitrary generation order, breaking incremental compilation
            types.sort(Comparator.comparing(it -> it.typeName().toString()));

            FieldSpec.Builder field = FieldSpec.builder(ClassName.get(EntityModel.class),
                    model.toUpperCase(), Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL);
            CodeBlock.Builder fieldType = CodeBlock.builder();
            fieldType.add("new $T($S)\n", ClassName.get(EntityModelBuilder.class), model);

            types.forEach(e -> fieldType.add(".addType($T.$L)\n",
                    ClassName.bestGuess(e.typeName().toString()), EntityGenerator.TYPE_NAME));

            fieldType.add(".build()");
            field.initializer("$L", fieldType.build());
            type.addField(field.build());
        }

        CodeGeneration.writeType(processingEnvironment, typeName.packageName(), type.build());
    }
}
