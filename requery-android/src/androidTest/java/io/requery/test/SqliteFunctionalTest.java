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

package io.requery.test;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import io.requery.android.sqlite.DatabaseSource;
import io.requery.meta.EntityModel;
import io.requery.sql.EntityDataStore;
import io.requery.test.model.Models;
import org.junit.runner.RunWith;

import java.sql.SQLException;

/**
 * Reuses the core functional tests from the test project
 */
@RunWith(AndroidJUnit4.class)
public class SqliteFunctionalTest extends FunctionalTest {

    private DatabaseSource dataSource;

    @Override
    public void setup() throws SQLException {
        Context context = InstrumentationRegistry.getContext();
        final String dbName = "test.db";
        context.deleteDatabase(dbName);
        EntityModel model = Models.DEFAULT;
        dataSource = new DatabaseSource(context, model, dbName, 1);
        dataSource.setLoggingEnabled(true);
        data = new EntityDataStore<>(dataSource.getConfiguration());
    }

    @Override
    public void teardown() {
        super.teardown();
        if (dataSource != null) {
            dataSource.close();
        }
    }
}
