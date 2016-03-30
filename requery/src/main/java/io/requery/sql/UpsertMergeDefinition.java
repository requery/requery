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

package io.requery.sql;

import io.requery.meta.Attribute;
import io.requery.meta.Type;

import java.util.LinkedHashSet;

import static io.requery.sql.Keyword.*;

/**
 * Performs an upsert (insert/update) using the SQL standard MERGE statement. This is the most
 * portable type of upsert.
 *
 * @author Nikhil Purushe
 */
public class UpsertMergeDefinition implements UpsertDefinition {

    protected final String alias = "val";

    @Override
    public <E> void appendUpsert(QueryBuilder qb,
                                 Iterable<Attribute<E, ?>> attributes,
                                 final Parameterizer<E> parameterizer) {
        Type<E> type = attributes.iterator().next().declaringType();
        qb.keyword(MERGE).keyword(INTO)
            .tableName(type.name()).keyword(USING);
            appendUsing(qb, attributes, parameterizer);
            qb.keyword(ON)
            .openParenthesis();
        int count = 0;
        for (Attribute<E, ?> attribute : type.keyAttributes()) {
            if (count > 0) {
                qb.append("&&");
            }
            qb.aliasAttribute(type.name(), attribute);
            qb.append(" = ");
            qb.aliasAttribute(alias, attribute);
            count++;
        }
        qb.closeParenthesis().space();
        // update fragment
        LinkedHashSet<Attribute<E, ?>> updates = new LinkedHashSet<>();
        for (Attribute<E, ?> attribute : attributes) {
            if (!attribute.isKey()) {
                updates.add(attribute);
            }
        }
        qb.keyword(WHEN, MATCHED, THEN, UPDATE, SET)
            .commaSeparated(updates, new QueryBuilder.Appender<Attribute<E, ?>>() {
                @Override
                public void append(QueryBuilder qb, Attribute<E, ?> value) {
                    qb.attribute(value);
                    qb.append(" = " + alias + "." + value.name());
                }
            }).space();
        // insert fragment
        qb.keyword(WHEN, NOT, MATCHED, THEN, INSERT)
            .openParenthesis()
            .commaSeparatedAttributes(attributes)
            .closeParenthesis().space()
            .keyword(VALUES)
            .openParenthesis()
            .commaSeparated(attributes, new QueryBuilder.Appender<Attribute<E, ?>>() {
                @Override
                public void append(QueryBuilder qb, Attribute<E, ?> value) {
                    qb.aliasAttribute(alias, value);
                }
            })
            .closeParenthesis();
    }

    protected <E> void appendUsing(QueryBuilder qb,
                                   Iterable<Attribute<E, ?>> attributes,
                                   final Parameterizer<E> parameterizer) {
        qb.openParenthesis()
            .keyword(VALUES).openParenthesis()
            .commaSeparated(attributes, new QueryBuilder.Appender<Attribute<E, ?>>() {
                @Override
                public void append(QueryBuilder qb, Attribute<E, ?> value) {
                    qb.append("?");
                    parameterizer.addParameter(value);
                }
            }).closeParenthesis()
            .closeParenthesis().space()
            .keyword(AS)
            .append(alias)
            .openParenthesis()
            .commaSeparatedAttributes(attributes)
            .closeParenthesis().space();
    }
}
