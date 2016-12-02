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

import io.requery.Converter;
import io.requery.PersistenceException;
import io.requery.ReferentialAction;
import io.requery.meta.Attribute;
import io.requery.meta.EntityModel;
import io.requery.meta.Type;
import io.requery.sql.platform.PlatformDelegate;
import io.requery.sql.type.IntegerType;
import io.requery.util.Objects;
import io.requery.util.function.Predicate;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import static io.requery.sql.Keyword.*;

/**
 * Given a {@link EntityModel} generates and executes DDL statements to create the corresponding
 * tables in a SQL database.
 *
 * @author Nikhil Purushe
 */
public class SchemaModifier {

    private final ConnectionProvider connectionProvider;
    private final EntityModel model;
    private final CompositeStatementListener statementListeners;
    private final Configuration configuration;
    private Mapping mapping;
    private Platform platform;
    private QueryBuilder.Options queryOptions;

    /**
     * Create a new {@link SchemaModifier} instance with the default {@link Mapping}.
     *
     * @param dataSource {@link DataSource} to use
     * @param model      entity model
     */
    public SchemaModifier(DataSource dataSource, EntityModel model) {
        this(new ConfigurationBuilder(dataSource, model).build());
    }

    /**
     * Create a new {@link SchemaModifier} instance with a specific {@link Mapping}.
     *
     * @param configuration data configuration
     */
    public SchemaModifier(Configuration configuration) {
        this.configuration = configuration;
        this.connectionProvider = configuration.getConnectionProvider();
        this.platform = configuration.getPlatform();
        this.model = Objects.requireNotNull(configuration.getModel());
        this.mapping = configuration.getMapping();
        this.statementListeners =
            new CompositeStatementListener(configuration.getStatementListeners());
        if (configuration.getUseDefaultLogging()) {
            statementListeners.add(new LoggingListener());
        }
    }

    private synchronized Connection getConnection() throws SQLException {
        Connection connection = connectionProvider.getConnection();
        if (platform == null) {
            platform = new PlatformDelegate(connection);
        }
        if (mapping == null) {
            mapping = new GenericMapping(platform);
        }
        return connection;
    }

    private QueryBuilder createQueryBuilder() {
        if (queryOptions == null) {
            try (Connection connection = getConnection()) {
                String quoteIdentifier = connection.getMetaData().getIdentifierQuoteString();
                queryOptions = new QueryBuilder.Options(quoteIdentifier, true,
                    configuration.getTableTransformer(),
                    configuration.getColumnTransformer(),
                    configuration.getQuoteTableNames(),
                    configuration.getQuoteColumnNames());
            } catch (SQLException e) {
                throw new PersistenceException(e);
            }
        }
        return new QueryBuilder(queryOptions);
    }

    /**
     * Create the tables over the connection.
     *
     * @param mode creation mode.
     * @throws TableModificationException if the creation fails.
     */
    public void createTables(TableCreationMode mode) {
        ArrayList<Type<?>> sorted = sortTypes();
        try (Connection connection = getConnection();
             Statement statement = connection.createStatement()) {

            connection.setAutoCommit(false);
            if (mode == TableCreationMode.DROP_CREATE) {
                executeDropStatements(statement);
            }
            for (Type<?> type : sorted) {
                String sql = tableCreateStatement(type, mode);
                statementListeners.beforeExecuteUpdate(statement, sql, null);
                statement.execute(sql);
                statementListeners.afterExecuteUpdate(statement);
            }
            for (Type<?> type : sorted) {
                createIndexes(connection, mode, type);
            }
            connection.commit();
        } catch (SQLException e) {
            throw new TableModificationException(e);
        }
    }

    /**
     * Convenience method to generated the create table statements as a string.
     *
     * @param mode table creation mode
     * @return DDL string
     */
    public String createTablesString(TableCreationMode mode) {
        ArrayList<Type<?>> sorted = sortTypes();
        StringBuilder sb = new StringBuilder();
        for (Type<?> type : sorted) {
            String sql = tableCreateStatement(type, mode);
            sb.append(sql);
            sb.append(";\n");
        }
        return sb.toString();
    }

    /**
     * Drop all tables in the schema. Note if the platform supports if exists that will be used in
     * the statement, if not and the table doesn't exist an exception will be thrown.
     */
    public void dropTables() {
        try (Connection connection = getConnection();
             Statement statement = connection.createStatement()) {
            executeDropStatements(statement);
        } catch (SQLException e) {
            throw new TableModificationException(e);
        }
    }

    private void executeDropStatements(Statement statement) throws SQLException {
        ArrayList<Type<?>> reversed = sortTypes();
        Collections.reverse(reversed);
        for (Type<?> type : reversed) {
            QueryBuilder qb = createQueryBuilder();
            qb.keyword(DROP, TABLE);
            if (platform.supportsIfExists()) {
                qb.keyword(IF, EXISTS);
            }
            qb.tableName(type.getName());
            try {
                String sql = qb.toString();
                statementListeners.beforeExecuteUpdate(statement, sql, null);
                statement.execute(sql);
                statementListeners.afterExecuteUpdate(statement);
            } catch (SQLException e) {
                if (platform.supportsIfExists()) {
                    throw e;
                }
            }
        }
    }

    /**
     * Alters the attribute's table and add's the column representing the given {@link Attribute}.
     *
     * @param attribute being added
     * @param <T>       parent type of the attribute
     */
    public <T> void addColumn(Attribute<T, ?> attribute) {
        Type<T> type = attribute.getDeclaringType();
        QueryBuilder qb = createQueryBuilder();
        qb.keyword(ALTER, TABLE)
            .tableName(type.getName());
        if (attribute.isForeignKey()) {
            if (platform.supportsAddingConstraint()) {
                // create the column first then the constraint
                qb.keyword(ADD, COLUMN);
                createColumn(qb, attribute);
                executeSql(qb);
                qb = createQueryBuilder();
                qb.keyword(ALTER, TABLE)
                    .tableName(type.getName()).keyword(ADD);
                createForeignKeyColumn(qb, attribute, true);
            } else {
                // just for SQLite for now adding the column and key is done in 1 statement
                qb = createQueryBuilder();
                qb.keyword(ALTER, TABLE)
                    .tableName(type.getName()).keyword(ADD);
                createForeignKeyColumn(qb, attribute, false);
            }
        } else {
            qb.keyword(ADD, COLUMN);
            createColumn(qb, attribute);
            executeSql(qb);
        }
    }

    /**
     * Alters the attribute's table and removes the column representing the given {@link Attribute}.
     *
     * @param attribute being added
     * @param <T>       parent type of the attribute
     */
    public <T> void dropColumn(Attribute<T, ?> attribute) {
        Type<T> type = attribute.getDeclaringType();
        if (attribute.isForeignKey()) {
            // TODO MySQL need to drop FK constraint first
        }
        QueryBuilder qb = createQueryBuilder();
        qb.keyword(ALTER, TABLE)
            .tableName(type.getName())
            .keyword(DROP, COLUMN)
            .attribute(attribute);
        executeSql(qb);
    }

    private void executeSql(QueryBuilder qb) {
        try (Connection connection = getConnection()) {
            executeSql(connection, qb);
        } catch (SQLException e) {
            throw new PersistenceException(e);
        }
    }

    private void executeSql(Connection connection, QueryBuilder qb) {
        try (Statement statement = connection.createStatement()) {
            String sql = qb.toString();
            statementListeners.beforeExecuteUpdate(statement, sql, null);
            statement.execute(sql);
            statementListeners.afterExecuteUpdate(statement);
        } catch (SQLException e) {
            throw new PersistenceException(e);
        }
    }

    private ArrayList<Type<?>> sortTypes() {
        // sort the types in table creation order to avoid referencing not created table via a
        // reference (could also add constraints at the end but SQLite doesn't support that)
        ArrayDeque<Type<?>> queue = new ArrayDeque<>(model.getTypes());
        ArrayList<Type<?>> sorted = new ArrayList<>();
        while (!queue.isEmpty()) {
            Type<?> type = queue.poll();
            Set<Type<?>> referencing = referencedTypesOf(type);

            for (Type<?> referenced : referencing) {
                Set<Type<?>> backReferences = referencedTypesOf(referenced);
                if (backReferences.contains(type)) {
                    throw new CircularReferenceException("circular reference detected between "
                        + type.getName() + " and " + referenced.getName());
                }
            }
            if (referencing.isEmpty() || sorted.containsAll(referencing)) {
                sorted.add(type);
                queue.remove(type);
            } else {
                queue.offer(type); // put back
            }
        }
        return sorted;
    }

    private Set<Type<?>> referencedTypesOf(Type<?> type) {
        Set<Type<?>> referencedTypes = new LinkedHashSet<>();
        for (Attribute<?, ?> attribute : type.getAttributes()) {
            if (attribute.isForeignKey()) {
                Class<?> referenced = attribute.getReferencedClass() == null ?
                        attribute.getClassType() :
                        attribute.getReferencedClass();
                if (referenced != null) {
                    for (Type<?> t : model.getTypes()) {
                        if (type != t && referenced.isAssignableFrom(t.getClassType())) {
                            referencedTypes.add(t);
                        }
                    }
                }
            }
        }
        return Collections.unmodifiableSet(referencedTypes);
    }

    public <T> String tableCreateStatement(Type<T> type, TableCreationMode mode) {
        String tableName = type.getName();
        Set<Attribute<T, ?>> attributes = type.getAttributes();

        QueryBuilder qb = createQueryBuilder();
        qb.keyword(CREATE, TABLE);
        if (mode == TableCreationMode.CREATE_NOT_EXISTS) {
            qb.keyword(IF, NOT, EXISTS);
        }
        qb.tableName(tableName);
        qb.openParenthesis();

        int index = 0;
        // columns to define first
        Predicate<Attribute> filter = new Predicate<Attribute>() {
            @Override
            public boolean test(Attribute value) {
                if (value.isVersion() &&
                    !platform.versionColumnDefinition().createColumn()) {
                    return false;
                }
                if (platform.supportsInlineForeignKeyReference()) {
                    return !value.isForeignKey() && !value.isAssociation();
                } else {
                    return value.isForeignKey() || !value.isAssociation();
                }
            }
        };

        for (Attribute attribute : attributes) {
            if (filter.test(attribute)) {
                if (index > 0) {
                    qb.comma();
                }
                createColumn(qb, attribute);
                index++;
            }
        }
        // foreign keys
        for (Attribute attribute : attributes) {
            if (attribute.isForeignKey()) {
                if (index > 0) {
                    qb.comma();
                }
                createForeignKeyColumn(qb, attribute, true);
                index++;
            }
        }
        // composite primary key
        if(type.getKeyAttributes().size() > 1) {
            if (index > 0) {
                qb.comma();
            }
            qb.keyword(PRIMARY, KEY);
            qb.openParenthesis();
            qb.commaSeparated(type.getKeyAttributes(),
                    new QueryBuilder.Appender<Attribute<T, ?>>() {
                @Override
                public void append(QueryBuilder qb, Attribute<T, ?> value) {
                    qb.attribute(value);
                }
            });
            qb.closeParenthesis();
        }
        qb.closeParenthesis();
        return qb.toString();
    }

    private void createForeignKeyColumn(QueryBuilder qb, Attribute<?,?> attribute,
                                        boolean forCreateStatement) {

        Type<?> referenced = model.typeOf(attribute.getReferencedClass() != null ?
            attribute.getReferencedClass() : attribute.getClassType());

        final Attribute referencedAttribute;
        if (attribute.getReferencedAttribute() != null) {
            referencedAttribute = attribute.getReferencedAttribute().get();
        } else {
            referencedAttribute = referenced.getKeyAttributes().iterator().next();
        }

        if (!platform.supportsInlineForeignKeyReference() && forCreateStatement) {
            qb.keyword(FOREIGN, KEY)
                .openParenthesis()
                .attribute(attribute)
                .closeParenthesis()
                .space();
        } else {
            qb.attribute(attribute);
            FieldType fieldType = null;
            if (referencedAttribute != null) {
                fieldType = mapping.mapAttribute(referencedAttribute);
            }
            if (fieldType == null) {
                fieldType = new IntegerType(int.class);
            }
            qb.value(fieldType.getIdentifier());
        }
        qb.keyword(REFERENCES);
        qb.tableName(referenced.getName());
        if (referencedAttribute != null) {
            qb.openParenthesis()
                .attribute(referencedAttribute)
                .closeParenthesis()
                .space();
        }
        if (attribute.getDeleteAction() != null) {
            qb.keyword(ON, DELETE);
            appendReferentialAction(qb, attribute.getDeleteAction());
        }
        if (referencedAttribute != null &&
            !referencedAttribute.isGenerated() && attribute.getUpdateAction() != null) {
            qb.keyword(ON, UPDATE);
            appendReferentialAction(qb, attribute.getUpdateAction());
        }
    }

    private void appendReferentialAction(QueryBuilder qb, ReferentialAction action) {
        switch (action) {
            case CASCADE:
                qb.keyword(CASCADE);
                break;
            case NO_ACTION:
                qb.keyword(NO, ACTION);
                break;
            case RESTRICT:
                qb.keyword(RESTRICT);
                break;
            case SET_DEFAULT:
                qb.keyword(SET, DEFAULT);
                break;
            case SET_NULL:
                qb.keyword(SET, NULL);
                break;
        }
    }

    private void createColumn(QueryBuilder qb, Attribute<?,?> attribute) {

        qb.attribute(attribute);
        FieldType fieldType = mapping.mapAttribute(attribute);
        GeneratedColumnDefinition generatedColumnDefinition = platform.generatedColumnDefinition();

        if(!(attribute.isGenerated() && generatedColumnDefinition.skipTypeIdentifier())) {

            // type id
            Object identifier = fieldType.getIdentifier();
            // type length
            Converter converter = attribute.getConverter();
            if (converter == null && mapping instanceof GenericMapping) {
                GenericMapping genericMapping = (GenericMapping) mapping;
                converter = genericMapping.converterForType(attribute.getClassType());
            }
            boolean hasLength = fieldType.hasLength() ||
                    (converter != null && converter.getPersistedSize() != null);

            if (attribute.getDefinition() != null && attribute.getDefinition().length() > 0) {
                qb.append(attribute.getDefinition());
            } else if (hasLength) {

                Integer length = attribute.getLength();
                if (length == null && converter != null) {
                    length = converter.getPersistedSize();
                }
                if (length == null) {
                    length = fieldType.getDefaultLength();
                }
                if (length == null) {
                    length = 255;
                }
                qb.append(identifier)
                        .openParenthesis()
                        .append(length)
                        .closeParenthesis();
            } else {
                qb.append(identifier);
            }
            qb.space();
        }

        String suffix = fieldType.getIdentifierSuffix();
        if(suffix != null) {
            qb.append(suffix).space();
        }
        // generate the primary key
        if (attribute.isKey() && !attribute.isForeignKey()) {
            if (attribute.isGenerated() && !generatedColumnDefinition.postFixPrimaryKey()) {
                generatedColumnDefinition.appendGeneratedSequence(qb, attribute);
                qb.space();
            }
            // if more than one Primary key declaration appears at the end not inline
            if (attribute.getDeclaringType().getKeyAttributes().size() == 1) {
                qb.keyword(PRIMARY, KEY);
            }
            if (attribute.isGenerated() && generatedColumnDefinition.postFixPrimaryKey()) {
                generatedColumnDefinition.appendGeneratedSequence(qb, attribute);
                qb.space();
            }
        } else if (attribute.isGenerated()) {
            generatedColumnDefinition.appendGeneratedSequence(qb, attribute);
            qb.space();
        }
        if (attribute.getCollate() != null && attribute.getCollate().length() > 0) {
            qb.keyword(COLLATE);
            qb.append(attribute.getCollate());
            qb.space();
        }
        if (attribute.getDefaultValue() != null && attribute.getDefaultValue().length() > 0) {
            qb.keyword(DEFAULT);
            qb.append(attribute.getDefaultValue());
            qb.space();
        }
        if (!attribute.isNullable()) {
            qb.keyword(NOT, NULL);
        }
        if (attribute.isUnique()) {
            qb.keyword(UNIQUE);
        }
    }

    private <T> void createIndexes(Connection connection, TableCreationMode mode, Type<T> type) {
        Set<Attribute<T, ?>> attributes = type.getAttributes();
        Map<String, Set<Attribute<?, ?>>> indexes = new LinkedHashMap<>();
        for (Attribute<T, ?> attribute : attributes) {
            if (attribute.isIndexed()) {
                Set<String> names = new LinkedHashSet<>(attribute.getIndexNames());
                if (names.isEmpty()) {
                    // if no name set create a default one
                    String indexName = attribute.getName() + "_index";
                    names.add(indexName);
                }
                for(String indexName : names) {
                    Set<Attribute<?, ?>> indexColumns = indexes.get(indexName);
                    if (indexColumns == null) {
                        indexes.put(indexName, indexColumns = new LinkedHashSet<>());
                    }
                    indexColumns.add(attribute);
                }
            }
        }
        for (Map.Entry<String, Set<Attribute<?, ?>>> entry : indexes.entrySet()) {
            QueryBuilder qb = createQueryBuilder();
            createIndex(qb, entry.getKey(), entry.getValue(), type, mode);
            executeSql(connection, qb);
        }
    }

    private void createIndex(QueryBuilder qb,
                             String indexName,
                             Set<Attribute<?,?>> attributes,
                             Type<?> type, TableCreationMode mode) {
        qb.keyword(CREATE);
        if ((attributes.size() >= 1 && attributes.iterator().next().isUnique()) ||
            (type.getTableUniqueIndexes() != null &&
             Arrays.asList(type.getTableUniqueIndexes()).contains(indexName))) {
            qb.keyword(UNIQUE);
        }
        qb.keyword(INDEX);
        // works on SQLite only?
        if (mode == TableCreationMode.CREATE_NOT_EXISTS) {
            qb.keyword(IF, NOT, EXISTS);
        }
        qb.append(indexName).space()
            .keyword(ON)
            .tableName(type.getName())
            .openParenthesis()
            .commaSeparated(attributes, new QueryBuilder.Appender<Attribute>() {
                @Override
                public void append(QueryBuilder qb, Attribute value) {
                    qb.attribute(value);
                }
            })
            .closeParenthesis();
    }
}
