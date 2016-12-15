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

import io.requery.query.BaseResult;
import io.requery.query.Expression;
import io.requery.query.Tuple;
import io.requery.util.CloseableIterator;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Set;

class GeneratedKeyResult extends BaseResult<Tuple> {

    private final Connection connection;
    private final ResultReader<Tuple> reader;
    private final ResultSet results;
    private final Set<? extends Expression<?>> selection;

    GeneratedKeyResult(RuntimeConfiguration configuration,
                       Set<? extends Expression<?>> selection,
                       Connection connection,
                       ResultSet results,
                       Integer maxSize) {
        super(maxSize);
        this.results = results;
        this.connection = connection;
        this.selection = selection;
        this.reader = new TupleResultReader(configuration);
    }

    @Override
    public CloseableIterator<Tuple> iterator(int skip, int take) {
        return new ResultSetIterator<>(reader, results, selection, true, true);
    }

    @Override
    public void close() {
        super.close();
        try {
            connection.close();
        } catch (SQLException ignored) {
        }
    }
}
