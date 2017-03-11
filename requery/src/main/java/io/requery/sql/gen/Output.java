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
import io.requery.query.Operator;
import io.requery.query.element.LogicalElement;
import io.requery.query.element.QueryWrapper;
import io.requery.sql.BoundParameters;
import io.requery.sql.QueryBuilder;

public interface Output {

    QueryBuilder builder();

    BoundParameters parameters();

    void appendColumn(Expression<?> expression);

    void appendColumnForSelect(Expression<?> expression);

    void appendTables();

    void appendConditionValue(Expression expression, Object value);

    void appendOperator(Operator operator);

    void appendConditional(LogicalElement element);

    void appendQuery(QueryWrapper<?> query);
}
