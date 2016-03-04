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
import io.requery.query.BaseResult;
import io.requery.query.Expression;
import io.requery.query.NamedExpression;
import io.requery.query.Result;
import io.requery.query.Tuple;
import io.requery.query.element.QueryElement;
import io.requery.query.element.QueryOperation;
import io.requery.util.CloseableIterator;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Iterator;
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
                    keys[index] = expression.name();
                    index++;
                }
                return keys;
            }
        });
        this.selection = selection;
    }

    @Override
    public Result<Tuple> execute(final QueryElement<Result<Tuple>> query) {
        QueryGenerator generator = new QueryGenerator<>(query);
        QueryBuilder qb = new QueryBuilder(configuration.queryBuilderOptions());
        String sql = generator.toSql(qb, configuration.platform());
        BoundParameters parameters = generator.parameters();
        int count;
        try {
            Connection connection = configuration.connectionProvider().getConnection();
            StatementListener listener = configuration.statementListener();
            PreparedStatement statement = prepare(sql, connection);
            mapParameters(statement, parameters);
            listener.beforeExecuteUpdate(statement, sql, parameters);
            count = statement.executeUpdate();
            listener.afterExecuteUpdate(statement);
            if (selection == null || selection.isEmpty()) {
                connection.close();
                ResultTuple tuple = new ResultTuple(1);
                tuple.set(0, NamedExpression.ofInteger("count"), count);
                final Iterator<Tuple> iterator = Collections.<Tuple>singleton(tuple).iterator();
                return new BaseResult<Tuple>(1) {
                    @Override
                    public CloseableIterator<Tuple> iterator(int skip, int take) {
                        return new CloseableIterator<Tuple>() {
                            @Override
                            public void close() {
                            }

                            @Override
                            public boolean hasNext() {
                                return iterator.hasNext();
                            }

                            @Override
                            public Tuple next() {
                                return iterator.next();
                            }
                        };
                    }
                };
            } else {
                ResultSet results = statement.getGeneratedKeys();
                return new GeneratedKeyResult(configuration, selection, connection, results, count);
            }
        } catch (SQLException e) {
            throw new PersistenceException(e);
        }
    }
}
