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

import javax.annotation.Nonnull;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Base class for insert/update operations.
 *
 * @author Nikhil Purushe
 */
abstract class PreparedQueryOperation {

    private static Logger logger = Logger.getLogger("PreparedQueryOperation");

    final RuntimeConfiguration configuration;
    private final EntityModel model;
    private final GeneratedResultReader generatedResultReader;

    PreparedQueryOperation(RuntimeConfiguration configuration,
                           GeneratedResultReader generatedResultReader) {
        this.configuration = configuration;
        this.generatedResultReader = generatedResultReader;
        this.model = configuration.getModel();
    }

    PreparedStatement prepare(String sql, Connection connection) throws SQLException {
        PreparedStatement statement;

        if (generatedResultReader != null) {
            if (configuration.getPlatform().supportsGeneratedColumnsInPrepareStatement()) {
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
            Class<?> type = value == null ? null : value.getClass();
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
            logger.log(Level.INFO, "Expression name={0}, index={1}, value={2}", new Object[] { expression.getName(), i, value });
            configuration.getMapping().write(expression, statement, i + 1, value);
        }
    }

    void mapParameters(@Nonnull PreparedStatement statement,
                       @Nonnull BoundNamedParameters namedParameters,
                       @Nonnull Map<String, List<Integer>> namedIndexMap) throws SQLException {

        for (int i = 0; i < namedParameters.count(); i++) {
            Expression expression = namedParameters.expressionAt(i);
            List<Integer> indexes = namedIndexMap.get(expression.getName());
            Object value = namedParameters.valueAt(i);

            if (expression instanceof Attribute) {
                Attribute attribute = (Attribute) expression;
                if (attribute.isAssociation()) {
                    // get the referenced value
                    value = Attributes.replaceKeyReference(value, attribute);
                }
            }
            Class<?> type = value == null ? null : value.getClass();
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

            logger.log(Level.INFO, "Expression name={0}, indexes={1}, value={2}", new Object[] { expression.getName(), indexes, value });

            for (int index : indexes) {
                configuration.getMapping().write(expression, statement, index, value);
            }
        }
    }

    void readGeneratedKeys(int index, Statement statement) throws SQLException {
        if (generatedResultReader != null) {
            try (ResultSet results = statement.getGeneratedKeys()) {
                generatedResultReader.read(index, results);
            }
        }
    }

    /**
     * IN 같이 배열인 경우 requery는 내부 인자를 모두 개별로 처리하도록 했다.
     * Named Parameter parsing 시에는 배열도 하나의 Parameter로 처리해서 인자 수가 맞지 않는다.
     * {@link io.requery.sql.ParameterInliner} 를 분석해서 NamedParameterInliner를 새로 만들어야 한다.
     * @param query
     * @param paramMap
     * @return
     */
    @Nonnull
    static String parse(@Nonnull final String query, @Nonnull final Map<String, List<Integer>> paramMap) {
        int length = query.length();
        StringBuilder parsedQuery = new StringBuilder(length);
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;
        int index = 1;

        int i = 0;
        while (i < length) {
            char c = query.charAt(i);
            if (inSingleQuote) {
                if (c == '\'') {
                    inSingleQuote = false;
                }
            } else if (inDoubleQuote) {
                if (c == '"') {
                    inDoubleQuote = false;
                }
            } else {
                if (c == '\'') {
                    inSingleQuote = true;
                } else if (c == '"') {
                    inDoubleQuote = true;
                } else if (c == ':' && i + 1 < length && Character.isJavaIdentifierStart(query.charAt(i + 1))) {
                    int j = i + 2;
                    while (j < length && Character.isJavaIdentifierPart(query.charAt(j))) {
                        j++;
                    }
                    String name = query.substring(i + 1, j);
                    c = '?'; // replace the parameter with a question mark
                    i += name.length(); // skip past the end if the parameter

                    List<Integer> indexList = paramMap.get(name);
                    if (indexList == null) {
                        indexList = new ArrayList<>();
                        paramMap.put(name, indexList);
                    }
                    indexList.add(index);

                    index++;
                }
            }
            parsedQuery.append(c);
            i++;
        }


        String parsed = parsedQuery.toString();
        logger.log(Level.INFO, "Parsed SQL={0}", parsed);
        return parsed;
    }
}
