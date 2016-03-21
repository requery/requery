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
import javax.lang.model.util.Elements;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

class JunctionTableAssociation implements AssociativeEntityDescriptor {

    private final JunctionTable table;
    private final Set<AssociativeReference> columns;

    JunctionTableAssociation(Elements elements, AttributeMember member, JunctionTable table) {
        this.table = table;
        this.columns = new LinkedHashSet<>();
        for (Column column : table.columns()) {
            ForeignKey key = column.foreignKey()[0];
            String columnName = column.name();
            ReferentialAction deleteAction = ReferentialAction.CASCADE;
            ReferentialAction updateAction = ReferentialAction.CASCADE;
            TypeElement referenceType = null;

            if (key != null) {
                deleteAction = key.delete();
                updateAction = key.update();
                Optional<? extends AnnotationValue> value =
                    Mirrors.findAnnotationMirror(member.element(), JunctionTable.class)
                        .flatMap(m -> Mirrors.findAnnotationValue(m, "columns"));

                if (value.isPresent()) {
                    List mirrors = (List) value.get().getValue();
                    for (Object m : mirrors) {
                        Optional<? extends AnnotationValue> keyValue =
                            Mirrors.findAnnotationValue((AnnotationMirror) m, "foreignKey");
                        if (keyValue.isPresent()) {
                            List children = (List) keyValue.get().getValue();
                            Optional<? extends AnnotationValue> annotationValue =
                                Mirrors.findAnnotationValue(
                                    (AnnotationMirror) children.get(0), "references");
                            if (annotationValue.isPresent()) {
                                referenceType = elements.getTypeElement(
                                    annotationValue.get().getValue().toString());
                            }
                        }
                    }
                }
            }
            columns.add(
                new AssociativeReference(columnName, referenceType, deleteAction, updateAction));
        }
    }

    @Override
    public String name() {
        return table.name();
    }

    @Override
    public Set<AssociativeReference> columns() {
        return columns;
    }
}
