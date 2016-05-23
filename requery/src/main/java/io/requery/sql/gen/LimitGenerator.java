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

import io.requery.query.element.LimitedElement;
import io.requery.sql.QueryBuilder;

import static io.requery.sql.Keyword.LIMIT;
import static io.requery.sql.Keyword.OFFSET;

public class LimitGenerator implements Generator<LimitedElement> {

    @Override
    public void write(Output output, LimitedElement query) {
        QueryBuilder qb = output.builder();
        Integer limit = query.getLimit();
        if (limit != null && limit > 0) {
            Integer offset = query.getOffset();
            qb.keyword(LIMIT).value(limit);
            if (offset != null) {
                qb.keyword(OFFSET).value(offset);
            }
        }
    }
}
