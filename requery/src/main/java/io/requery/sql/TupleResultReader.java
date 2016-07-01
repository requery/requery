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

import io.requery.query.MutableTuple;
import io.requery.query.Tuple;
import io.requery.query.Expression;
import io.requery.util.Objects;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Set;

class TupleResultReader implements ResultReader<Tuple> {

    private final RuntimeConfiguration configuration;

    TupleResultReader(RuntimeConfiguration configuration) {
        this.configuration = Objects.requireNotNull(configuration);
    }

    @Override
    public Tuple read(ResultSet results, Set<? extends Expression<?>> selection)
        throws SQLException {
        MutableTuple tuple = new MutableTuple(selection.size());
        int index = 1;
        Mapping mapping = configuration.getMapping();
        for (Expression<?> expression : selection) {
            Object value = mapping.read(expression, results, index);
            tuple.set(index - 1, expression, value);
            index++;
        }
        return tuple;
    }
}
