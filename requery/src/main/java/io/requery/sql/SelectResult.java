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
import io.requery.TransactionListener;
import io.requery.meta.Attribute;
import io.requery.query.BaseResult;
import io.requery.query.Expression;
import io.requery.query.element.QueryElement;
import io.requery.TransactionListenable;
import io.requery.query.element.QueryWrapper;
import io.requery.sql.gen.DefaultOutput;
import io.requery.util.CloseableIterator;
import io.requery.util.function.Supplier;

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
class SelectResult<E> extends BaseResult<E> implements TransactionListenable, QueryWrapper {

    private final QueryElement<?> query;
    private final RuntimeConfiguration configuration;
    private final ResultReader<E> reader;
    private final Set<? extends Expression<?>> selection;
    private final Integer limit;
    private final int resultSetType;
    private final int resultSetConcurrency;
    private final boolean keepStatement;
    private String sql;
    private Statement statement;
    private Connection connection;
    private boolean closeConnection;

    SelectResult(RuntimeConfiguration configuration,
                 QueryElement<?> query, ResultReader<E> reader) {
        super(query.getLimit());
        this.query = query;
        this.configuration = configuration;
        this.reader = reader;
        selection = query.getSelection();
        limit = query.getLimit();
        closeConnection = true;
        keepStatement = false;
        resultSetType = ResultSet.TYPE_FORWARD_ONLY;
        resultSetConcurrency = ResultSet.CONCUR_READ_ONLY;
    }

    private Statement createStatement(boolean prepared) throws SQLException {
        if (keepStatement && statement != null) {
            return statement;
        }
        Connection connection = configuration.getConnection();
        closeConnection = !(connection instanceof UncloseableConnection);
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

    private BoundParameters createQuery(int skip, int take) {
        // query can't already have been limited and skip/take must be non-defaults
        if (limit == null && take > 0 && take != Integer.MAX_VALUE) {
            query.limit(take).offset(skip);
        }
        DefaultOutput generator = new DefaultOutput(configuration, query);
        sql = generator.toSql();
        return generator.parameters();
    }

    @Override
    public CloseableIterator<E> iterator(int skip, int take) {
        try {
            // connection held by the iterator if statement not reused
            BoundParameters parameters = createQuery(skip, take);
            Statement statement = createStatement(!parameters.isEmpty());
            statement.setFetchSize(limit == null ? 0 : limit);

            StatementListener listener = configuration.getStatementListener();
            listener.beforeExecuteQuery(statement, sql, parameters);

            ResultSet results;
            if (parameters.isEmpty()) {
                results = statement.executeQuery(sql);
            } else {
                PreparedStatement preparedStatement = (PreparedStatement) statement;
                Mapping mapping = configuration.getMapping();
                for (int i = 0; i < parameters.count(); i++) {
                    Expression expression = parameters.expressionAt(i);
                    Object value = parameters.valueAt(i);
                    if (expression instanceof Attribute) {
                        // extract foreign key reference
                        Attribute attribute = (Attribute) expression;
                        if (attribute.isAssociation() &&
                            (attribute.isForeignKey() || attribute.isKey())) {
                            // get the referenced value
                            if (value != null &&
                                ((Expression<?>)expression).getClassType()
                                    .isAssignableFrom(value.getClass())) {
                                value = Attributes.replaceKeyReference(value, attribute);
                            }
                        }
                    }
                    mapping.write(expression, preparedStatement, i + 1, value);
                }
                results = preparedStatement.executeQuery();
            }
            listener.afterExecuteQuery(statement);

            return new ResultSetIterator<>(
                reader, results, selection, !keepStatement, closeConnection);
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
    public void addTransactionListener(Supplier<TransactionListener> supplier) {
        if (supplier != null) {
            configuration.getTransactionListenerFactories().add(supplier);
        }
    }

    @Override
    public QueryElement unwrapQuery() {
        return query;
    }
}
