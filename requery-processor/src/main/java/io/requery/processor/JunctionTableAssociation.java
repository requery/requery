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

import io.requery.Column;
import io.requery.ForeignKey;
import io.requery.JunctionTable;
import io.requery.ReferentialAction;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

class JunctionTableAssociation implements AssociativeEntityDescriptor {

    private final JunctionTable table;
    private final Set<AssociativeReference> columns;
    private final TypeMirror typeMirror;

    JunctionTableAssociation(Elements elements, AttributeMember member, JunctionTable table) {
        this.table = table;
        this.columns = new LinkedHashSet<>();

        Optional<? extends AnnotationValue> columnValues =
                Mirrors.findAnnotationMirror(member.element(), JunctionTable.class)
                        .flatMap(m -> Mirrors.findAnnotationValue(m, "columns"));

        for (Column column : table.columns()) {
            ReferentialAction deleteAction = ReferentialAction.CASCADE;
            ReferentialAction updateAction = ReferentialAction.CASCADE;
            TypeElement referenceType = null;
            String referencedColumn = null;

            if (column.foreignKey().length > 0) {
                ForeignKey key = column.foreignKey()[0];
                deleteAction = key.delete();
                updateAction = key.update();

                if (columnValues.isPresent()) {
                    List mirrors = (List) columnValues.get().getValue();
                    AnnotationMirror mirror = null;
                    for (Object m : mirrors) {
                        String name = Mirrors.findAnnotationValue((AnnotationMirror) m, "name")
                                .map(AnnotationValue::getValue)
                                .map(Object::toString)
                                .orElse(null);
                        if (column.name().equals(name)) {
                            mirror = (AnnotationMirror) m;
                            break;
                        }
                    }
                    if (mirror != null) {
                        Optional<? extends AnnotationValue> keyValue =
                                Mirrors.findAnnotationValue(mirror, "foreignKey");
                        if (keyValue.isPresent()) {
                            List children = (List) keyValue.get().getValue();
                            AnnotationMirror keyMirror = (AnnotationMirror) children.get(0);
                            referenceType =
                                Mirrors.findAnnotationValue(keyMirror, "references")
                                .map(value -> elements.getTypeElement( value.getValue().toString()))
                                .orElse(null);

                            referencedColumn =
                                Mirrors.findAnnotationValue(keyMirror, "referencedColumn")
                                .map(value -> value.getValue().toString())
                                .orElse(null);
                        }
                    }
                }
            }
            columns.add(new AssociativeReference(column.name(), referenceType,
                    referencedColumn, deleteAction, updateAction));
        }
        TypeMirror mirror = null;
        try {
            table.type();
        } catch (MirroredTypeException e) {
            if (!e.getTypeMirror().toString().equals("void")) {
                mirror = e.getTypeMirror(); // easiest way to get the mirror
            }
        }
        this.typeMirror = mirror;
    }

    @Override
    public String name() {
        return table.name();
    }

    @Override
    public Set<AssociativeReference> columns() {
        return columns;
    }

    @Override
    public Optional<TypeMirror> type() {
        return Optional.ofNullable(typeMirror);
    }
}
