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

import io.requery.meta.EntityModel;
import io.requery.sql.Configuration;
import io.requery.sql.ConfigurationBuilder;
import io.requery.sql.Platform;
import io.requery.sql.SchemaModifier;
import io.requery.sql.TableCreationMode;
import io.requery.sql.platform.Derby;
import io.requery.sql.platform.H2;
import io.requery.sql.platform.HSQL;
import io.requery.sql.platform.PostgresSQL;
import io.requery.test.model.Person;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import javax.sql.CommonDataSource;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.Executors;

@RunWith(Parameterized.class)
public class SchemaModifierTest extends RandomData {

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Platform> data() {
        return Arrays.<Platform>asList(new PostgresSQL(), new H2(), new HSQL(), new Derby());
    }

    private Platform platform;

    public SchemaModifierTest(Platform platform) {
        this.platform = platform;
    }
    private SchemaModifier schemaModifier;

    @Before
    public void setup() throws SQLException {
        CommonDataSource dataSource = DatabaseType.getDataSource(platform);
        EntityModel model = io.requery.test.model.Models.DEFAULT;

        Configuration configuration = new ConfigurationBuilder(dataSource, model)
            .useDefaultLogging()
            .setStatementCacheSize(10)
            .setBatchUpdateSize(50)
            .setWriteExecutor(Executors.newSingleThreadExecutor())
            .build();

        schemaModifier = new SchemaModifier(configuration);
        try {
            schemaModifier.dropTables();
        } catch (Exception e) {
            // expected if 'drop if exists' not supported (so ignore in that case)
            if (!platform.supportsIfExists()) {
                throw e;
            }
        }
        schemaModifier.createTables(TableCreationMode.CREATE);
    }

    @Test
    public void testAddRemoveColumn() throws Exception {
        schemaModifier.dropColumn(Person.AGE);
        schemaModifier.addColumn(Person.AGE);
    }

    @Test
    public void testAddRemoveForeignKeyColumn() throws Exception {
        schemaModifier.dropColumn(Person.ADDRESS);
        schemaModifier.addColumn(Person.ADDRESS);
    }
}
