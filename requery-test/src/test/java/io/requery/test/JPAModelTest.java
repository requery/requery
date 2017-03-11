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
import io.requery.sql.EntityDataStore;
import io.requery.sql.SchemaModifier;
import io.requery.sql.TableCreationMode;
import io.requery.sql.platform.H2;
import io.requery.test.jpa.AddressEntity;
import io.requery.test.jpa.AddressType;
import io.requery.test.jpa.Group;
import io.requery.test.jpa.GroupEntity;
import io.requery.test.jpa.GroupType;
import io.requery.test.jpa.Models;
import io.requery.test.jpa.Person;
import io.requery.test.jpa.PersonEntity;
import io.requery.test.jpa.PhoneEntity;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.cache.CacheManager;
import javax.cache.Caching;
import javax.cache.spi.CachingProvider;
import javax.sql.CommonDataSource;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class JPAModelTest {

    protected EntityDataStore<Serializable> data;

    private static PersonEntity randomPerson() {
        Random random = new Random();
        PersonEntity person = new PersonEntity();
        String[] firstNames = new String[]{"Alice", "Bob", "Carol"};
        String[] lastNames = new String[]{"Smith", "Lee", "Jones"};
        person.setName(firstNames[random.nextInt(firstNames.length)] + " " +
            lastNames[random.nextInt(lastNames.length)]);
        person.setEmail(person.getName().replaceAll(" ", "").toLowerCase() + "@example.com");
        person.setUUID(UUID.randomUUID());
        try {
            person.setHomepage(new URL("http://www.google.com"));
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
        Calendar calendar = Calendar.getInstance();
        //noinspection MagicConstant
        calendar.set(1900 + random.nextInt(90), random.nextInt(12), random.nextInt(30));
        person.setBirthday(calendar.getTime());
        return person;
    }

    @Before
    public void setup() throws SQLException {
        CommonDataSource dataSource = DatabaseType.getDataSource(new H2());
        EntityModel model = Models.JPA;

        CachingProvider provider = Caching.getCachingProvider();
        CacheManager cacheManager = provider.getCacheManager();
        Configuration configuration = new ConfigurationBuilder(dataSource, model)
            .useDefaultLogging()
            .setEntityCache(new EntityCacheBuilder(model)
                .useReferenceCache(true)
                .useSerializableCache(true)
                .useCacheManager(cacheManager)
                .build())
            .build();
        data = new EntityDataStore<>(configuration);
        SchemaModifier tables = new SchemaModifier(configuration);
        tables.dropTables();
        TableCreationMode mode = TableCreationMode.CREATE;
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
    public void testInsertManyToMany() {
        PersonEntity person = randomPerson();
        data.insert(person);
        assertTrue(person.getGroups().toList().isEmpty());
        List<Group> added = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            GroupEntity group = new GroupEntity();
            group.setName("Group" + i);
            group.setDescription("Some description");
            group.setType(GroupType.PRIVATE);
            data.insert(group);
            person.getGroups().add(group);
            added.add(group);
        }
        data.update(person);
        data.refresh(person, PersonEntity.GROUPS);
        assertTrue(added.containsAll(person.getGroups().toList()));
    }

    @Test
    public void testDeleteManyToMany() {
        final PersonEntity person = randomPerson();
        data.insert(person);
        final Collection<Group> groups = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            GroupEntity group = new GroupEntity();
            group.setName("DeleteGroup" + i);
            data.insert(group);
            person.getGroups().add(group);
            groups.add(group);
        }
        data.update(person);
        for (Group g : groups) {
            person.getGroups().remove(g);
        }
        data.update(person);
    }

    @Test
    public void testInsertOneToMany() {
        PersonEntity person = randomPerson();
        PhoneEntity phone = new PhoneEntity();
        phone.setPhoneNumber("+1800123456");
        phone.setOwner(person);
        person.getPhoneNumbers().add(phone);
        data.insert(person);
    }

    @Test
    public void testSingleQueryExecute() {
        data.insert(randomPerson());
        Result<Person> result = data.select(Person.class).get();
        assertEquals(1, result.toList().size());
        PersonEntity person = randomPerson();
        data.insert(person);
        assertEquals(2, result.toList().size());
    }

    @Test
    public void testGroupWithOptionalDescription() {
        GroupEntity group = new GroupEntity();
        group.setName("group1");
        assertNotNull(group.getDescription());
        assertFalse(group.getDescription().isPresent());
        group.setDescription("text");
        data.insert(group);
        assertEquals("text", group.getDescription().get());
    }

    @Test
    public void testInsertEmbedded() {
        PersonEntity person = randomPerson();
        AddressEntity address = new AddressEntity();
        address.setLine1("Market St");
        address.setCity("San Francisco");
        address.setState("California");
        address.getCoordinate().setLatitude(37.7749f);
        address.getCoordinate().setLongitude(122.4194f);
        address.setType(AddressType.HOME);
        person.setAddress(address);
        data.insert(person);
    }
}
