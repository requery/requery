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
import io.requery.query.element.QueryElement;
import io.requery.query.element.QueryWrapper;
import io.requery.util.CloseableIterable;
import io.requery.util.CloseableIterator;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Set;

/**
 * Represents the result of a select query.
 *
 * @author Nikhil Purushe
 */
class SelectResult<E> extends BaseResult<E> implements CloseableIterable<E>, QueryWrapper {

    private final QueryElement<?> query;
    private final RuntimeConfiguration configuration;
    private final ResultReader<E> reader;
    private final Set<? extends Expression<?>> selection;
    private final Integer limit;
    private final int resultSetType;
    private final int resultSetConcurrency;
    private final boolean keepStatement;
    private QueryGenerator generator;
    private String sql;
    private Statement statement;
    private Connection connection;

    SelectResult(RuntimeConfiguration configuration,
                 QueryElement<?> query, ResultReader<E> reader) {
        super(query.getLimit());
        this.query = query;
        this.configuration = configuration;
        this.reader = reader;
        selection = query.selection();
        limit = query.getLimit();
        keepStatement = false;
        resultSetType = ResultSet.TYPE_FORWARD_ONLY;
        resultSetConcurrency = ResultSet.CONCUR_READ_ONLY;
    }

    private Statement createStatement(boolean prepared) throws SQLException {
        if (keepStatement && statement != null) {
            return statement;
        }
        Connection connection = configuration.connectionProvider().getConnection();
        Statement statement;
        if (!prepared) {
            statement = connection.createStatement(resultSetType, resultSetConcurrency);
        } else {
            statement = connection.prepareStatement(sql, resultSetType, resultSetConcurrency);
        }
        if (keepStatement) {
            this.statement = statement;
            this.connection = connection;
        }
        return statement;
    }

    private void createQuery(int skip, int take) {
        // query can't already have been limited and skip/take must be non-defaults
        if (limit == null && take > 0 && take != Integer.MAX_VALUE) {
            query.limit(take).offset(skip);
        }
        generator = new QueryGenerator<>(query);
        QueryBuilder qb = new QueryBuilder(configuration.queryBuilderOptions());
        sql = generator.toSql(qb, configuration.platform());
    }

    @Override
    public CloseableIterator<E> iterator(int skip, int take) {
        try {
            createQuery(skip, take);
            // connection held by the iterator if statement not reused
            BoundParameters parameters = generator.parameters();
            Statement statement = createStatement(!parameters.isEmpty());
            statement.setFetchSize(limit == null ? 0 : limit);

            StatementListener listener = configuration.statementListener();
            listener.beforeExecuteQuery(statement, sql, parameters);

            ResultSet results;
            if (parameters.isEmpty()) {
                results = statement.executeQuery(sql);
            } else {
                PreparedStatement preparedStatement = (PreparedStatement) statement;
                Mapping mapping = configuration.mapping();
                for (int i = 0; i < parameters.count(); i++) {
                    Expression expression = parameters.expressionAt(i);
                    mapping.write(expression, preparedStatement, i + 1, parameters.valueAt(i));
                }
                results = preparedStatement.executeQuery();
            }
            listener.afterExecuteQuery(statement);

            return new ResultSetIterator<>(reader, results, selection, !keepStatement);
        } catch (SQLException e) {
            throw new PersistenceException(e);
        }
    }

    @Override
    public void close() {
        super.close();
        if (keepStatement) {
            if (statement != null) {
                try {
                    statement.close();
                } catch (SQLException ignored) {
                }
            }
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException ignored) {
                }
            }
        }
    }

    @Override
    public QueryElement unwrapQuery() {
        return query;
    }
}
