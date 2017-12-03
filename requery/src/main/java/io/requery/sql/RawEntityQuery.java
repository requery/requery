/*
 * Copyright 2017 requery.io
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
import io.requery.meta.Type;
import io.requery.query.BaseResult;
import io.requery.query.Result;
import io.requery.util.CloseableIterator;
import io.requery.util.function.Predicate;
import io.requery.util.function.Supplier;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Executes a raw string query using a prepared statement with optional parameters returning a
 * {@link Result} of entity elements.
 *
 * @author Nikhil Purushe
 */
class RawEntityQuery<E extends S, S> extends PreparedQueryOperation implements
    Supplier<Result<E>> {

    private final EntityReader<E, S> reader;
    private final Type<E> type;
    private final BoundParameters boundParameters;
    private final String sql;

    RawEntityQuery(EntityContext<S> context, Class<E> cls, String sql, Object[] parameters) {
        super(context, null);
        ParameterInliner inlined = new ParameterInliner(sql, parameters).apply();
        this.type = configuration.getModel().typeOf(cls);
        this.sql = inlined.sql();
        this.reader = context.read(cls);
        boundParameters = new BoundParameters(inlined.parameters());
    }

    @Override
    public Result<E> get() {
        PreparedStatement statement = null;
        try {
            Connection connection = configuration.getConnection();
            statement = prepare(sql, connection);
            mapParameters(statement, boundParameters);
            return new EntityResult(statement);
        } catch (Exception e) {
            throw StatementExecutionException.closing(statement, e, sql);
        }
    }

    private class EntityResult extends BaseResult<E> {

        private final PreparedStatement statement;

        private EntityResult(PreparedStatement statement) {
            this.statement = statement;
        }

        @Override
        public CloseableIterator<E> createIterator(int skip, int take) {
            try {
                StatementListener listener = configuration.getStatementListener();
                listener.beforeExecuteQuery(statement, sql, boundParameters);
                ResultSet results = statement.executeQuery();
                listener.afterExecuteQuery(statement);
                // read the result meta data
                ResultSetMetaData metadata = results.getMetaData();
                // map of entity column names to attributes
                Map<String, Attribute<E, ?>> map = new HashMap<>();
                for (Attribute<E, ?> attribute : type.getAttributes()) {
                    map.put(attribute.getName().toLowerCase(Locale.ROOT), attribute);
                }
                Set<Attribute<E, ?>> attributes = new LinkedHashSet<>();
                for (int i = 0; i < metadata.getColumnCount(); i++) {
                    String name = metadata.getColumnName(i + 1);
                    Attribute<E, ?> attribute = map.get(name.toLowerCase(Locale.ROOT));
                    if (attribute != null) {
                        attributes.add(attribute);
                    }
                }
                Attribute[] array = Attributes.toArray(attributes,
                    new Predicate<Attribute<E, ?>>() {
                    @Override
                    public boolean test(Attribute<E, ?> value) {
                        return true;
                    }
                });
                EntityResultReader<E, S> entityReader = new EntityResultReader<>(reader, array);
                return new ResultSetIterator<>(entityReader, results, null, true, true);
            } catch (SQLException e) {
                throw new PersistenceException(e);
            }
        }
    }
}
