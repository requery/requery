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

import io.requery.meta.Type;
import io.requery.query.BaseScalar;
import io.requery.query.Scalar;
import io.requery.query.element.QueryElement;
import io.requery.query.element.QueryOperation;
import io.requery.sql.gen.DefaultOutput;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Set;

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
    public Scalar<Integer> evaluate(final QueryElement<Scalar<Integer>> query) {
        return new BaseScalar<Integer>(configuration.getWriteExecutor()) {
            @Override
            public Integer evaluate() {
                DefaultOutput output = new DefaultOutput(configuration, query);
                String sql = output.toSql();
                int result;
                TransactionProvider transactionProvider = configuration.getTransactionProvider();
                Set<Type<?>> types = query.entityTypes();
                try (TransactionScope scope = new TransactionScope(transactionProvider, types);
                     Connection connection = configuration.getConnection()) {
                    StatementListener listener = configuration.getStatementListener();
                    try (PreparedStatement statement = prepare(sql, connection)) {
                        BoundParameters parameters = output.parameters();
                        mapParameters(statement, parameters);
                        listener.beforeExecuteUpdate(statement, sql, parameters);
                        result = statement.executeUpdate();
                        listener.afterExecuteUpdate(statement);
                        readGeneratedKeys(0, statement);
                    }
                    scope.commit();
                } catch (SQLException e) {
                    throw new StatementExecutionException(e, sql);
                }
                return result;
            }
        };
    }
}
