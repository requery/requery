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

import io.requery.ReferentialAction;

import javax.persistence.ConstraintMode;
import javax.persistence.ForeignKey;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import java.util.LinkedHashSet;
import java.util.Set;

class JoinTableAssociation implements AssociativeEntityDescriptor {

    private final JoinTable table;
    private final Set<AssociativeReference> columns;

    JoinTableAssociation(JoinTable table) {
        this.table = table;
        this.columns = new LinkedHashSet<>();
        for (JoinColumn column : table.joinColumns()) {
            String columnName = column.name();
            ReferentialAction action = ReferentialAction.CASCADE;
            ForeignKey foreignKey = column.foreignKey();
            if (foreignKey != null) {
                action = mapConstraint(foreignKey.value());
            }
            columns.add(new AssociativeReference(columnName, action, null));
        }
        for (JoinColumn column : table.inverseJoinColumns()) {
            String columnName = column.name();
            ReferentialAction action = ReferentialAction.CASCADE;
            ForeignKey foreignKey = column.foreignKey();
            if (foreignKey != null) {
                action = mapConstraint(foreignKey.value());
            }
            columns.add(new AssociativeReference(columnName, action, null));
        }
    }

    private ReferentialAction mapConstraint(ConstraintMode constraint) {
        switch (constraint) {
            case NO_CONSTRAINT:
                return ReferentialAction.NO_ACTION;
            default:
            case CONSTRAINT:
            case PROVIDER_DEFAULT:
                return ReferentialAction.CASCADE;
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
