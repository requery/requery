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
import io.requery.meta.EntityModel;
import io.requery.meta.Type;
import io.requery.query.Expression;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Base class for insert/update operations.
 *
 * @author Nikhil Purushe
 */
abstract class PreparedQueryOperation {

    final RuntimeConfiguration configuration;
    private final EntityModel model;
    private final GeneratedResultReader generatedResultReader;

    PreparedQueryOperation(RuntimeConfiguration configuration,
                           GeneratedResultReader generatedResultReader) {
        this.configuration = configuration;
        this.generatedResultReader = generatedResultReader;
        this.model = configuration.model();
    }

    PreparedStatement prepare(String sql, Connection connection) throws SQLException {
        PreparedStatement statement;
        if (generatedResultReader != null) {
            if (configuration.platform().supportsGeneratedColumnsInPrepareStatement()) {
                String[] generatedColumns = generatedResultReader.generatedColumns();
                statement = connection.prepareStatement(sql, generatedColumns);
            } else {
                statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            }
        } else {
            statement = connection.prepareStatement(sql, Statement.NO_GENERATED_KEYS);
        }
        return statement;
    }

    void mapParameters(PreparedStatement statement, BoundParameters parameters)
        throws SQLException {

        for (int i = 0; i < parameters.count(); i++) {
            Expression expression = parameters.expressionAt(i);
            Object value = parameters.valueAt(i);
            if (expression instanceof Attribute) {
                Attribute attribute = (Attribute) expression;
                if (attribute.isAssociation()) {
                    // get the referenced value
                    value = Attributes.replaceKeyReference(value, attribute);
                }
            }
            Class<?> type = value == null ? null: value.getClass();
            if (type != null) {
                // allows entity arguments with single keys to be remapped to their keys
                if (model.containsTypeOf(type)) {
                    Type<Object> entityType = model.typeOf(type);
                    Attribute<Object, ?> keyAttribute = entityType.getSingleKeyAttribute();
                    if (keyAttribute != null) {
                        value = keyAttribute.getProperty().get(value);
                        expression = (Expression) keyAttribute;
                    }
                }
            }
            configuration.mapping().write(expression, statement, i + 1, value);
        }
    }

    void readGeneratedKeys(int index, Statement statement) throws SQLException {
        if (generatedResultReader != null) {
            try (ResultSet results = statement.getGeneratedKeys()) {
                generatedResultReader.read(index, results);
            }
        }
    }
}
