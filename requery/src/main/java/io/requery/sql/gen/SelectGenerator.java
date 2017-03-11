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

package io.requery.sql.gen;

import io.requery.query.Expression;
import io.requery.query.element.SelectionElement;
import io.requery.sql.QueryBuilder;

import java.util.Set;

import static io.requery.sql.Keyword.DISTINCT;
import static io.requery.sql.Keyword.FROM;
import static io.requery.sql.Keyword.SELECT;

class SelectGenerator implements Generator<SelectionElement> {

    @Override
    public void write(final Output output, SelectionElement query) {
        QueryBuilder qb = output.builder();
        qb.keyword(SELECT);
        if (query.isDistinct()) {
            qb.keyword(DISTINCT);
        }
        Set<? extends Expression<?>> selection = query.getSelection();
        if (selection == null || selection.isEmpty()) {
            qb.append("*");
        } else {
            qb.commaSeparated(selection,
                new QueryBuilder.Appender<Expression<?>>() {
                    @Override
                    public void append(QueryBuilder qb, Expression<?> value) {
                        output.appendColumnForSelect(value);
                    }
                });
        }
        qb.keyword(FROM);
        output.appendTables();
    }
}
