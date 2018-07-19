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

package io.requery.sql.platform;

import io.requery.query.Expression;
import io.requery.query.element.LimitedElement;
import io.requery.query.element.OrderByElement;
import io.requery.query.element.QueryElement;
import io.requery.sql.GeneratedColumnDefinition;
import io.requery.sql.Mapping;
import io.requery.sql.Platform;
import io.requery.sql.VersionColumnDefinition;
import io.requery.sql.gen.Generator;

import java.sql.Connection;
import java.util.Map;

/**
 * Given a {@link Connection} will pick a existing platform type based on the JDBC driver
 * metadata product name, otherwise will use {@link Generic} as a default.
 *
 * @author Nikhil Purushe
 */
public class PlatformDelegate implements Platform {

    private final Platform platform;

    public PlatformDelegate(Connection connection) {
        platform = new PlatformFromConnection().apply(connection);
    }

    @Override
    public void addMappings(Mapping mapping) {
        platform.addMappings(mapping);
    }

    @Override
    public boolean supportsIfExists() {
        return platform.supportsIfExists();
    }

    @Override
    public boolean supportsInlineForeignKeyReference() {
        return platform.supportsInlineForeignKeyReference();
    }

    @Override
    public boolean supportsAddingConstraint() {
        return platform.supportsAddingConstraint();
    }

    @Override
    public boolean supportsGeneratedKeysInBatchUpdate() {
        return platform.supportsGeneratedKeysInBatchUpdate();
    }

    @Override
    public boolean supportsGeneratedColumnsInPrepareStatement() {
        return platform.supportsGeneratedColumnsInPrepareStatement();
    }

    @Override
    public boolean supportsOnUpdateCascade() {
        return platform.supportsOnUpdateCascade();
    }

    @Override
    public boolean supportsUpsert() {
        return platform.supportsUpsert();
    }

    @Override
    public GeneratedColumnDefinition generatedColumnDefinition() {
        return platform.generatedColumnDefinition();
    }

    @Override
    public Generator<LimitedElement> limitGenerator() {
        return platform.limitGenerator();
    }

    @Override
    public VersionColumnDefinition versionColumnDefinition() {
        return platform.versionColumnDefinition();
    }

    @Override
    public Generator<QueryElement<?>> insertGenerator() {
        return platform.insertGenerator();
    }

    @Override
    public Generator<Map<Expression<?>, Object>> updateGenerator() {
        return platform.updateGenerator();
    }

    @Override
    public Generator<Map<Expression<?>, Object>> upsertGenerator() {
        return platform.upsertGenerator();
    }

    @Override
    public Generator<OrderByElement> orderByGenerator() {
        return platform.orderByGenerator();
    }

    @Override
    public String toString() {
        return platform.toString();
    }
}
