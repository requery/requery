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

import io.requery.query.element.ExistsElement;
import io.requery.query.element.QueryWrapper;
import io.requery.query.element.WhereConditionElement;
import io.requery.query.element.WhereElement;
import io.requery.sql.QueryBuilder;
import io.requery.util.function.Supplier;

import static io.requery.sql.Keyword.EXISTS;
import static io.requery.sql.Keyword.NOT;
import static io.requery.sql.Keyword.WHERE;

class WhereGenerator implements Generator<WhereElement> {

    @Override
    public void write(Output output, WhereElement query) {
        QueryBuilder qb = output.builder();
        ExistsElement<?> whereExists = query.whereExistsElement();
        if (whereExists != null) {
            qb.keyword(WHERE);
            if (whereExists.isNotExists()) {
                qb.keyword(NOT);
            }
            qb.keyword(EXISTS);
            qb.openParenthesis();
            Supplier<?> wrapper = whereExists.getQuery();
            output.appendQuery((QueryWrapper) wrapper);
            qb.closeParenthesis().space();
        } else if (query.whereElements() != null && query.whereElements().size() > 0) {
            qb.keyword(WHERE);
            for (WhereConditionElement<?> w : query.whereElements()) {
                output.appendConditional(w);
            }
        }
    }
}
