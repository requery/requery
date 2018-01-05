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

package io.requery.sql;

import io.requery.query.BaseResult;
import io.requery.query.Expression;
import io.requery.query.NamedExpression;
import io.requery.query.Result;
import io.requery.query.MutableTuple;
import io.requery.query.Tuple;
import io.requery.query.element.QueryType;
import io.requery.util.CloseableIterator;
import io.requery.util.function.Supplier;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Locale;
import java.util.Set;

/**
 * Executes a raw string query using a prepared statement with optional parameters returning a
 * {@link Result} of {@link Tuple} elements.
 *
 * @author Nikhil Purushe
 */
class RawTupleQuery extends PreparedQueryOperation implements Supplier<Result<Tuple>> {

    private final BoundParameters boundParameters;
    private final String sql;
    private final QueryType queryType;

    RawTupleQuery(RuntimeConfiguration configuration, String sql, Object[] parameters) {
        super(configuration, null);
        ParameterInliner inlined = new ParameterInliner(sql, parameters).apply();
        this.sql = inlined.sql();
        queryType = queryTypeOf(sql);
        boundParameters = new BoundParameters(inlined.parameters());
    }

    private static QueryType queryTypeOf(String sql) {
        int end = sql.indexOf(" ");
        if (end < 0) {
            throw new IllegalArgumentException("Invalid query " + sql);
        }
        String keyword = sql.substring(0, end).trim().toUpperCase(Locale.ROOT);
        try {
            return QueryType.valueOf(keyword);
        } catch (IllegalArgumentException e) {
            return QueryType.SELECT;
        }
    }

    @Override
    public Result<Tuple> get() {
        PreparedStatement statement = null;
        try {
            Connection connection = configuration.getConnection();
            statement = prepare(sql, connection);
            mapParameters(statement, boundParameters);
            switch (queryType) {
                case SELECT:
                default:
                    return new TupleResult(statement);
                case INSERT:
                case UPDATE:
                case UPSERT:
                case DELETE:
                case TRUNCATE:
                case MERGE:
                    // DML, only the row count is returned
                    StatementListener listener = configuration.getStatementListener();
                    listener.beforeExecuteUpdate(statement, sql, boundParameters);
                    int count = statement.executeUpdate();
                    listener.afterExecuteUpdate(statement, count);
                    MutableTuple tuple = new MutableTuple(1);
                    tuple.set(0, NamedExpression.ofInteger("count"), count);
                    try {
                        statement.close();
                    } finally {
                        try {
                            connection.close();
                        } catch (Exception ignored) {
                        }
                    }
                    return new CollectionResult<Tuple>(tuple);
            }
        } catch (Exception e) {
            throw StatementExecutionException.closing(statement, e, sql);
        }
    }

    private class TupleResult extends BaseResult<Tuple> implements ResultReader<Tuple> {

        private final PreparedStatement statement;
        private Expression[] expressions;

        private TupleResult(PreparedStatement statement) {
            this.statement = statement;
        }

        @Override
        public Tuple read(ResultSet results, Set<? extends Expression<?>> selection)
            throws SQLException {
            Mapping mapping = configuration.getMapping();
            MutableTuple tuple = new MutableTuple(expressions.length);
            for (int i = 0; i < tuple.count(); i++) {
                Object value = mapping.read(expressions[i], results, i + 1);
                tuple.set(i, expressions[i], value);
            }
            return tuple;
        }

        @Override
        public CloseableIterator<Tuple> createIterator(int skip, int take) {
            try {
                // execute the query
                StatementListener listener = configuration.getStatementListener();
                listener.beforeExecuteQuery(statement, sql, boundParameters);
                ResultSet results = statement.executeQuery();
                listener.afterExecuteQuery(statement);
                // read the result meta data
                ResultSetMetaData metadata = results.getMetaData();
                int columns = metadata.getColumnCount();
                expressions = new Expression[columns];
                Mapping mapping = configuration.getMapping();

                CloseableIterator<Tuple> iterator =
                    new ResultSetIterator<>(this, results, null, true, true);
                if (iterator.hasNext()) { // need to be positioned at some row (for android)
                    for (int i = 0; i < columns; i++) {
                        String name = metadata.getColumnName(i + 1);
                        int sqlType = metadata.getColumnType(i + 1);
                        if (sqlType == Types.NUMERIC) {
                            sqlType = Types.INTEGER;
                        }
                        Set<Class<?>> types = mapping.typesOf(sqlType);
                        expressions[i] = NamedExpression.of(name, types.iterator().next());
                    }
                }
                return iterator;
            } catch (SQLException e) {
                throw StatementExecutionException.closing(statement, e, sql);
            }
        }

        @Override
        public void close() {
            try {
                if (statement != null) {
                    Connection connection = statement.getConnection();
                    if (connection != null) {
                        connection.close();
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            } finally {
                super.close();
            }
        }
    }
}
