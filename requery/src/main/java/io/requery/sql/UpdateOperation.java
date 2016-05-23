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

import io.requery.query.Scalar;
import io.requery.query.SuppliedScalar;
import io.requery.query.element.QueryElement;
import io.requery.query.element.QueryOperation;
import io.requery.sql.gen.DefaultOutput;
import io.requery.util.function.Supplier;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Executes a single update operation.
 *
 * @author Nikhil Purushe
 */
class UpdateOperation extends PreparedQueryOperation implements QueryOperation<Scalar<Integer>> {

    UpdateOperation(RuntimeConfiguration configuration) {
        super(configuration, null);
    }

    UpdateOperation(RuntimeConfiguration configuration, GeneratedResultReader resultReader) {
        super(configuration, resultReader);
    }

    @Override
    public Scalar<Integer> execute(final QueryElement<Scalar<Integer>> query) {
        return new SuppliedScalar<>(new Supplier<Integer>() {
            @Override
            public Integer get() {
                DefaultOutput generator = new DefaultOutput(configuration, query);
                String sql = generator.toSql();
                BoundParameters parameters = generator.parameters();
                int result;
                try (Connection connection = configuration.connectionProvider().getConnection()) {
                    StatementListener listener = configuration.statementListener();
                    if (parameters.isEmpty()) {
                        try (Statement statement = connection.createStatement()) {
                            listener.beforeExecuteUpdate(statement, sql, null);
                            result = statement.executeUpdate(sql);
                            listener.afterExecuteUpdate(statement);
                            readGeneratedKeys(0, statement);
                        }
                    } else {
                        try (PreparedStatement statement = prepare(sql, connection)) {
                            mapParameters(statement, parameters);
                            listener.beforeExecuteUpdate(statement, sql, parameters);
                            result = statement.executeUpdate();
                            listener.afterExecuteUpdate(statement);
                            readGeneratedKeys(0, statement);
                        }
                    }
                } catch (SQLException e) {
                    throw new StatementExecutionException(e, sql);
                }
                return result;
            }
        }, configuration.writeExecutor());
    }
}
