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
import io.requery.meta.Attribute;
import io.requery.query.Scalar;
import io.requery.query.SuppliedScalar;
import io.requery.query.element.QueryElement;
import io.requery.util.function.Predicate;
import io.requery.util.function.Supplier;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Extends {@link UpdateOperation} specifically for binding to an entity, skipping the query
 * parameters to avoid boxing overhead.
 *
 * @param <E> entity type
 */
class EntityUpdateOperation<E> extends UpdateOperation {

    private final E element;
    private final ParameterBinder<E> parameterBinder;
    private final Predicate<Attribute<E, ?>> filter;

    EntityUpdateOperation(RuntimeConfiguration configuration,
                          E element,
                          ParameterBinder<E> parameterBinder,
                          Predicate<Attribute<E, ?>> filter,
                          GeneratedResultReader resultReader) {
        super(configuration, resultReader);
        this.element = element;
        this.filter = filter;
        this.parameterBinder = parameterBinder;
    }

    @Override
    public Scalar<Integer> execute(final QueryElement<Scalar<Integer>> query) {
        return new SuppliedScalar<>(new Supplier<Integer>() {
            @Override
            public Integer get() {
                // doesn't use the query params, just maps to the parameterBinder callback
                QueryGenerator generator = new QueryGenerator<>(query, null, false);
                QueryBuilder qb = new QueryBuilder(configuration.queryBuilderOptions());
                String sql = generator.toSql(qb, configuration.platform());
                int result;
                try (Connection connection = configuration.connectionProvider().getConnection()) {
                    StatementListener listener = configuration.statementListener();
                    try (PreparedStatement statement = prepare(sql, connection)) {
                        parameterBinder.bindParameters(statement, element, filter);
                        listener.beforeExecuteUpdate(statement, sql, null);
                        result = statement.executeUpdate();
                        listener.afterExecuteUpdate(statement);
                        readGeneratedKeys(0, statement);
                    }
                } catch (SQLException e) {
                    throw new PersistenceException(e);
                }
                return result;
            }
        }, configuration.writeExecutor());
    }
}
