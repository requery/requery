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

import io.requery.sql.GeneratedColumnDefinition;
import io.requery.sql.LimitDefinition;
import io.requery.sql.Mapping;
import io.requery.sql.Platform;
import io.requery.sql.UpsertDefinition;
import io.requery.sql.VersionColumnDefinition;

import java.sql.Connection;

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
    public boolean supportsUpsert() {
        return platform.supportsUpsert();
    }

    @Override
    public GeneratedColumnDefinition generatedColumnDefinition() {
        return platform.generatedColumnDefinition();
    }

    @Override
    public LimitDefinition limitDefinition() {
        return platform.limitDefinition();
    }

    @Override
    public VersionColumnDefinition versionColumnDefinition() {
        return platform.versionColumnDefinition();
    }

    @Override
    public UpsertDefinition upsertDefinition() {
        return platform.upsertDefinition();
    }

    @Override
    public String toString() {
        return platform.toString();
    }
}
