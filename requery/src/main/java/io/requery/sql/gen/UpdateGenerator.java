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
import io.requery.sql.QueryBuilder;

import java.util.Map;

import static io.requery.sql.Keyword.SET;
import static io.requery.sql.Keyword.UPDATE;

public class UpdateGenerator implements Generator<Map<Expression<?>, Object>> {

    @Override
    public void write(final Output output, Map<Expression<?>, Object> values) {
        QueryBuilder qb = output.builder();
        qb.keyword(UPDATE);
        output.appendTables();
        qb.keyword(SET);
        int index = 0;
        for (Map.Entry<Expression<?>, Object> entry : values.entrySet()) {
            if (index > 0) {
                qb.append(",");
            }
            output.appendColumn(entry.getKey());
            output.appendOperator(Operator.EQUAL);
            output.appendConditionValue(entry.getKey(), entry.getValue());
            index++;
        }
    }
}
