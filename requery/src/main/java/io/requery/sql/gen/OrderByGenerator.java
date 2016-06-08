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
import io.requery.query.ExpressionType;
import io.requery.query.Order;
import io.requery.query.OrderingExpression;
import io.requery.query.element.OrderByElement;
import io.requery.sql.QueryBuilder;

import java.util.Set;

import static io.requery.sql.Keyword.ASC;
import static io.requery.sql.Keyword.BY;
import static io.requery.sql.Keyword.DESC;
import static io.requery.sql.Keyword.FIRST;
import static io.requery.sql.Keyword.LAST;
import static io.requery.sql.Keyword.NULLS;
import static io.requery.sql.Keyword.ORDER;

public class OrderByGenerator implements Generator<OrderByElement> {

    @Override
    public void write(Output output, OrderByElement query) {
        Set<Expression<?>> orderBy = query.getOrderByExpressions();
        if (orderBy != null && orderBy.size() > 0) {
            QueryBuilder qb = output.builder();
            qb.keyword(ORDER, BY);
            int i = 0;
            int size = orderBy.size();
            for (Expression<?> order : orderBy) {
                output.appendColumn(order);
                if (order.getExpressionType() == ExpressionType.ORDERING) {
                    OrderingExpression ordering = (OrderingExpression) order;
                    qb.keyword(ordering.getOrder() == Order.ASC ? ASC : DESC);
                    if(ordering.getNullOrder() != null) {
                        qb.keyword(NULLS);
                        switch (ordering.getNullOrder()) {
                            case FIRST:
                                qb.keyword(FIRST);
                                break;
                            case LAST:
                                qb.keyword(LAST);
                                break;
                        }
                    }
                }
                if (i < size - 1) {
                    qb.append(",");
                }
                i++;
            }
        }
    }
}
