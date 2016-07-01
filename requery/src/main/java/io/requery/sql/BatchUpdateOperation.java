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
import io.requery.query.element.QueryElement;
import io.requery.query.element.QueryOperation;
import io.requery.sql.gen.DefaultOutput;

import java.sql.BatchUpdateException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Executes a batched update or insert operation using JDBC batching if enabled otherwise reuses
 * the same {@link PreparedStatement} for multiple inserts.
 *
 * @author Nikhil Purushe
 */
class BatchUpdateOperation<E> extends PreparedQueryOperation implements QueryOperation<int[]> {

    private final E[] elements;
    private final int length;
    private final ParameterBinder<E> parameterBinder;
    private final boolean batchInStatement;

    BatchUpdateOperation(RuntimeConfiguration configuration,
                         E[] elements,
                         int length,
                         ParameterBinder<E> parameterBinder,
                         GeneratedResultReader generatedResultReader,
                         boolean batchInStatement) {
        super(configuration, generatedResultReader);
        this.elements = elements;
        this.length = length;
        this.parameterBinder = parameterBinder;
        this.batchInStatement = batchInStatement;
    }

    @Override
    public int[] evaluate(QueryElement<int[]> query) {
        int[] result = batchInStatement ? null : new int[length];

        try (Connection connection = configuration.getConnection()) {
            DefaultOutput generator = new DefaultOutput(configuration, query);
            String sql = generator.toSql();
            StatementListener listener = configuration.getStatementListener();

            try (PreparedStatement statement = prepare(sql, connection)) {
                for (int i = 0; i < length; i++) {
                    E element = elements[i];
                    parameterBinder.bindParameters(statement, element, null);
                    if (batchInStatement) {
                        statement.addBatch();
                    } else {
                        listener.beforeExecuteUpdate(statement, sql, null);
                        result[i] = statement.executeUpdate();
                        listener.afterExecuteUpdate(statement);
                        readGeneratedKeys(i, statement);
                    }
                }
                if (batchInStatement) {
                    listener.beforeExecuteUpdate(statement, sql, null);
                    result = statement.executeBatch();
                    listener.afterExecuteUpdate(statement);
                    readGeneratedKeys(0, statement);
                }
            }
        } catch (BatchUpdateException e) {
            result = e.getUpdateCounts();
            if (result == null) {
                throw new PersistenceException(e);
            }
        } catch (SQLException e) {
            throw new PersistenceException(e);
        }
        return result;
    }
}
