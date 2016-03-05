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

import io.requery.cache.EntityCacheBuilder;
import io.requery.meta.EntityModel;
import io.requery.query.Result;
import io.requery.sql.Configuration;
import io.requery.sql.ConfigurationBuilder;
import io.requery.sql.QueryableStore;
import io.requery.sql.SchemaModifier;
import io.requery.sql.TableCreationMode;
import io.requery.sql.platform.SQLite;
import io.requery.test.modelautovalue.Models;
import io.requery.test.modelautovalue.Person;
import io.requery.test.modelautovalue.PersonEntity;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.sql.CommonDataSource;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import static org.junit.Assert.assertTrue;

public class AutoValueModelTest {

    protected QueryableStore<Serializable> data;

    int randomPerson() throws MalformedURLException {
        Random random = new Random();
        String[] firstNames = new String[]{"Alice", "Bob", "Carol"};
        String[] lastNames = new String[]{"Smith", "Lee", "Jones"};
        String name = firstNames[random.nextInt(firstNames.length)] + " " +
            lastNames[random.nextInt(lastNames.length)];

        Calendar calendar = Calendar.getInstance();
        //noinspection MagicConstant
        calendar.set(1900 + random.nextInt(90), random.nextInt(12), random.nextInt(30));

        return data.insert(Person.class)
            .value(PersonEntity.NAME, name)
            .value(PersonEntity.AGE, random.nextInt(50))
            .value(PersonEntity.EMAIL, name.replaceAll(" ", "").toLowerCase() + "@example.com")
            .value(PersonEntity.UUID, UUID.randomUUID())
            .value(PersonEntity.BIRTHDAY, calendar.getTime())
            .value(PersonEntity.ABOUT, "About this person")
            .value(PersonEntity.HOMEPAGE, new URL("http://www.google.com"))
            .get().first().get(PersonEntity.ID);
    }

    @Before
    public void setup() throws SQLException {
        CommonDataSource dataSource = DatabaseType.getDataSource(new SQLite());
        EntityModel model = Models.MODELAUTOVALUE;
        Configuration configuration = new ConfigurationBuilder(dataSource, model)
            .useDefaultLogging()
            .setEntityCache(new EntityCacheBuilder(model)
                .useReferenceCache(true)
                .build())
            .build();
        data = new QueryableStore<>(configuration);
        SchemaModifier tables = new SchemaModifier(configuration);
        tables.dropTables();
        TableCreationMode mode = TableCreationMode.CREATE_NOT_EXISTS;
        System.out.println(tables.createTablesString(mode));
        tables.createTables(mode);
    }

    @After
    public void teardown() {
        if (data != null) {
            data.close();
        }
    }

    @Test
    public void testSelectAll() {
        List<Integer> added = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            try {
                added.add( randomPerson() );
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }
        }
        Result<Person> result = data.select(Person.class).get();
        for (Person p : result) {
            assertTrue(added.contains(p.getId()));
        }
    }
}
