/*
 * Copyright 2018 requery.io
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
import io.requery.query.element.LimitedElement;
import io.requery.query.element.OrderByElement;
import io.requery.query.element.QueryElement;
import io.requery.query.element.SelectionElement;
import io.requery.query.element.SetOperationElement;
import io.requery.query.element.WhereElement;
import io.requery.sql.Platform;
import io.requery.sql.QueryBuilder;

import java.util.Map;

import static io.requery.sql.Keyword.DELETE;
import static io.requery.sql.Keyword.FROM;
import static io.requery.sql.Keyword.TRUNCATE;

public final class StatementGenerator implements Generator<QueryElement<?>> {

    private Generator<SelectionElement> select;
    private Generator<QueryElement<?>> insert;
    private Generator<Map<Expression<?>, Object>> update;
    private Generator<Map<Expression<?>, Object>> upsert;
    private Generator<WhereElement> where;
    private Generator<GroupByElement> groupBy;
    private Generator<OrderByElement> orderBy;
    private Generator<LimitedElement> limit;
    private Generator<SetOperationElement> setOperation;

    public StatementGenerator(Platform platform) {
        // TODO eventually all parts will be overridable by the platform
        select = new SelectGenerator();
        insert = platform.insertGenerator();
        update = platform.updateGenerator();
        upsert = platform.upsertGenerator();
        where = new WhereGenerator();
        groupBy = new GroupByGenerator();
        orderBy = platform.orderByGenerator();
        limit = platform.limitGenerator();
        setOperation = new SetOperatorGenerator();
    }

    @Override
    public void write(Output output, QueryElement<?> query) {
        QueryBuilder qb = output.builder();
        switch (query.queryType()) {
            case SELECT:
                select.write(output, query);
                break;
            case INSERT:
                insert.write(output, query);
                break;
            case UPDATE:
                update.write(output, checkEmpty(query.updateValues()));
                break;
            case UPSERT:
                upsert.write(output, checkEmpty(query.updateValues()));
                break;
            case DELETE:
                qb.keyword(DELETE, FROM);
                output.appendTables();
                break;
            case TRUNCATE:
                qb.keyword(TRUNCATE);
                output.appendTables();
                break;
        }
        where.write(output, query);
        groupBy.write(output, query);
        orderBy.write(output, query);
        limit.write(output, query);
        setOperation.write(output, query);
    }

    private static Map<Expression<?>, Object> checkEmpty(Map<Expression<?>, Object> values) {
        if (values == null || values.isEmpty()) {
            throw new IllegalStateException(
                "Cannot generate update statement with an empty set of values");
        }
        return values;
    }
}
