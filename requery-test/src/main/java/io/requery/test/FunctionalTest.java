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

import io.requery.Persistable;
import io.requery.PersistenceException;
import io.requery.Transaction;
import io.requery.meta.Attribute;
import io.requery.proxy.CompositeKey;
import io.requery.proxy.EntityProxy;
import io.requery.proxy.PropertyState;
import io.requery.query.NamedExpression;
import io.requery.query.Result;
import io.requery.query.Return;
import io.requery.query.Tuple;
import io.requery.query.function.Case;
import io.requery.query.function.Coalesce;
import io.requery.query.function.Count;
import io.requery.sql.EntityDataStore;
import io.requery.test.model.Address;
import io.requery.test.model.Group;
import io.requery.test.model.GroupType;
import io.requery.test.model.Group_Person;
import io.requery.test.model.Person;
import io.requery.test.model.Phone;
import io.requery.util.function.Consumer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * The idea here is to create a set of entity models using as many different features of the
 * library as possible. In a way that an actual user of the API might do so and then use the API
 * with those models. It's a more 'real world' test.
 *
 * This class is located outside the test package so it can be reused in the Android tests.
 *
 * @author Nikhil Purushe
 */
public abstract class FunctionalTest extends RandomData {

    protected EntityDataStore<Persistable> data;

    @Before
    public abstract void setup() throws SQLException;

    @After
    public void teardown() {
        if (data != null) {
            data.close();
        }
    }

    @Test
    public void testEqualsHashCode() {
        Person p1 = new Person();
        p1.setAge(10);
        p1.setName("Bob");
        p1.setEmail("test@test.com");
        Person p2 = new Person();
        p2.setAge(10);
        p2.setName("Bob");
        p2.setEmail("test@test.com");
        assertEquals(p1, p2);
        assertEquals(p1.hashCode(), p2.hashCode());
    }

    @Test
    public void testInsert() {
        Person person = randomPerson();
        data.insert(person);
        assertTrue(person.getId() > 0);
        Person cached = data.select(Person.class)
                .where(Person.ID.equal(person.getId())).get().first();
        assertSame(cached, person);
    }

    @Test
    public void testInsertWithTransaction() {
        data.runInTransaction(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                for (int i = 0; i < 10; i++) {
                    Person person = randomPerson();
                    data.insert(person);
                    assertTrue(person.getId() > 0);
                }
                return null;
            }
        });
    }

    @Test
    public void testInsertBatch() {
        List<Person> persons = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            Person person = randomPerson();
            persons.add(person);
        }
        data.insert(persons);
        for (Person person : persons) {
            assertTrue(person.getId() != 0);
        }
        int people = 0;
        for (Person ignored : data.select(Person.class).get()) {
            people++;
        }
        assertEquals(100, people);
    }

    @Test
    public void testInsertConcurrent() throws InterruptedException {
        ExecutorService executorService = Executors.newFixedThreadPool(3);
        final int count = 5;
        final CountDownLatch latch = new CountDownLatch(count);
        final Map<Integer, Person> map = new HashMap<>();
        for (int i = 0; i < count; i++) {
            executorService.submit(new Callable<Person>() {
                @Override
                public Person call() throws Exception {
                    Person person = randomPerson();
                    data.insert(person);
                    assertTrue(person.getId() > 0);
                    map.put(person.getId(), person);
                    latch.countDown();
                    return person;
                }
            });
        }
        assertTrue(latch.await(10, TimeUnit.SECONDS));
        for (Map.Entry<Integer, Person> entry : map.entrySet()) {
            Person cached = data.select(Person.class)
                .where(Person.ID.equal(entry.getKey())).get().first();
            assertSame(cached, entry.getValue());
        }
        executorService.shutdownNow();
    }

    @Test
    public void testInsertEmptyObject() {
        Phone phone = new Phone();
        data.insert(phone);
        assertTrue(phone.getId() > 0);
    }

    @Test
    public void testInsertDerivedObject() {
        class Phone2 extends Phone {
            @Override
            public String toString() {
                return "phone2";
            }
        }
        Phone2 phone2 = new Phone2();
        phone2.setPhoneNumber("55555555");
        data.insert(phone2);
    }

    @Test
    public void testInsertQuery() {
        Integer id1 = data.insert(Person.class)
                .value(Person.ABOUT, "nothing")
                .value(Person.AGE, 50).get().first().get(0);
        assertNotNull(id1);

        Integer id2 = data.insert(Person.class)
            .value(Person.NAME, "Bob")
            .value(Person.AGE, 50).get().first().get(Person.ID);
        assertNotNull(id2);
        assertTrue(!id1.equals(id2));
    }

    @Test
    public void testInsertWithTransactionCallable() {
        assertTrue("success".equals(
                data.runInTransaction(new Callable<String>() {
                    @Override
                    public String call() {
                        for (int i = 0; i < 10; i++) {
                            Person person = randomPerson();
                            data.insert(person);
                            assertTrue(person.getId() > 0);
                        }
                        return "success";
                    }
                })));
    }

    @Test
    public void testFindByKey() {
        Person person = randomPerson();
        data.insert(person);
        assertTrue(person.getId() > 0);
        Person other = data.findByKey(Person.class, person.getId());
        assertSame(person, other);
    }

    @Test
    public void testFindByCompositeKey() {
        Group group = new Group();
        group.setName("group");
        group.setType(GroupType.PRIVATE);

        Person person = randomPerson();
        person.getGroups().add(group);
        data.insert(person);
        assertTrue(person.getId() > 0);

        // create the composite key
        Map<Attribute<Group_Person, Integer>, Integer> map = new LinkedHashMap<>();
        map.put(Group_Person.GROUPS_ID, group.getId());
        map.put(Group_Person.PERSON_ID, person.getId());

        CompositeKey<Group_Person> compositeKey = new CompositeKey<>(map);
        Group_Person joined = data.findByKey(Group_Person.class, compositeKey);
        assertNotNull(joined);
    }

    @Test
    public void testTransactionRollback() {
        ArrayList<Integer> ids = new ArrayList<>();
        try (Transaction transaction = data.transaction().begin()) {
            for (int i = 0; i < 10; i++) {
                Person person = randomPerson();
                data.insert(person);
                assertTrue(person.getId() > 0);
                ids.add(person.getId());
                if (i == 5) {
                    throw new Exception("rollback...");
                }
            }
            transaction.commit();
        } catch (Exception ignored) {

        }
        for (Integer id : ids) {
            Person p = data.select(Person.class)
                    .where(Person.ID.equal(id)).get().firstOrNull();
            assertNull(p);
        }
    }

    @Test
    public void testUpdate() {
        Person person = randomPerson();
        data.insert(person);
        assertTrue(person.getId() > 0);
        person.setName("Bob Smith");
        Calendar calendar = Calendar.getInstance();
        calendar.set(1983, Calendar.NOVEMBER, 11);
        person.setBirthday(calendar.getTime());
        EntityProxy<Person> proxy = Person.$TYPE.proxyProvider().apply(person);
        int count = 0;
        for (Attribute<Person, ?>  ignored : Person.$TYPE.attributes()) {
            if (proxy.getState(ignored) == PropertyState.MODIFIED) {
                count++;
            }
        }
        assertEquals(2, count);
        data.update(person);
        for (Attribute<Person, ?> ignored : Person.$TYPE.attributes()) {
            if (proxy.getState(ignored) == PropertyState.MODIFIED) {
                fail();
            }
        }
    }

    @Test
    public void testUpdateNoChanges() {
        Person person = randomPerson();
        data.insert(person);
        assertTrue(person.getId() > 0);
        data.update(person);
    }

    @Test
    public void testEntityListeners() {
        Person person = randomPerson();
        data.insert(person);
        assertTrue(person.getPreviousState() == EntityState.PRE_SAVE);
        assertTrue(person.getCurrentState() == EntityState.POST_SAVE);
        person.setEmail("newemail@something.com");
        data.update(person);
        assertTrue(person.getPreviousState() == EntityState.PRE_UPDATE);
        assertTrue(person.getCurrentState() == EntityState.POST_UPDATE);
        data.delete(person);
        assertTrue(person.getPreviousState() == EntityState.PRE_DELETE);
        assertTrue(person.getCurrentState() == EntityState.POST_DELETE);
    }

    @Test
    public void testGetNullAssociation() {
        Person person = randomPerson();
        data.insert(person);
        assertNull(person.getAddress());
    }

    @Test
    public void testGetNullAssociationInverse() {
        Address address = randomAddress();
        data.insert(address);
        assertNull(address.getPerson());
    }

    @Test
    public void testInsertOneToOne() {
        Address address = randomAddress();
        data.insert(address);
        assertTrue(address.getId() > 0);
        Person person = randomPerson();
        data.insert(person);
        person.setAddress(address);
        data.update(person);
        // fetch inverse
        assertSame(address.getPerson(), person);
        // unset
        person.setAddress(null);
    }

    @Test
    public void testInsertOneToOneCascade() {
        Address address = randomAddress();
        Person person = randomPerson();
        person.setAddress(address);
        data.insert(person);
        // fetch inverse
        assertSame(address.getPerson(), person);
    }

    @Test
    public void testUpdateOneToOneCascade() {
        Address address = randomAddress();
        Person person = randomPerson();
        data.insert(person);
        person.setAddress(address);
        data.update(person);
        assertSame(address.getPerson(), person);
    }

    @Test
    public void testRefreshAll() {
        Person person = randomPerson();
        data.insert(person);
        Phone phone = randomPhone();
        person.getPhoneNumbers().add(phone);
        data.update(person);
        data.refreshAll(person);
        assertTrue(person.getPhoneNumbersSet().contains(phone));
    }

    @Test
    public void testRefreshMultiple() {
        List<Person> people = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            Person person = randomPerson();
            people.add(person);
            data.insert(person);
        }
        int count = data.update(Person.class).set(Person.NAME, "fff").get().value();
        assertEquals(10, count);
        data.refresh(people);
        data.refresh(people, Person.NAME);
        for (Person p : people) {
            assertEquals("fff", p.getName());
        }
    }

    @Test
    public void testRefreshAttributes() {
        Person person = randomPerson();
        data.insert(person);
        Phone phone = randomPhone();
        person.getPhoneNumbers().add(phone);
        data.update(person);
        data.refresh(person,
            Person.NAME,
            Person.PHONE_NUMBERS_SET,
            Person.ADDRESS,
            Person.EMAIL);
        assertTrue(person.getPhoneNumbersSet().contains(phone));
    }

    @Test
    public void testVersionIncrement() {
        Group group = new Group();
        group.setName("group");
        group.setType(GroupType.PRIVATE);
        data.insert(group);
        group.setType(GroupType.PUBLIC);
        data.update(group);
        data.refresh(group, Group.VERSION);
        //System.out.println("group.version + " + group.version());
        assertTrue(group.getVersion() > 0);

        Group group2 = new Group();
        group2.setName("group2");
        group2.setType(GroupType.PRIVATE);
        data.insert(group2);
        data.refresh(Arrays.asList(group, group2), Group.VERSION);
    }

    @Test
    public void testFillResult() {
        data.delete(Person.class).get();
        Person person = randomPerson();
        data.insert(person);
        assertEquals(1, data.select(Person.class)
                .get().collect(new HashSet<Person>()).size());
    }

    @Test
    public void testListResult() {
        data.delete(Person.class).get();
        Person person = randomPerson();
        data.insert(person);
        assertEquals(1, data.select(Person.class).get().toList().size());
    }

    @Test
    public void testDeleteCascadeOneToOne() {
        Address address = randomAddress();
        data.insert(address);
        assertTrue(address.getId() > 0);
        Person person = randomPerson();
        data.insert(person);
        data.delete(person);
        assertNull(address.getPerson());
    }

    @Test
    public void testDeleteOne() {
        Person person = randomPerson();
        data.insert(person);
        assertTrue(person.getId() > 0);
        data.delete(person);
        Person cached = data.select(Person.class)
                .where(Person.ID.equal(person.getId())).get().firstOrNull();
        assertNull(cached);
    }

    @Test
    public void testDeleteCascadeRemoveOneToMany() {
        Person person = randomPerson();
        data.insert(person);
        Phone phone1 = randomPhone();
        Phone phone2 = randomPhone();
        phone1.setOwner(person);
        phone2.setOwner(person);
        data.insert(phone1);
        data.insert(phone2);
        data.refresh(person);
        List<Phone> list = person.getPhoneNumbersList();
        phone1.getOwner();
        assertTrue(list.contains(phone1));
        data.delete(phone1);
        assertFalse(list.contains(phone1));
    }

    @Test
    public void testDeleteCascadeOneToMany() {
        Person person = randomPerson();
        data.insert(person);
        Phone phone1 = randomPhone();
        phone1.setOwner(person);
        data.insert(phone1);
        int phoneId = phone1.getId();
        person.getPhoneNumbers();
        data.delete(person);
        Phone phone = data.select(Phone.class)
                .where(Phone.ID.equal(phoneId)).get().firstOrNull();
        assertNull(phone);
    }

    @Test
    public void testInsertOneToMany() {
        Person person = randomPerson();
        data.insert(person);
        Phone phone1 = randomPhone();
        Phone phone2 = randomPhone();
        phone1.setOwner(person);
        phone2.setOwner(person);
        data.insert(phone1);
        data.insert(phone2);
        HashSet<Phone> set = new HashSet<>(person.getPhoneNumbers().toList());
        assertEquals(2, set.size());
        assertTrue(set.containsAll(Arrays.asList(phone1, phone2)));
    }

    @Test
    public void testInsertOneToManyInverse() {
        Person person = randomPerson();
        data.insert(person);
        Phone phone1 = randomPhone();
        Phone phone2 = randomPhone();
        person.getPhoneNumbers().add(phone1);
        person.getPhoneNumbers().add(phone2);
        data.update(person);
        HashSet<Phone> set = new HashSet<>(person.getPhoneNumbers().toList());
        assertEquals(2, set.size());
        assertTrue(set.containsAll(Arrays.asList(phone1, phone2)));
    }

    @Test
    public void testInsertOneToManyInverseThroughSet() {
        Person person = randomPerson();
        data.insert(person);
        Phone phone1 = randomPhone();
        Phone phone2 = randomPhone();
        person.getPhoneNumbers().add(phone1);
        person.getPhoneNumbers().add(phone2);
        data.update(person);
        assertEquals(2, person.getPhoneNumbersSet().size());
        assertTrue(person.getPhoneNumbersSet().containsAll(Arrays.asList(phone1, phone2)));
    }

    @Test
    public void testInsertOneToManyOneInsert() {
        Person person = randomPerson();
        Phone phone1 = randomPhone();
        Phone phone2 = randomPhone();
        person.getPhoneNumbers().add(phone1);
        person.getPhoneNumbers().add(phone2);
        data.insert(person);
        data.refresh(person, Person.PHONE_NUMBERS);
        HashSet<Phone> set = new HashSet<>(person.getPhoneNumbers().toList());
        assertEquals(2, set.size());
        assertTrue(set.containsAll(Arrays.asList(phone1, phone2)));
    }

    @Test
    public void testManyToOneRefresh() {
        Person person = randomPerson();
        Phone phone1 = randomPhone();
        Phone phone2 = randomPhone();
        person.getPhoneNumbers().add(phone1);
        person.getPhoneNumbers().add(phone2);
        data.insert(person);
        assertSame(person, phone1.getOwner());
        assertSame(person, phone2.getOwner());
        data.refresh(phone1, Phone.OWNER);
        data.refresh(phone2, Phone.OWNER);
    }

    @Test
    public void testInsertManyToMany() {
        Person person = randomPerson();
        data.insert(person);
        assertTrue(person.getGroups().toList().isEmpty());
        List<Group> added = new ArrayList<>();
        try (Transaction transaction = data.transaction().begin()) {
            for (int i = 0; i < 10; i++) {
                Group group = new Group();
                group.setName("Group" + i);
                group.setDescription("Some description");
                group.setType(GroupType.PRIVATE);
                data.insert(group);
                person.getGroups().add(group);
                added.add(group);
            }
            data.update(person);
            transaction.commit();
        }
        data.refresh(person, Person.GROUPS);
        assertTrue(added.containsAll(person.getGroups().toList()));
        for (Group group : added) {
            assertTrue(group.getMembers().toList().contains(person));
        }
    }

    @Test
    public void testIterateInsertMany() {
        Person person = randomPerson();
        assertTrue(person.getGroups().toList().isEmpty());
        HashSet<Group> toAdd = new HashSet<>();
        for (int i = 0; i < 10; i++) {
            Group group = new Group();
            group.setName("Group" + i);
            person.getGroups().add(group);
            toAdd.add(group);
        }
        int count = 0;
        for (Group g : person.getGroups()) {
            assertTrue(toAdd.contains(g));
            count++;
        }
        assertEquals(10, count);
        data.insert(person);
    }

    @Test
    public void testDeleteManyToMany() {
        final Person person = randomPerson();
        data.insert(person);
        final Collection<Group> groups = new ArrayList<>();
        data.runInTransaction(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                for (int i = 0; i < 10; i++) {
                    Group group = new Group();
                    group.setName("DeleteGroup" + i);
                    data.insert(group);
                    person.getGroups().add(group);
                    groups.add(group);
                }
                data.update(person);
                return null;
            }
        });
        for (Group g : groups) {
            person.getGroups().remove(g);
        }
        data.update(person);
    }

    @Test
    public void testSingleQueryWhere() {
        final String name = "duplicateFirstName";
        data.delete(Person.class).where(Person.NAME.equal(name)).get();
        for (int i = 0; i < 10; i++) {
            Person person = randomPerson();
            person.setName(name);
            data.insert(person);
        }
        try (Result<Person> query = data.select(Person.class)
                .where(Person.NAME.equal(name)).get()) {
            assertEquals(10, query.toList().size());
        }
    }

    @Test
    public void testSingleQueryExecute() {
        data.insert(randomPersons(10));
        Result<Person> result = data.select(Person.class).get();
        assertEquals(10, result.toList().size());
        Person person = randomPerson();
        data.insert(person);
        assertEquals(11, result.toList().size());
    }

    @Test
    public void testSingleQueryLimitSkip() {
        final String name = "duplicateFirstName";
        data.delete(Person.class).where(Person.NAME.equal(name)).get();
        for (int i = 0; i < 10; i++) {
            Person person = randomPerson();
            person.setName(name);
            data.insert(person);
        }
        for (int i = 0; i < 3; i++) {
            try (Result<Person> query = data.select(Person.class)
                    .where(Person.NAME.equal(name))
                    .limit(5).get()) {
                assertEquals(5, query.toList().size());
            }
            try (Result<Person> query = data.select(Person.class)
                    .where(Person.NAME.equal(name)).limit(5).offset(5).get()) {
                assertEquals(5, query.toList().size());
            }
        }
    }

    @Test
    public void testSingleQueryWhereNull() {
        data.delete(Person.class).where(Person.NAME.isNull()).get();
        Person person = randomPerson();
        person.setName(null);
        data.insert(person);
        try (Result<Person> query = data.select(Person.class)
                .where(Person.NAME.equal(null)).get()) {
            assertEquals(1, query.toList().size());
        }
        try (Result<Person> query = data.select(Person.class)
                .where(Person.NAME.isNull()).get()) {
            assertEquals(1, query.toList().size());
        }
    }

    @Test
    public void testDeleteAll() {
        final String name = "someName";
        for (int i = 0; i < 10; i++) {
            Person person = randomPerson();
            person.setName(name);
            data.insert(person);
        }
        assertTrue(data.delete(Person.class).get().value() > 0);
        assertTrue(data.select(Person.class).get().toList().isEmpty());
    }

    @Test
    public void testDeleteBatch() {
        List<Person> persons = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            Person person = randomPerson();
            persons.add(person);
        }
        data.insert(persons);
        assertEquals(100, data.count(Person.class).get().value().intValue());
        data.delete(persons);
        assertEquals(0, data.count(Person.class).get().value().intValue());
    }

    @Test
    public void testQuerySelectDistinct() {
        for (int i = 0; i < 10; i++) {
            Person person = randomPerson();
            person.setName(String.valueOf(i / 2));
            data.insert(person);
        }
        try (Result<Tuple> result = data.select(Person.NAME).distinct().get()) {
            assertEquals(5, result.toList().size());
        }
    }

    @Test
    public void testQuerySelectCount() {
        data.insert(randomPersons(10));
        try (Result<Tuple> result = data.select(Count.count(Person.class).as("bb")).get()) {
            assertTrue(result.first().get("bb").equals(10));
        }
        try (Result<Tuple> result = data.select(Count.count(Person.class)).get()) {
            assertTrue(result.first().get(0).equals(10));
        }
        assertEquals(10, data.count(Person.class).get().value().intValue());

        data.count(Person.class).get().consume(new Consumer<Integer>() {
            @Override
            public void accept(Integer integer) {
                assertTrue(integer == 10);
            }
        });
    }

    @Test
    public void testQuerySelectCountWhere() {
        for (int i = 0; i < 10; i++) {
            Person person = randomPerson();
            if (i == 0) {
                person.setName("countme");
            }
            data.insert(person);
        }
        assertEquals(1,
                data.count(Person.class).where(Person.NAME.eq("countme")).get().value().intValue());
    }

    @Test
    public void testQueryNotNull() {
        for (int i = 0; i < 10; i++) {
            Person person = randomPerson();
            data.insert(person);
        }
        assertNotNull(data.select(Person.class)
            .where(Person.NAME.notNull()).get().first());
    }

    @Test
    public void testQueryFromSub() {
        for (int i = 0; i < 10; i++) {
            Person person = randomPerson();
            person.setAge(i+1);
            data.insert(person);
        }
        Integer result = data.select(NamedExpression.ofInteger("avg_age").avg())
            .from(data.select(Person.AGE.sum().as("avg_age"))
                .groupBy(Person.AGE).as("t1")).get().first().get(0);
        assertTrue(result >= 5); // derby rounds up
    }

    @SuppressWarnings("MagicConstant")
    @Test
    public void testQuerySelectMin() {
        for (int i = 0; i < 10; i++) {
            Person person = randomPerson();
            if (i == 9) {
                Calendar calendar = Calendar.getInstance();
                calendar.set(1800, Calendar.NOVEMBER, 11, 0, 0, 0);
                person.setBirthday(calendar.getTime());
            }
            data.insert(person);
        }
        try (Result<Tuple> query = data.select(Person.BIRTHDAY.min().as("oldestBday")).get()) {
            Date date = query.first().get("oldestBday");
            Calendar c = Calendar.getInstance();
            c.setTime(date);
            assertTrue(c.get(Calendar.YEAR) == 1800); // just check the year...
        }
    }

    @Test
    public void testQuerySelectTrim() {
        // TODO fix use ltrim/rtrim for SQLServer
        Person person = randomPerson();
        person.setName("  Name  ");
        data.insert(person);
        Tuple result = data.select(Person.NAME.trim().as("name")).get().first();
        String name = result.get(0);
        assertEquals(name, "Name");
    }

    @Test
    public void testQueryOrderBy() {
        for (int i = 0; i < 5; i++) {
            Person person = randomPerson();
            person.setAge(i);
            data.insert(person);
        }
        try (Result<Tuple> query = data.select(Person.AGE)
                .orderBy(Person.AGE.desc()).get()) {
            Integer i = query.first().get(0);
            assertTrue(i.equals(4));
        }
    }

    @Test
    public void testQueryGroupBy() {
        for (int i = 0; i < 5; i++) {
            Person person = randomPerson();
            person.setAge(i);
            data.insert(person);
        }
        try (Result<Tuple> result = data.select(Person.AGE)
                .groupBy(Person.AGE)
                .having(Person.AGE.greaterThan(3)).get()) {
            assertTrue(result.toList().size() == 1);
        }
        assertTrue(data.select(Person.AGE)
            .groupBy(Person.AGE)
            .having(Person.AGE.lessThan(0)).get().toList().isEmpty());
    }

    @Test
    public void testQuerySelectWhereIn() {
        final String name = "Hello!";
        Person person = randomPerson();
        person.setName(name);
        data.insert(person);
        Group group = new Group();
        group.setName("Hello!");
        data.insert(group);
        person.getGroups().add(group);
        data.update(person);
        Return<Result<Tuple>> groupNames = data.select(Group.NAME)
                .where(Group.NAME.equal(name));
        Person p = data.select(Person.class).where(Person.NAME.in(groupNames)).get().first();
        assertEquals(p.getName(), name);
        p = data.select(Person.class).where(Person.NAME.notIn(groupNames)).get().firstOrNull();
        assertNull(p);
        p = data.select(Person.class)
            .where(Person.NAME.in(Arrays.asList("Hello!", "Other"))).get().first();
        assertEquals(p.getName(), name);
        p = data.select(Person.class)
            .where(Person.NAME.in(Collections.singleton("Hello!"))).get().first();
        assertEquals(p.getName(), name);
        assertTrue(data.select(Person.class)
            .where(Person.NAME.notIn(Collections.singleton("Hello!")))
            .get().toList().isEmpty());
    }

    @Test
    public void testQueryBetween() {
        Person person = randomPerson();
        person.setAge(75);
        data.insert(person);
        Person p = data.select(Person.class).where(Person.AGE.between(50, 100)).get().first();
        assertTrue(p == person);
    }

    @Test
    public void testQueryConditions() {
        Person person = randomPerson();
        person.setAge(75);
        data.insert(person);
        Person p = data.select(Person.class).where(Person.AGE.greaterThanOrEqual(75)).get().first();
        assertSame(p, person);
        p = data.select(Person.class).where(Person.AGE.lessThanOrEqual(75)).get().first();
        assertSame(p, person);
        p = data.select(Person.class).where(Person.AGE.greaterThan(75)).get().firstOrNull();
        assertNull(p);
        p = data.select(Person.class).where(Person.AGE.notEqual(75)).get().firstOrNull();
        assertNull(p);

        p = data.select(Person.class).where(Person.AGE.gte(75)).get().first();
        assertSame(p, person);
        assertSame(p, person);
        p = data.select(Person.class).where(Person.AGE.lte(75)).get().first();
        assertSame(p, person);
        p = data.select(Person.class).where(Person.AGE.gt(75)).get().firstOrNull();
        assertNull(p);
        p = data.select(Person.class).where(Person.AGE.ne(75)).get().firstOrNull();
        assertNull(p);
    }

    @Test
    public void testQueryCompoundConditions() {
        Person person = randomPerson();
        person.setAge(75);
        data.insert(person);
        Person person2 = randomPerson();
        person2.setAge(10);
        person2.setName("Carol");
        data.insert(person2);
        Person person3 = randomPerson();
        person3.setAge(0);
        person3.setName("Bob");
        data.insert(person3);
        List<Person> result = data.select(Person.class)
            .where( Person.AGE.gt(5).and(Person.AGE.lt(75)).and(Person.NAME.ne("Bob")) )
            .or(Person.NAME.eq("Bob"))
            .get().toList();
        assertTrue(result.contains(person2));
        assertTrue(result.contains(person3));

        result = data.select(Person.class)
            .where( Person.AGE.gt(10).or(Person.AGE.eq(75)) )
            .and(Person.NAME.eq("Bob"))
            .get().toList();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testQueryConsume() {
        int count = 10;
        for (int i = 0; i < count; i++) {
            Person person = randomPerson();
            data.insert(person);
        }
        final int[] counts = new int[]{0};
        Result<Person> result = data.select(Person.class).get();
        result.each(new Consumer<Person>() {
            @Override
            public void accept(Person e) {
                counts[0] = counts[0] + 1;
            }
        });
        assertEquals(counts[0], count);
    }

    @Test
    public void testQueryMap() {
        int count = 10;
        for (int i = 0; i < count; i++) {
            Person person = randomPerson();
            if (i == 0) {
                person.setEmail("one@test.com");
            }
            data.insert(person);
        }
        Result<Person> result = data.select(Person.class).get();
        Map<String, Person> map = result.toMap(Person.EMAIL,
                new ConcurrentHashMap<String, Person>());
        assertNotNull(map.get("one@test.com"));
        map = result.toMap(Person.EMAIL);
        assertNotNull(map.get("one@test.com"));
    }

    @Test
    public void testQueryUpdate() {
        Person person = randomPerson();
        person.setAge(100);
        data.insert(person);
        int rowCount = data.update(Person.class)
                .set(Person.ABOUT, "nothing")
                .set(Person.AGE, 50)
                .where(Person.AGE.equal(100)).get().value();
        assertEquals(1, rowCount);
    }

    @Test
    public void testQueryCoalesce() {
        Person person = randomPerson();
        person.setName("Carol");
        person.setEmail(null);
        data.insert(person);
        person = randomPerson();
        person.setName("Bob");
        person.setEmail("test@test.com");
        person.setHomepage(null);
        data.insert(person);
        Result<Tuple> result = data.select
            (Coalesce.coalesce(Person.EMAIL, Person.NAME)).get();
        List<Tuple> list = result.toList();
        List<String> values = new ArrayList<>();
        for (Tuple tuple : list) {
            values.add(tuple.get(0).toString());
        }
        assertEquals(values.size(), 2);
        assertTrue(values.contains("Carol"));
        assertTrue(values.contains("test@test.com"));
    }

    @Test
    public void testQueryLike() {
        Person person = randomPerson();
        person.setName("Carol");
        data.insert(person);
        person = randomPerson();
        person.setName("Bob");
        data.insert(person);
        Person a = data.select(Person.class)
                .where(Person.NAME.like("B%"))
                .get().first();
        assertSame(a, person);
        a = data.select(Person.class)
            .where(Person.NAME.lower().like("b%"))
            .get().first();
        assertSame(a, person);
        Person b = data.select(Person.class)
                .where(Person.NAME.notLike("B%"))
                .get().firstOrNull();
        assertTrue(b != person);
    }

    @Test
    public void testQueryCase() {
        String[] names = new String[]{"Carol", "Bob", "Jack"};
        for (String name : names) {
            Person person = randomPerson();
            person.setName(name);
            data.insert(person);
        }
        Result<Tuple> a = data.select(Person.NAME,
            Case.type(String.class)
                .when(Person.NAME.equal("Bob"), "B")
                .when(Person.NAME.equal("Carol"), "C")
                .elseThen("Unknown"))
            .from(Person.class)
            .orderBy(Person.NAME)
            .get();
        List<Tuple> list = a.toList();
        assertTrue(list.get(0).get(1).equals("B"));
        assertTrue(list.get(1).get(1).equals("C"));
        assertTrue(list.get(2).get(1).equals("Unknown"));

        a = data.select(Person.NAME,
            Case.type(Integer.class)
                .when(Person.NAME.equal("Bob"), 1)
                .when(Person.NAME.equal("Carol"), 2)
                .elseThen(0))
            .orderBy(Person.NAME)
            .get();
        list = a.toList();
        assertTrue(list.get(0).get(1).equals(1));
        assertTrue(list.get(1).get(1).equals(2));
        assertTrue(list.get(2).get(1).equals(0));
    }

    @Test
    public void testQueryUnion() {
        Person person = randomPerson();
        person.setName("Carol");
        data.insert(person);
        Group group = new Group();
        group.setName("Hello!");
        data.insert(group);
        List<Tuple> result = data.select(Person.NAME.as("name"))
                .union()
                .select(Group.NAME.as("name"))
                .orderBy(Group.NAME.as("name")).get().toList();
        assertTrue(result.size() == 2);
        assertTrue(result.get(0).get(0).equals("Carol"));
        assertTrue(result.get(1).get(0).equals("Hello!"));
    }

    @Test(expected = PersistenceException.class)
    public void testViolateUniqueConstraint() {
        UUID uuid = UUID.randomUUID();
        Person p1 = randomPerson();
        p1.setUUID(uuid);
        data.insert(p1);
        Person p2 = randomPerson();
        p2.setUUID(uuid);
        data.insert(p2);
        fail();
    }
}
