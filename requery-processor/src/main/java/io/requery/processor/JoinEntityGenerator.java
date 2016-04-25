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
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import io.requery.Entity;
import io.requery.ForeignKey;
import io.requery.Key;
import io.requery.ReferentialAction;
import io.requery.Table;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;
import java.io.IOException;
import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
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
        AssociativeEntityDescriptor descriptor = attribute.associativeEntity()
            .orElseThrow(IllegalStateException::new);

        String name = descriptor.name();
        if (Names.isEmpty(name)) {
            // create junction table name with TableA_TableB
            name = from.tableName() + "_" + to.tableName();
        }
        ClassName entityName = nameResolver.joinEntityName(descriptor, from, to);
        String className = "Abstract" + entityName.simpleName();
        TypeSpec.Builder junctionType = TypeSpec.classBuilder(className)
            .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
            .addSuperinterface(Serializable.class)
            .addAnnotation(AnnotationSpec.builder(Entity.class)
                .addMember("model", "$S", from.modelName()).build())
            .addAnnotation(AnnotationSpec.builder(Table.class)
                .addMember("name", "$S", name).build());
        CodeGeneration.addGeneratedAnnotation(processingEnvironment, junctionType);

        Set<AssociativeReference> references = descriptor.columns();
        EntityDescriptor[] entities = new EntityDescriptor[] { from, to };
        Map<AssociativeReference, EntityDescriptor> map = new LinkedHashMap<>();
        if (references.isEmpty()) {
            // generate with defaults
            for (EntityDescriptor type : entities) {
                AssociativeReference reference = new AssociativeReference(
                    type.tableName() + "Id",
                    type.element(),
                    ReferentialAction.CASCADE,
                    ReferentialAction.CASCADE );
                references.add(reference);
                map.put(reference, type);
            }
        } else {
            for (AssociativeReference reference : references) {
                for (EntityDescriptor entity : entities) {
                    if (reference.referencedType() != null &&
                        reference.referencedType().equals(entity.element())) {
                        map.put(reference, entity);
                    }
                }
            }
        }

        int index = 0;
        for (AssociativeReference reference : references) {
            ClassName action = ClassName.get(ReferentialAction.class);
            AnnotationSpec.Builder key = AnnotationSpec.builder(ForeignKey.class)
                .addMember("delete", "$T.$L", action, reference.deleteAction().toString())
                .addMember("update", "$T.$L", action, reference.updateAction().toString());

            TypeElement referenceElement = reference.referencedType();
            EntityDescriptor entity = map.get(reference);
            if (referenceElement == null) {
                if (entity != null) {
                    referenceElement = entity.element();
                } else {
                    referenceElement = entities[index++].element();
                }
            }
            if (referenceElement != null) {
                key.addMember("references", "$L.class",
                    nameResolver.generatedTypeNameOf(referenceElement)
                        .orElseThrow(IllegalStateException::new));
            }
            AnnotationSpec.Builder id = AnnotationSpec.builder(Key.class);
            TypeName typeName = TypeName.get(Integer.class);
            if (entity != null) {
                Optional<? extends AttributeDescriptor> keyAttribute =
                    entity.attributes().values().stream()
                        .filter(AttributeDescriptor::isKey).findAny();

                if (keyAttribute.isPresent()) {
                    TypeMirror keyType = keyAttribute.get().typeMirror();
                    if (keyType.getKind().isPrimitive()) {
                        Types types = processingEnvironment.getTypeUtils();
                        keyType = types.boxedClass((PrimitiveType)keyType).asType();
                    }
                    typeName = TypeName.get(keyType);
                }
            }
            FieldSpec.Builder field = FieldSpec.builder(typeName, reference.name(),
                Modifier.PROTECTED)
                .addAnnotation(key.build())
                .addAnnotation(id.build());
            junctionType.addField(field.build());
        }
        String packageName = entityName.packageName();
        CodeGeneration.writeType(processingEnvironment, packageName, junctionType.build());
    }
}
