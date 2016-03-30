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

import io.requery.meta.Attribute;
import io.requery.proxy.EntityProxy;
import io.requery.query.Expression;
import io.requery.query.Scalar;
import io.requery.query.SuppliedScalar;
import io.requery.query.element.QueryElement;
import io.requery.util.function.Supplier;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

class EntityUpsertOperation<E> extends UpdateOperation {

    private final Iterable<Attribute<E, ?>> attributes;
    private final EntityProxy<E> proxy;

    EntityUpsertOperation(RuntimeConfiguration configuration, EntityProxy<E> proxy,
                          Iterable<Attribute<E, ?>> attributes) {
        super(configuration, null);
        this.proxy = proxy;
        this.attributes = attributes;
    }

    @Override
    public Scalar<Integer> execute(final QueryElement<Scalar<Integer>> query) {
        return new SuppliedScalar<>(new Supplier<Integer>() {
            @Override
            public Integer get() {
                QueryBuilder qb = new QueryBuilder(configuration.queryBuilderOptions());
                Platform platform = configuration.platform();
                UpsertDefinition upsertDefinition = platform.upsertDefinition();
                final BoundParameters parameters = new BoundParameters();
                UpsertDefinition.Parameterizer<E> parameterizer =
                    new UpsertDefinition.Parameterizer<E>() {
                    @Override
                    public void addParameter(Attribute<E, ?> attribute) {
                        Object value = proxy.get(attribute);
                        @SuppressWarnings("unchecked")
                        Expression<Object> expression = (Expression<Object>) attribute;
                        parameters.add(expression, value);
                    }
                };
                upsertDefinition.appendUpsert(qb, attributes, parameterizer);
                String sql = qb.toString();
                int result;
                try (Connection connection = configuration.connectionProvider().getConnection()) {
                    StatementListener listener = configuration.statementListener();
                    try (PreparedStatement statement = prepare(sql, connection)) {
                        listener.beforeExecuteUpdate(statement, sql, parameters);
                        mapParameters(statement, parameters);
                        result = statement.executeUpdate();
                        listener.afterExecuteUpdate(statement);
                    }
                } catch (SQLException e) {
                    throw new StatementExecutionException(e, sql);
                }
                return result;
            }
        }, configuration.writeExecutor());
    }
}
