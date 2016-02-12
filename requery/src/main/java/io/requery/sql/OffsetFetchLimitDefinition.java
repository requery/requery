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

import static io.requery.sql.Keyword.FETCH;
import static io.requery.sql.Keyword.NEXT;
import static io.requery.sql.Keyword.OFFSET;
import static io.requery.sql.Keyword.ONLY;
import static io.requery.sql.Keyword.ROW;
import static io.requery.sql.Keyword.ROWS;

public class OffsetFetchLimitDefinition implements LimitDefinition {

    @Override
    public boolean requireOrderBy() {
        return false;
    }

    @Override
    public void appendLimit(QueryBuilder qb, Integer limit, Integer offset) {
        if (offset != null) {
            qb.keyword(OFFSET)
                    .value(offset)
                    .keyword(offset > 1 ? ROWS : ROW)
                    .keyword(FETCH, NEXT)
                    .value(limit)
                    .keyword(limit > 1 ? ROWS : ROW)
                    .keyword(ONLY);
        } else {
            qb.keyword(FETCH, Keyword.FIRST)
                    .value(limit)
                    .keyword(limit > 1 ? ROWS : ROW)
                    .keyword(ONLY);
        }
    }
}
