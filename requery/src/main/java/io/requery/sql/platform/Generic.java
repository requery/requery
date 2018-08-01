/*
 * Copyright 2018 requery.io
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
import io.requery.sql.IdentityColumnDefinition;
import io.requery.sql.Mapping;
import io.requery.sql.Platform;
import io.requery.sql.UserVersionColumnDefinition;
import io.requery.sql.VersionColumnDefinition;
import io.requery.sql.gen.Generator;
import io.requery.sql.gen.InsertGenerator;
import io.requery.sql.gen.LimitGenerator;
import io.requery.sql.gen.OffsetFetchGenerator;
import io.requery.sql.gen.OrderByGenerator;
import io.requery.sql.gen.UpdateGenerator;
import io.requery.sql.gen.UpsertMergeGenerator;

import java.util.Map;

/**
 * Base platform implementation assuming standard ANSI SQL support.
 */
public class Generic implements Platform {

    private final GeneratedColumnDefinition generatedColumnDefinition;
    private final LimitGenerator limitGenerator;
    private final VersionColumnDefinition versionColumnDefinition;
    private final Generator<QueryElement<?>> insertGenerator;
    private final Generator<Map<Expression<?>, Object>> updateGenerator;
    private final Generator<Map<Expression<?>, Object>> upsertGenerator;
    private final Generator<OrderByElement> orderByGenerator;

    public Generic() {
        generatedColumnDefinition = new IdentityColumnDefinition();
        limitGenerator = new OffsetFetchGenerator();
        versionColumnDefinition = new UserVersionColumnDefinition();
        insertGenerator = new InsertGenerator();
        updateGenerator = new UpdateGenerator();
        upsertGenerator = new UpsertMergeGenerator();
        orderByGenerator = new OrderByGenerator();
    }

    @Override
    public void addMappings(Mapping mapping) {
    }

    @Override
    public boolean supportsInlineForeignKeyReference() {
        return false;
    }

    @Override
    public boolean supportsAddingConstraint() {
        return true;
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
    public boolean supportsOnUpdateCascade() {
        return true;
    }

    @Override
    public boolean supportsUpsert() {
        return true;
    }

    @Override
    public GeneratedColumnDefinition generatedColumnDefinition() {
        return generatedColumnDefinition;
    }

    @Override
    public Generator<LimitedElement> limitGenerator() {
        return limitGenerator;
    }

    @Override
    public VersionColumnDefinition versionColumnDefinition() {
        return versionColumnDefinition;
    }

    @Override
    public Generator<QueryElement<?>> insertGenerator() {
        return insertGenerator;
    }

    @Override
    public Generator<Map<Expression<?>, Object>> updateGenerator() {
        return updateGenerator;
    }

    @Override
    public Generator<Map<Expression<?>, Object>> upsertGenerator() {
        return upsertGenerator;
    }

    @Override
    public Generator<OrderByElement> orderByGenerator() {
        return orderByGenerator;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName();
    }
}
