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
import io.requery.query.Tuple;
import io.requery.sql.Configuration;
import io.requery.sql.ConfigurationBuilder;
import io.requery.sql.EntityDataStore;
import io.requery.sql.SchemaModifier;
import io.requery.sql.TableCreationMode;
import io.requery.sql.platform.SQLite;
import io.requery.test.autovalue.Models;
import io.requery.test.autovalue.Person;
import io.requery.test.autovalue.PersonType;
import io.requery.test.autovalue.Phone;
import io.requery.test.autovalue.PhoneType;
import io.requery.util.function.Consumer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.sql.CommonDataSource;
import java.net.MalformedURLException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class AutoValueModelTest {

    protected EntityDataStore<Object> data;

    int randomPerson() {
        Random random = new Random();
        String[] firstNames = new String[]{"Alice", "Bob", "Carol"};
        String[] lastNames = new String[]{"Smith", "Lee", "Jones"};
        String name = firstNames[random.nextInt(firstNames.length)] + " " +
            lastNames[random.nextInt(lastNames.length)];

        Calendar calendar = Calendar.getInstance();
        //noinspection MagicConstant
        calendar.set(1900 + random.nextInt(90), random.nextInt(12), random.nextInt(30));

        return data.insert(Person.class)
            .value(PersonType.NAME, name)
            .value(PersonType.AGE, random.nextInt(50))
            .value(PersonType.EMAIL, name.replaceAll(" ", "").toLowerCase() + "@example.com")
            .value(PersonType.UUID, UUID.randomUUID())
            .value(PersonType.BIRTHDAY, calendar.getTime())
            .value(PersonType.ABOUT, "About this person")
            .get().first().get(PersonType.ID);
    }

    @Before
    public void setup() throws SQLException {
        CommonDataSource dataSource = DatabaseType.getDataSource(new SQLite());
        EntityModel model = Models.AUTOVALUE;
        Configuration configuration = new ConfigurationBuilder(dataSource, model)
            .useDefaultLogging()
            .setEntityCache(new EntityCacheBuilder(model)
                .useReferenceCache(true)
                .build())
            .build();
        data = new EntityDataStore<>(configuration);
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

            added.add( randomPerson() );
        }
        Result<Person> result = data.select(Person.class).get();
        for (Person p : result) {
            assertTrue(added.contains(p.getId()));
        }
    }

    @Test
    public void testInsert() {
        Set<Integer> ids = new HashSet<>();
        for (int i = 0; i < 10; i++) {
            Person p = Person.builder()
                .setName("person" + i)
                .setAge(30)
                .setEmail("test@example.com")
                .setUUID(UUID.randomUUID())
                .setBirthday(new Date())
                .setAbout("About me")
                .build();
            Integer key = data.insert(p, Integer.class);
            assertTrue(key > 0);
            ids.add(key);
        }
        final Set<Integer> selected = new HashSet<>();
        data.select(PersonType.ID).get().each(new Consumer<Tuple>() {
            @Override
            public void accept(Tuple tuple) {
                selected.add(tuple.get(PersonType.ID));
            }
        });
        assertEquals(ids, selected);
    }

    @Test
    public void testUpdate() {
        Person person = Person.builder()
            .setName("Bob")
            .setAge(30)
            .setEmail("test@example.com")
            .setUUID(UUID.randomUUID())
            .setBirthday(new Date())
            .setAbout("About me")
            .build();
        Integer key = data.insert(person, Integer.class);

        Person renamed = person.toBuilder().setId(key).setName("Bobby").build();
        data.update(renamed);

        person = data.findByKey(Person.class, key);
        assertTrue(person.getName().equals("Bobby"));
    }

    @Test
    public void testDelete() throws MalformedURLException {
        Integer key = randomPerson();
        Person p = data.findByKey(Person.class, key);
        assertNotNull(p);
        data.delete(p);
        p = data.findByKey(Person.class, key);
        assertNull(p);
    }

    @Test
    public void testRefresh() throws MalformedURLException {
        Integer key = randomPerson();
        Integer count = data.update(Person.class).set(PersonType.NAME, "Unknown").get().value();
        assertTrue(count > 0);
        Person p = data.findByKey(Person.class, key);
        data.refresh(p);
        assertEquals(p.getName(), "Unknown");
    }

    @Test
    public void testInsertReference() {
        randomPerson();
        Result<Person> result = data.select(Person.class).get();
        Person person = result.first();
        int id = data.insert(Phone.class)
            .value(PhoneType.PHONE_NUMBER, "5555555")
            .value(PhoneType.NORMALIZED, false)
            .value(PhoneType.OWNER_ID, person.getId())
            .get().first().get(PhoneType.ID);
        assertTrue(id != 0);

        Phone phone = data.select(Phone.class).get().first();
        assertSame(phone.getOwnerId(), person.getId());
    }
}
