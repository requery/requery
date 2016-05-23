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
import io.requery.query.element.GroupByElement;
import io.requery.query.element.HavingConditionElement;
import io.requery.sql.QueryBuilder;

import java.util.Set;

import static io.requery.sql.Keyword.BY;
import static io.requery.sql.Keyword.GROUP;
import static io.requery.sql.Keyword.HAVING;

class GroupByGenerator implements Generator<GroupByElement> {

    @Override
    public void write(final Output output, GroupByElement query) {
        QueryBuilder qb = output.builder();
        Set<Expression<?>> groupBy = query.groupByExpressions();
        if (groupBy != null && groupBy.size() > 0) {
            qb.keyword(GROUP, BY);
            qb.commaSeparated(groupBy, new QueryBuilder.Appender<Expression<?>>() {
                @Override
                public void append(QueryBuilder qb, Expression<?> value) {
                    output.appendColumn(value);
                }
            });
            if (query.havingElements() != null) {
                qb.keyword(HAVING);
                for (HavingConditionElement<?> clause : query.havingElements()) {
                    output.appendConditional(clause);
                }
            }
        }
    }
}
