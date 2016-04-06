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

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.TypeSpec;
import io.requery.Entity;
import io.requery.ForeignKey;
import io.requery.Key;
import io.requery.ReferentialAction;
import io.requery.Table;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import java.io.IOException;
import java.io.Serializable;
import java.util.Set;

/**
 * Generates a joining entity between to {@link EntityDescriptor} instances to form a Many-to-Many
 * relationship. This entity will then be processed in another round by the annotation processor.
 *
 * @author Nikhil Purushe
 */
class JoinEntityGenerator implements SourceGenerator {

    private final ProcessingEnvironment processingEnvironment;
    private final EntityNameResolver nameResolver;
    private final EntityDescriptor from;
    private final EntityDescriptor to;
    private final AttributeDescriptor attribute;

    JoinEntityGenerator(ProcessingEnvironment processingEnvironment,
                        EntityNameResolver nameResolver,
                        EntityDescriptor from,
                        EntityDescriptor to,
                        AttributeDescriptor attribute) {
        this.processingEnvironment = processingEnvironment;
        this.nameResolver = nameResolver;
        this.from = from;
        this.to = to;
        this.attribute = attribute;
    }

    @Override
    public void generate() throws IOException {
        AssociativeEntityDescriptor associativeDescriptor = attribute.associativeEntity()
            .orElseThrow(IllegalStateException::new);

        String name = associativeDescriptor.name();
        if (Names.isEmpty(name)) {
            // create junction table name with TableA_TableB
            name = from.tableName() + "_" + to.tableName();
        }
        ClassName joinEntityName = nameResolver.joinEntityName(associativeDescriptor, from, to);
        String className = "Abstract" + joinEntityName.simpleName();
        TypeSpec.Builder junctionType = TypeSpec.classBuilder(className)
            .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
            .addSuperinterface(Serializable.class)
            .addAnnotation(AnnotationSpec.builder(Entity.class)
                .addMember("model", "$S", from.modelName()).build())
            .addAnnotation(AnnotationSpec.builder(Table.class)
                .addMember("name", "$S", name).build());
        CodeGeneration.addGeneratedAnnotation(processingEnvironment, junctionType);

        Set<AssociativeReference> references = associativeDescriptor.columns();
        EntityDescriptor[] types = new EntityDescriptor[] { from, to };
        if (references.isEmpty()) {
            // generate with defaults
            for (EntityDescriptor type : types) {
                AssociativeReference reference = new AssociativeReference(
                    type.tableName() + "Id",
                    type.element(),
                    ReferentialAction.CASCADE,
                    ReferentialAction.CASCADE );
                references.add(reference);
            }
        }

        int typeIndex = 0;
        for (AssociativeReference reference : references) {
            AnnotationSpec.Builder key = AnnotationSpec.builder(ForeignKey.class)
                .addMember("delete", "$T.$L",
                    ClassName.get(ReferentialAction.class), reference.deleteAction().toString())
                .addMember("update", "$T.$L",
                    ClassName.get(ReferentialAction.class), reference.updateAction().toString());

            TypeElement referenceElement = reference.referencedType();
            if (referenceElement == null && typeIndex < types.length) {
                referenceElement = types[typeIndex++].element();
            }
            if (referenceElement != null) {
                key.addMember("references", "$L.class",
                    nameResolver.generatedTypeNameOf(referenceElement)
                        .orElseThrow(IllegalStateException::new));
            }
            AnnotationSpec.Builder id = AnnotationSpec.builder(Key.class);
            FieldSpec.Builder field = FieldSpec.builder(Integer.class, reference.name(),
                Modifier.PROTECTED)
                .addAnnotation(key.build())
                .addAnnotation(id.build());
            junctionType.addField(field.build());
        }
        CodeGeneration.writeType(processingEnvironment,
            joinEntityName.packageName(), junctionType.build());
    }
}
