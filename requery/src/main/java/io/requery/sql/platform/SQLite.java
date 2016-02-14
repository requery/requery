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

import io.requery.sql.AutoIncrementColumnDefinition;
import io.requery.sql.GeneratedColumnDefinition;
import io.requery.sql.LimitOffsetDefinition;
import io.requery.sql.LimitDefinition;

/**
 * SQLite. (3.6 and later)
 */
public class SQLite extends Generic {

    private final AutoIncrementColumnDefinition autoIncrementColumn;
    private final LimitDefinition limitDefinition;

    public SQLite() {
        autoIncrementColumn = new AutoIncrementColumnDefinition("autoincrement");
        limitDefinition = new LimitOffsetDefinition();
    }

    @Override
    public boolean supportsGeneratedKeysInBatchUpdate() {
        return false;
    }

    @Override
    public boolean supportsAddingConstraint() {
        return false;
    }

    @Override
    public GeneratedColumnDefinition generatedColumnDefinition() {
        return autoIncrementColumn;
    }

    @Override
    public LimitDefinition limitDefinition() {
        return limitDefinition;
    }
}
