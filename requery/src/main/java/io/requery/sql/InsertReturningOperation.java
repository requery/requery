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

import io.requery.PersistenceException;
import io.requery.query.Expression;
import io.requery.query.NamedExpression;
import io.requery.query.Result;
import io.requery.query.MutableTuple;
import io.requery.query.Tuple;
import io.requery.query.element.QueryElement;
import io.requery.query.element.QueryOperation;
import io.requery.sql.gen.DefaultOutput;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Set;

/**
 * Executes insert operation returning the set of generated keys as a result, if no generated keys
 * then a single value representing the affected row count.
 *
 * @author Nikhil Purushe
 */
class InsertReturningOperation extends PreparedQueryOperation implements
    QueryOperation<Result<Tuple>> {

    private final Set<? extends Expression<?>> selection;

    InsertReturningOperation(RuntimeConfiguration configuration,
                             final Set<? extends Expression<?>> selection) {
        super(configuration, new GeneratedResultReader() {
            @Override
            public void read(int index, ResultSet results) throws SQLException {
                // skipped, not used
            }

            @Override
            public String[] generatedColumns() {
                String[] keys = new String[selection.size()];
                int index = 0;
                for (Expression<?> expression : selection) {
                    keys[index] = expression.getName();
                    index++;
                }
                return keys;
            }
        });
        this.selection = selection;
    }

    @Override
    public Result<Tuple> evaluate(final QueryElement<Result<Tuple>> query) {
        DefaultOutput generator = new DefaultOutput(configuration, query);
        String sql = generator.toSql();
        BoundParameters parameters = generator.parameters();
        int count;
        try {
            Connection connection = configuration.getConnection();
            StatementListener listener = configuration.getStatementListener();
            PreparedStatement statement = prepare(sql, connection);
            mapParameters(statement, parameters);
            listener.beforeExecuteUpdate(statement, sql, parameters);
            count = statement.executeUpdate();
            listener.afterExecuteUpdate(statement);
            if (selection == null || selection.isEmpty()) {
                connection.close();
                MutableTuple tuple = new MutableTuple(1);
                tuple.set(0, NamedExpression.ofInteger("count"), count);
                return new SingleResult<Tuple>(tuple);
            } else {
                ResultSet results = statement.getGeneratedKeys();
                return new GeneratedKeyResult(configuration, selection, connection, results, count);
            }
        } catch (SQLException e) {
            throw new PersistenceException(e);
        }
    }
}
