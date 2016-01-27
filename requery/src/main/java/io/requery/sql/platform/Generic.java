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
import io.requery.sql.IdentityColumnDefinition;
import io.requery.sql.LimitDefinition;
import io.requery.sql.Mapping;
import io.requery.sql.OffsetFetchLimitDefinition;
import io.requery.sql.Platform;
import io.requery.sql.UserVersionColumnDefinition;
import io.requery.sql.VersionColumnDefinition;

/**
 * Base platform implementation assuming standard ANSI SQL support.
 */
public class Generic implements Platform {

    private final GeneratedColumnDefinition generatedColumnDefinition;
    private final LimitDefinition limitSupport;
    private final VersionColumnDefinition versionColumnDefinition;

    public Generic() {
        generatedColumnDefinition = new IdentityColumnDefinition();
        limitSupport = new OffsetFetchLimitDefinition();
        versionColumnDefinition = new UserVersionColumnDefinition();
    }

    @Override
    public void addMappings(Mapping mapping) {

    }

    @Override
    public boolean supportsInlineForeignKeyReference() {
        return false;
    }

    @Override
    public boolean supportsIfExists() {
        return true;
    }

    @Override
    public boolean supportsGeneratedKeysInBatchUpdate() {
        return false;
    }

    @Override
    public boolean supportsGeneratedColumnsInPrepareStatement() {
        return true;
    }

    @Override
    public GeneratedColumnDefinition generatedColumnDefinition() {
        return generatedColumnDefinition;
    }

    @Override
    public LimitDefinition limitDefinition() {
        return limitSupport;
    }

    @Override
    public VersionColumnDefinition versionColumnDefinition() {
        return versionColumnDefinition;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName();
    }
}
