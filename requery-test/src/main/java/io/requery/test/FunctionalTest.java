/*
 * Copyright 2017 requery.io
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
import io.requery.RollbackException;
import io.requery.Transaction;
import io.requery.TransactionIsolation;
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
import io.requery.query.function.Now;
import io.requery.query.function.Random;
import io.requery.query.function.Upper;
import io.requery.sql.EntityDataStore;
import io.requery.sql.RowCountException;
import io.requery.sql.StatementExecutionException;
import io.requery.test.model.Address;
import io.requery.test.model.Child;
import io.requery.test.model.ChildManyToManyNoCascade;
import io.requery.test.model.ChildManyToOneNoCascade;
import io.requery.test.model.ChildOneToManyNoCascade;
import io.requery.test.model.ChildOneToOneNoCascade;
import io.requery.test.model.Group;
import io.requery.test.model.GroupType;
import io.requery.test.model.Group_Person;
import io.requery.test.model.Parent;
import io.requery.test.model.ParentNoCascade;
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

import static io.requery.query.Unary.not;
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
 * <p>
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
    public void testCopy() {
        Address address = new Address();
        address.setCity("San Francisco");
        address.setState("CA");
        address.setCountry("US");
        Address copy = address.copy();
        assertEquals(address.getCity(), copy.getCity());
        assertEquals(address.getState(), copy.getState());
        assertEquals(address.getCountry(), copy.getCountry());
    }

    @Test
    public void testConverter() {
        Phone phone = randomPhone();
        phone.getExtensions().add(1);
        phone.getExtensions().add(999);
        data.insert(phone);
        Phone result = data.select(Phone.class)
            .where(Phone.EXTENSIONS.eq(phone.getExtensions())).get().first();
        assertSame(phone, result);
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
    public void testInsertDefaultValue() {
        Person person = randomPerson();
        data.insert(person);
        assertTrue(person.getId() > 0);
        assertEquals("empty", person.getDescription());
    }

    @Test
    public void testInsertSelectNullKeyReference() {
        Person person = randomPerson();
        data.insert(person);
        assertNull(person.getAddress());
        assertNull(data.select(Person.class).get().first().getAddress());
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
        final int count = 3;
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
    public void testInsertIntoSelectQuery() {
        Group group = new Group();
        group.setName("Bob");
        group.setDescription("Bob's group");
        data.insert(group);
        int count = data.insert(Person.class, Person.NAME, Person.DESCRIPTION)
            .query(data.select(Group.NAME, Group.DESCRIPTION)).get().first().count();
        assertEquals(1, count);
        Person p = data.select(Person.class).get().first();
        assertEquals("Bob", p.getName());
        assertEquals("Bob's group", p.getDescription());
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
    public void testInsertWithTransactionCallableRollback() {
        boolean rolledBack = false;
        try {
            data.runInTransaction(new Callable<String>() {
                @Override
                public String call() {
                    Person person = randomPerson();
                    data.insert(person);
                    assertTrue(person.getId() > 0);
                    throw new RuntimeException("Exception!");
                }
            }, TransactionIsolation.SERIALIZABLE);
        } catch (RollbackException e) {
            rolledBack = true;
            assertSame(0, data.select(Person.class).get().toList().size());
        }
        assertTrue(rolledBack);
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
    public void testFindByKeyDelete() {
        Person person = randomPerson();
        Address address = randomAddress();
        person.setAddress(address);
        data.insert(person);
        assertTrue(address.getId() > 0);

        Person other = data.findByKey(Person.class, person.getId());
        assertSame(person, other);
        data.delete(other);

        other = data.findByKey(Person.class, person.getId());
        assertNull(other);
        Address cached = data.findByKey(Address.class, address.getId());
        assertNull(cached);
    }

    @Test
    public void testFindByKeyDeleteInverse() {
        Person person = randomPerson();
        Address address = randomAddress();
        person.setAddress(address);
        data.insert(person);
        data.delete(address);
        person = data.findByKey(Person.class, person.getId());
        assertNull(person);
        Address cached = data.findByKey(Address.class, address.getId());
        assertNull(cached);
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
        EntityProxy<Person> proxy = Person.$TYPE.getProxyProvider().apply(person);
        int count = 0;
        for (Attribute<Person, ?> ignored : Person.$TYPE.getAttributes()) {
            if (proxy.getState(ignored) == PropertyState.MODIFIED) {
                count++;
            }
        }
        assertEquals(2, count);
        data.update(person);
        for (Attribute<Person, ?> ignored : Person.$TYPE.getAttributes()) {
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
    public void testVersionUpdate() {
        Group group = new Group();
        group.setName("Test1");
        data.insert(group);
        assertTrue(group.getVersion() > 0);
        group.setName("Test2");
        data.update(group);
        assertTrue(group.getVersion() > 0);
        group.setName("Test3");
        data.update(group);
        assertTrue(group.getVersion() > 0);
    }

    @Test
    public void testFillResult() {
        Person person = randomPerson();
        data.insert(person);
        assertEquals(1, data.select(Person.class)
            .get().collect(new HashSet<Person>()).size());
    }

    @Test
    public void testListResult() {
        Person person = randomPerson();
        data.insert(person);
        assertEquals(1, data.select(Person.class).get().toList().size());
    }

    @Test
    public void testDeleteCascadeOneToOne() {
        Address address = randomAddress();
        data.insert(address);
        int id = address.getId();
        assertTrue(id > 0);
        Person person = randomPerson();
        person.setAddress(address);
        data.insert(person);
        data.delete(person);
        assertNull(address.getPerson());
        assertNull(data.findByKey(Address.class, id));
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
    public void testDeleteOneToManyResult() {
        Person person = randomPerson();
        data.insert(person);
        Phone phone1 = randomPhone();
        Phone phone2 = randomPhone();
        phone1.setOwner(person);
        phone2.setOwner(person);
        data.insert(phone1);
        data.insert(phone2);
        data.refresh(person);
        assertEquals(2, person.getPhoneNumbers().toList().size());
        data.delete(person.getPhoneNumbers());
        Phone cached = data.findByKey(Phone.class, phone1.getId());
        assertNull(cached);
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
    public void testInsertOneToManyInverseUpdate() {
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
        assertEquals(person, phone1.getOwner());
        assertEquals(person, phone2.getOwner());
    }

    @Test
    public void testInsertOneToManyInverse() {
        Person person = randomPerson();
        Phone phone1 = randomPhone();
        Phone phone2 = randomPhone();
        phone1.setOwner(person);
        person.getPhoneNumbers().add(phone1);
        person.getPhoneNumbers().add(phone2);
        data.insert(person);
        HashSet<Phone> set = new HashSet<>(person.getPhoneNumbers().toList());
        assertEquals(2, set.size());
        assertTrue(set.containsAll(Arrays.asList(phone1, phone2)));
        assertEquals(person, phone1.getOwner());
        assertEquals(person, phone2.getOwner());
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
    public void testInsertOneToManyInsert() {
        Person person = randomPerson();
        Phone phone1 = randomPhone();
        Phone phone2 = randomPhone();
        person.getPhoneNumbers().add(phone1);
        person.getPhoneNumbers().add(phone2);
        data.insert(person);
        HashSet<Phone> set = new HashSet<>(person.getPhoneNumbers().toList());
        assertEquals(2, set.size());
        assertTrue(set.containsAll(Arrays.asList(phone1, phone2)));
        assertSame(2, data.select(Phone.class).get().toList().size());
    }

    @Test
    public void testInsertOneToManyInsertThroughList() {
        Person person = randomPerson();
        Phone phone1 = randomPhone();
        Phone phone2 = randomPhone();
        person.getPhoneNumbersList().add(phone1);
        person.getPhoneNumbersList().add(phone2);
        data.insert(person);
        HashSet<Phone> set = new HashSet<>(person.getPhoneNumbersList());
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
    public void testInsertManyToManySelfReferencing() {
        Person person = randomPerson();
        data.insert(person);
        List<Person> added = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            Person p = randomPerson();
            person.getFriends().add(p);
            added.add(p);
        }
        data.update(person);
        assertTrue(added.containsAll(person.getFriends()));
        int count = data.count(Person.class).get().value();
        assertEquals(11, count);
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
    public void testManyOrderBy() {
        Group group = new Group();
        group.setName("Group");
        data.insert(group);
        for (int i = 3; i >= 0; i--) {
            Person person = randomPerson();
            person.setName(new String(Character.toChars(65 + i)));
            data.insert(person);
            group.getOwners().add(person);
        }
        data.update(group);
        data.refresh(group, Group.OWNERS);
        List<Person> list = group.getOwners().toList();
        assertEquals("A", list.get(0).getName());
        assertEquals("B", list.get(1).getName());
        assertEquals("C", list.get(2).getName());
    }

    @Test
    public void testQueryFunctionNow() {
        Person person = randomPerson();
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DATE, 1);
        person.setBirthday(calendar.getTime());
        data.insert(person);
        try (Result<Person> query = data.select(Person.class)
            .where(Person.BIRTHDAY.gt(Now.now(Date.class))).get()) {
            assertEquals(1, query.toList().size());
        }
    }

    @Test
    public void testQueryFunctionRandom() {
        for (int i = 0; i < 10; i++) {
            Person person = randomPerson();
            data.insert(person);
        }
        try (Result<Person> query = data.select(Person.class).orderBy(new Random()).get()) {
            assertEquals(10, query.toList().size());
        }
    }

    @Test
    public void testSingleQueryWhere() {
        final String name = "duplicateFirstName";
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
    public void testSingleQueryWhereNot() {
        final String name = "firstName";
        final String email = "not@test.io";
        for (int i = 0; i < 10; i++) {
            Person person = randomPerson();
            switch (i) {
                case 0:
                    person.setName(name);
                    break;
                case 1:
                    person.setEmail(email);
                    break;
            }
            data.insert(person);
        }
        try (Result<Person> query = data.select(Person.class)
            .where(
                not(Person.NAME.equal(name)
                        .or(Person.EMAIL.equal(email)))
            ).get()) {
            assertEquals(8, query.toList().size());
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
        for (int i = 0; i < 10; i++) {
            Person person = randomPerson();
            person.setName(name);
            data.insert(person);
        }
        for (int i = 0; i < 3; i++) {
            try (Result<Person> query = data.select(Person.class)
                .where(Person.NAME.equal(name))
                .orderBy(Person.NAME)
                .limit(5).get()) {
                assertEquals(5, query.toList().size());
            }
            try (Result<Person> query = data.select(Person.class)
                .where(Person.NAME.equal(name))
                .orderBy(Person.NAME)
                .limit(5).offset(5).get()) {
                assertEquals(5, query.toList().size());
            }
        }
    }

    @Test
    public void testSingleQueryWhereNull() {
        Person person = randomPerson();
        person.setName(null);
        data.insert(person);
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
    public void testQueryByForeignKey() {
        Person person = randomPerson();
        data.insert(person);
        Phone phone1 = randomPhone();
        Phone phone2 = randomPhone();
        person.getPhoneNumbers().add(phone1);
        person.getPhoneNumbers().add(phone2);
        data.update(person);
        assertTrue(person.getPhoneNumbersSet().contains(phone1));
        try (Result<Phone> result = data.select(Phone.class).where(Phone.OWNER.eq(person)).get()) {
            assertTrue(person.getPhoneNumbersList().containsAll(result.toList()));
            assertEquals(2, person.getPhoneNumbersList().size());
            assertEquals(2, result.toList().size());
        }
        // by id
        try (Result<Phone> result =
                 data.select(Phone.class).where(Phone.OWNER_ID.eq(person.getId())).get()) {
            assertTrue(person.getPhoneNumbersList().containsAll(result.toList()));
            assertEquals(2, person.getPhoneNumbersList().size());
            assertEquals(2, result.toList().size());
        }
    }

    @Test
    public void testQueryByUUID() {
        Person person = randomPerson();
        data.insert(person);
        UUID uuid = person.getUUID();
        Person result = data.select(Person.class).where(Person.UUID.eq(uuid)).get().first();
        assertEquals(person, result);
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
            person.setAge(i + 1);
            data.insert(person);
        }
        Integer result = data.select(NamedExpression.ofInteger("avg_age").avg())
            .from(data.select(Person.AGE.sum().as("avg_age"))
                      .groupBy(Person.AGE).as("t1")).get().first().get(0);
        assertTrue(result >= 5); // derby rounds up
    }

    @Test
    public void testQueryJoinOrderBy() {
        Person person = randomPerson();
        person.setAddress(randomAddress());
        data.insert(person);
        // not a useful query just tests the sql output
        Result<Address> result = data.select(Address.class)
            .join(Person.class).on(Person.ADDRESS_ID.eq(Person.ID))
            .where(Person.ID.eq(person.getId()))
            .orderBy(Address.CITY.desc())
            .get();
        List<Address> addresses = result.toList();
        assertTrue(addresses.size() > 0);
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
    public void testQuerySelectSubstr() {
        // TODO fix for SQLServer
        Person person = randomPerson();
        person.setName("  Name");
        data.insert(person);
        Tuple result = data.select(Person.NAME.substr(3, 6).as("name")).get().first();
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
    public void testQueryOrderByFunction() {
        Person person = randomPerson();
        person.setName("BOBB");
        data.insert(person);
        person = randomPerson();
        person.setName("BobA");
        data.insert(person);
        person = randomPerson();
        person.setName("bobC");
        data.insert(person);
        List<Tuple> list = data.select(Person.NAME)
            .orderBy(Upper.upper(Person.NAME).desc()).get().toList();
        assertTrue(list.get(0).get(0).equals("bobC"));
        assertTrue(list.get(1).get(0).equals("BOBB"));
        assertTrue(list.get(2).get(0).equals("BobA"));
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
        Return<? extends Result<Tuple>> groupNames = data.select(Group.NAME)
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
            .where(Person.AGE.gt(5).and(Person.AGE.lt(75)).and(Person.NAME.ne("Bob")))
            .or(Person.NAME.eq("Bob"))
            .get().toList();
        assertTrue(result.contains(person2));
        assertTrue(result.contains(person3));

        result = data.select(Person.class)
            .where(Person.AGE.gt(10).or(Person.AGE.eq(75)))
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
        final int[] counts = new int[] { 0 };
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
    public void testQueryUpdateRefresh() {
        Person person = randomPerson();
        data.insert(person);
        int id = person.getId();
        int rowCount = data.update(Person.class)
            .set(Person.AGE, 50)
            .where(Person.ID.eq(id)).get().value();
        assertEquals(1, rowCount);
        Person selected = data.select(Person.class).where(Person.ID.eq(id)).get().first();
        assertSame(50, selected.getAge());
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
    public void testQueryEqualsIgnoreCase() {
        Person person = randomPerson();
        person.setName("Carol");
        data.insert(person);
        Person a = data.select(Person.class)
            .where(Person.NAME.equalsIgnoreCase("carol"))
            .get().first();
        assertSame(a, person);
    }

    @Test
    public void testQueryCase() {
        String[] names = new String[] { "Carol", "Bob", "Jack" };
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

    @Test
    public void testQueryRaw() {
        final int count = 5;
        List<Person> people = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Person person = randomPerson();
            data.insert(person);
            people.add(person);
        }
        List<Long> resultIds = new ArrayList<>();
        try (Result<Tuple> result = data.raw("select * from Person")) {
            List<Tuple> list = result.toList();
            assertEquals(count, list.size());
            for (int i = 0; i < people.size(); i++) {
                Tuple tuple = list.get(i);
                String name = tuple.get("name");
                assertEquals(people.get(i).getName(), name);
                Number id = tuple.get("id");
                assertEquals(people.get(i).getId(), id.intValue());
                resultIds.add(id.longValue());
            }
        }
        try (Result<Tuple> result = data.raw("select * from Person WHERE id IN ?", resultIds)) {
            List<Tuple> list = result.toList();
            List<Long> ids = new ArrayList<>(list.size());
            for (Tuple tuple : list) {
                ids.add(tuple.<Number>get("id").longValue());
            }
            assertEquals(resultIds, ids);
        }
        try (Result<Tuple> result = data.raw("select count(*) from Person")) {
            Number number = result.first().get(0); // can be long or int depending on db
            assertEquals(count, number.intValue());
        }
        try (Result<Tuple> result = data.raw("select * from Person WHERE id = ?", people.get(0))) {
            assertEquals(result.first().<Number>get("id").intValue(), people.get(0).getId());
        }
    }

    @Test
    public void testQueryRawWithNamedParameter() {
        final int count = 5;
        List<Person> people = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Person person = randomPerson();
            data.insert(person);
            people.add(person);
        }
        List<Long> resultIds = new ArrayList<>();
        try (Result<Tuple> result = data.raw("select * from Person")) {
            List<Tuple> list = result.toList();
            assertEquals(count, list.size());
            for (int i = 0; i < people.size(); i++) {
                Tuple tuple = list.get(i);
                String name = tuple.get("name");
                assertEquals(people.get(i).getName(), name);
                Number id = tuple.get("id");
                assertEquals(people.get(i).getId(), id.intValue());
                resultIds.add(id.longValue());
            }
        }
        Map<String, Object> resultIdMap = new HashMap<>();
        resultIdMap.put("ids", resultIds);
        try (Result<Tuple> result = data.raw("select * from Person WHERE id IN :ids", resultIdMap)) {
            List<Tuple> list = result.toList();
            List<Long> ids = new ArrayList<>(list.size());
            for (Tuple tuple : list) {
                ids.add(tuple.<Number>get("id").longValue());
            }
            assertEquals(resultIds, ids);
        }
        try (Result<Tuple> result = data.raw("select count(*) from Person")) {
            Number number = result.first().get(0); // can be long or int depending on db
            assertEquals(count, number.intValue());
        }

        Map<String, Object> idMap = new HashMap<>();
        idMap.put("id", people.get(0).getId());
        try (Result<Tuple> result = data.raw("select * from Person WHERE id = :id", idMap)) {
            assertEquals(result.first().<Number>get("id").intValue(), people.get(0).getId());
        }

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("id", people.get(0).getId());
        parameters.put("name", people.get(0).getName());
        try (Result<Tuple> result = data.raw("select * from Person WHERE id = :id and name = :name and id = :id", parameters)) {
            assertEquals(result.first().<Number>get("id").intValue(), people.get(0).getId());
        }
    }

    @Test
    public void testQueryRawEntities() {
        final int count = 5;
        List<Person> people = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Person person = randomPerson();
            data.insert(person);
            people.add(person);
        }
        List<Integer> resultIds = new ArrayList<>();
        try (Result<Person> result = data.raw(Person.class, "select * from Person")) {
            List<Person> list = result.toList();
            assertEquals(count, list.size());
            for (int i = 0; i < people.size(); i++) {
                Person person = list.get(i);
                String name = person.getName();
                assertEquals(people.get(i).getName(), name);
                assertEquals(people.get(i).getId(), person.getId());
                resultIds.add(person.getId());
            }
        }
        try (Result<Person> result =
                 data.raw(Person.class, "select * from Person WHERE id IN ?", resultIds)) {
            List<Person> list = result.toList();
            List<Integer> ids = new ArrayList<>(list.size());
            for (Person tuple : list) {
                ids.add(tuple.getId());
            }
            assertEquals(resultIds, ids);
        }
        try (Result<Person> result =
                 data.raw(Person.class, "select * from Person WHERE id = ?", people.get(0))) {
            assertEquals(result.first().getId(), people.get(0).getId());
        }
    }

    @Test
    public void testQueryRawEntitiesWithNamedParameter() {
        final int count = 5;
        List<Person> people = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Person person = randomPerson();
            data.insert(person);
            people.add(person);
        }
        List<Integer> resultIds = new ArrayList<>();
        try (Result<Person> result = data.raw(Person.class, "select * from Person")) {
            List<Person> list = result.toList();
            assertEquals(count, list.size());
            for (int i = 0; i < people.size(); i++) {
                Person person = list.get(i);
                String name = person.getName();
                assertEquals(people.get(i).getName(), name);
                assertEquals(people.get(i).getId(), person.getId());
                resultIds.add(person.getId());
            }
        }
        Map<String, Object> resultIdMap = new HashMap<>();
        resultIdMap.put("ids", resultIds);
        try (Result<Person> result =
                 data.raw(Person.class, "select * from Person WHERE id IN :ids", resultIdMap)) {
            List<Person> list = result.toList();
            List<Integer> ids = new ArrayList<>(list.size());
            for (Person tuple : list) {
                ids.add(tuple.getId());
            }
            assertEquals(resultIds, ids);
        }

        Map<String, Object> idMap = new HashMap<>();
        idMap.put("id", people.get(0).getId());
        try (Result<Person> result =
                 data.raw(Person.class, "select * from Person WHERE id = :id", idMap)) {
            assertEquals(result.first().getId(), people.get(0).getId());
        }

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("id", people.get(0).getId());
        parameters.put("name", people.get(0).getName());
        try (Result<Tuple> result = data.raw("select * from Person WHERE id = :id and name = :name and id = :id", parameters)) {
            assertEquals(result.first().<Number>get("id").intValue(), people.get(0).getId());
        }
    }

    @Test
    public void testQueryUnionJoinOnSameEntities() {
        Group group = new Group();
        group.setName("Hello!");
        data.insert(group);
        Person person1 = randomPerson();
        person1.setName("Carol");
        person1.getGroups().add(group);
        data.insert(person1);
        Person person2 = randomPerson();
        person2.getGroups().add(group);
        person2.setName("Bob");
        data.insert(person2);
        List<Tuple> result = data.select(Person.NAME.as("personName"), Group.NAME.as("groupName"))
            .where(Person.ID.eq(person1.getId()))
            .union()
            .select(Person.NAME.as("personName"), Group.NAME.as("groupName"))
            .where(Person.ID.eq(person2.getId()))
            .orderBy(Person.NAME.as("personName")).get().toList();
        System.err.println(result.size());
        System.err.println(result.size() == 2);
        assertTrue(result.size() == 2);
        assertTrue(result.get(0).get("personName").equals("Bob"));
        assertTrue(result.get(0).get("groupName").equals("Hello!"));
        assertTrue(result.get(1).get("personName").equals("Carol"));
        assertTrue(result.get(1).get("groupName").equals("Hello!"));
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

    @Test(expected = PersistenceException.class)
    public void testInsertWithoutCascadeActionSave() {
        Child child = new Child();
        child.setId(1);
        Parent parent = new Parent();
        parent.setChild(child);

        // This violates the Foreign Key Constraint, because Child does not exist and CascadeAction.SAVE is not specified
        data.insert(parent);
    }

    @Test(expected = StatementExecutionException.class)
    public void testInsertNoCascade_OneToOne_NonExistingChild() {
        // Insert parent entity, associated one-to-one to a non existing child entity
        // This should fail with a foreign-key violation, since child does not exist in the database
        ChildOneToOneNoCascade child = new ChildOneToOneNoCascade();
        ParentNoCascade parent = new ParentNoCascade();
        parent.setId(1);
        parent.setOneToOne(child);
        data.insert(parent);
    }

    @Test
    public void testInsertNoCascade_OneToOne_ExistingChild() {
        // Insert parent entity, associated one-to-one to an existing child entity
        ChildOneToOneNoCascade child = new ChildOneToOneNoCascade();
        child.setId(1);
        child.setAttribute("1");
        data.insert(child);
        ParentNoCascade parent = new ParentNoCascade();
        parent.setId(1);
        parent.setOneToOne(child);
        data.insert(parent);

        // Assert that child has been associated to parent
        ParentNoCascade parentGot = data.findByKey(ParentNoCascade.class, 1l);
        assertEquals(child, parentGot.getOneToOne());
    }

    @Test(expected = StatementExecutionException.class)
    public void testInsertNoCascade_ManyToOne_NonExistingchild() {
        // Insert parent entity, associated many-to-one to a non existing child entity
        // This should fail with a foreign-key violation, since child does not exist in the database
        ChildManyToOneNoCascade child = new ChildManyToOneNoCascade();
        child.setId(1);
        child.setAttribute("1");
        ParentNoCascade parent = new ParentNoCascade();
        parent.setId(1);
        parent.setManyToOne(child);
        data.insert(parent);
    }

    @Test
    public void testInsertNoCascade_ManyToOne_ExistingChild() {
        // Insert parent entity, associated many-to-one to an existing child entity
        ChildManyToOneNoCascade child = new ChildManyToOneNoCascade();
        child.setId(1);
        child.setAttribute("1");
        data.insert(child);
        ParentNoCascade parent = new ParentNoCascade();
        parent.setId(1);
        parent.setManyToOne(child);
        data.insert(parent);

        // Assert that child has been associated to parent
        ParentNoCascade parentGot = data.findByKey(ParentNoCascade.class, 1l);
        assertEquals(child, parentGot.getManyToOne());
    }

    //@Test(expected = StatementExecutionException.class)
    @Test(expected = RowCountException.class)
    public void testInsertNoCascade_OneToMany_NonExistingChild() {
        // Insert parent entity, associated one-to-may to 1 non-existing child entity
        ChildOneToManyNoCascade child = new ChildOneToManyNoCascade();
        child.setId(1);
        child.setAttribute("1");
        ParentNoCascade parent = new ParentNoCascade();
        parent.setId(1);
        parent.getOneToMany().add(child);
        data.insert(parent);
    }

    @Test
    public void testInsertNoCascade_OneToMany_ExistingChild() {
        // Insert parent entity, associated one-to-may to 1 existing child entity
        ChildOneToManyNoCascade child = new ChildOneToManyNoCascade();
        child.setId(1);
        child.setAttribute("1");
        data.insert(child);
        ParentNoCascade parent = new ParentNoCascade();
        parent.setId(1);
        parent.getOneToMany().add(child);
        data.insert(parent);

        // Assert that child has been associated to parent
        ParentNoCascade parentGot = data.findByKey(ParentNoCascade.class, 1l);
        assertTrue(parentGot.getOneToMany().size() == 1);
        assertEquals(child, parentGot.getOneToMany().get(0));
    }

    @Test(expected = StatementExecutionException.class)
    public void testInsertNoCascade_ManyToMany_NonExistingChild() {
        // Insert parent entity, associated many-to-may to 1 non-existing child entity
        ChildManyToManyNoCascade child = new ChildManyToManyNoCascade();
        child.setId(1);
        child.setAttribute("1");
        ParentNoCascade parent = new ParentNoCascade();
        parent.setId(1);
        parent.getManyToMany().add(child);
        data.insert(parent);
    }

    @Test
    public void testInsertNoCascade_ManyToMany_ExistingChild() {
        // Insert parent entity, associated many-to-may to 1 existing child entity
        ChildManyToManyNoCascade child = new ChildManyToManyNoCascade();
        child.setId(1);
        child.setAttribute("1");
        data.insert(child);
        ParentNoCascade parent = new ParentNoCascade();
        parent.setId(1);
        parent.getManyToMany().add(child);
        data.insert(parent);

        // Assert that child has been associated to parent
        ParentNoCascade parentGot = data.findByKey(ParentNoCascade.class, 1l);
        assertTrue(parentGot.getManyToMany().size() == 1);
        assertEquals(child, parentGot.getManyToMany().get(0));
    }

    @Test
    public void testDeleteNoCascade_OneToOne() {
        // Insert parent and child entity
        ChildOneToOneNoCascade child = new ChildOneToOneNoCascade();
        child.setId(123);
        child.setAttribute("1");
        ParentNoCascade parent = new ParentNoCascade();
        parent.setId(123);
        parent.setOneToOne(child);
        data.insert(child);
        data.insert(parent);

        // Delete parent
        data.delete(parent);

        // Assert that child has not been deleted (i.e. that delete has not been cascaded)
        ChildOneToOneNoCascade childGot = data.findByKey(ChildOneToOneNoCascade.class, 123l);
        assertNotNull(childGot);
    }

    @Test
    public void testDeleteNoCascade_ManyToOne() {
        // Insert parent entity and child
        ChildManyToOneNoCascade child = new ChildManyToOneNoCascade();
        child.setId(123);
        child.setAttribute("1");
        ParentNoCascade parent = new ParentNoCascade();
        parent.setId(123);
        parent.setManyToOne(child);
        data.insert(child);
        data.insert(parent);

        // Delete parent
        data.delete(parent);

        // Assert that child has not been deleted (i.e. that delete has not been cascaded)
        ChildManyToOneNoCascade childGot = data.findByKey(ChildManyToOneNoCascade.class, 123l);
        assertNotNull(childGot);
    }

    @Test
    public void testDeleteNoCascade_OneToMany() {
        // Insert parent entity and children
        ChildOneToManyNoCascade child1 = new ChildOneToManyNoCascade();
        child1.setId(1);
        child1.setAttribute("1");
        ChildOneToManyNoCascade child2 = new ChildOneToManyNoCascade();
        child2.setId(2);
        child2.setAttribute("2");
        ParentNoCascade parent = new ParentNoCascade();
        parent.setId(1);
        parent.getOneToMany().add(child1);
        parent.getOneToMany().add(child2);
        data.insert(child1);
        data.insert(child2);
        data.insert(parent);

        // Delete parent
        data.delete(parent);

        // Assert that children have not been deleted (i.e. that delete has not been cascaded)
        ChildOneToManyNoCascade child1Got = data.findByKey(ChildOneToManyNoCascade.class, 1);
        assertNotNull(child1Got);
        assertNull(child1Got.getParent());
        ChildOneToManyNoCascade child2Got = data.findByKey(ChildOneToManyNoCascade.class, 2);
        assertNotNull(child2Got);
        assertNull(child2Got.getParent());
    }

    @Test
    public void testDeleteNoCascade_ManyToMany() {
        // Insert parent entity and children
        ChildManyToManyNoCascade child1 = new ChildManyToManyNoCascade();
        child1.setId(1);
        child1.setAttribute("1");
        ChildManyToManyNoCascade child2 = new ChildManyToManyNoCascade();
        child2.setId(2);
        child2.setAttribute("2");
        ParentNoCascade parent = new ParentNoCascade();
        parent.setId(1);
        parent.getManyToMany().add(child1);
        parent.getManyToMany().add(child2);
        data.insert(child1);
        data.insert(child2);
        data.insert(parent);

        // Delete parent
        data.delete(parent);

        // Assert that children have not been deleted (i.e. that delete has not been cascaded)
        ChildManyToManyNoCascade child1Got = data.findByKey(ChildManyToManyNoCascade.class, 1);
        assertNotNull(child1Got);
        ChildManyToManyNoCascade child2Got = data.findByKey(ChildManyToManyNoCascade.class, 2);
        assertNotNull(child2Got);
    }

}
