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

package io.requery.test;

import android.content.Context;
import androidx.test.InstrumentationRegistry;
import io.requery.android.sqlcipher.SqlCipherDatabaseSource;
import io.requery.android.sqlite.DatabaseProvider;
import io.requery.android.sqlite.DatabaseSource;
import io.requery.android.sqlitex.SqlitexDatabaseSource;
import io.requery.meta.EntityModel;
import io.requery.sql.EntityDataStore;
import io.requery.test.model.Models;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

/**
 * Reuses the core functional tests from the test project
 */
@RunWith(Parameterized.class)
public class SqliteFunctionalTest extends FunctionalTest {

    enum Type {
        ANDROID,  // default Android api
        SUPPORT,  // requery SQLite support library
        SQLCIPHER // SQLCipher
    }

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Type> data() {
        return Arrays.asList(Type.values());
    }

    private DatabaseProvider dataSource;
    private String dbName;

    public SqliteFunctionalTest(Type type) {
        EntityModel model = Models.DEFAULT;
        Context context = InstrumentationRegistry.getContext();
        dbName = type.toString().toLowerCase() + ".db";
        switch (type) {
            default:
            case ANDROID:
                dataSource = new DatabaseSource(context, model, dbName, 1);
                break;
            case SUPPORT:
                dataSource = new SqlitexDatabaseSource(context, model, dbName, 1);
                break;
            case SQLCIPHER:
                dataSource = new SqlCipherDatabaseSource(context, model, dbName, "test123", 1);
                break;
        }
    }

    @Override
    public void setup() {
        dataSource.setLoggingEnabled(true);
        Context context = InstrumentationRegistry.getContext();
        context.deleteDatabase(dbName);
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
