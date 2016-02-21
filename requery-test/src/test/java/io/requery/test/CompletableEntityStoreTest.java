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
import io.requery.TransactionIsolation;
import io.requery.TransactionListener;
import io.requery.async.CompletableEntityStore;
import io.requery.async.CompletionStageEntityStore;
import io.requery.meta.EntityModel;
import io.requery.proxy.EntityProxy;
import io.requery.sql.Configuration;
import io.requery.sql.ConfigurationBuilder;
import io.requery.sql.EntityDataStore;
import io.requery.sql.Platform;
import io.requery.sql.SchemaModifier;
import io.requery.sql.TableCreationMode;
import io.requery.sql.platform.HSQL;
import io.requery.test.model.Person;
import io.requery.test.model.Phone;
import io.requery.util.function.Supplier;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.sql.CommonDataSource;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Function;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class CompletableEntityStoreTest extends RandomData {

    protected CompletionStageEntityStore<Persistable> data;
    private TransactionState transactionState;
    private enum TransactionState {
        BEGIN,
        COMMIT,
        ROLLBACK,
    }

    @Before
    public void setup() throws SQLException {
        Platform platform = new HSQL();
        CommonDataSource dataSource = DatabaseType.getDataSource(platform);
        EntityModel model = io.requery.test.model.Models.DEFAULT;

        final TransactionListener transactionListener = new TransactionListener() {
            @Override
            public void beforeBegin(TransactionIsolation isolation) {

            }

            @Override
            public void afterBegin(TransactionIsolation isolation) {
                transactionState = TransactionState.BEGIN;
            }

            @Override
            public void beforeCommit(Set<EntityProxy<?>> entities) {

            }

            @Override
            public void afterCommit(Set<EntityProxy<?>> entities) {
                transactionState = TransactionState.COMMIT;
            }

            @Override
            public void beforeRollback(Set<EntityProxy<?>> entities) {

            }

            @Override
            public void afterRollback(Set<EntityProxy<?>> entities) {
                transactionState = TransactionState.ROLLBACK;
            }
        };
        Configuration configuration = new ConfigurationBuilder(dataSource, model)
            .useDefaultLogging()
            .setStatementCacheSize(10)
            .setBatchUpdateSize(50)
            .setWriteExecutor(Executors.newSingleThreadExecutor())
            .addTransactionListenerFactory(new Supplier<TransactionListener>() {
                @Override
                public TransactionListener get() {
                    return transactionListener;
                }
            })
            .build();

        data = new CompletableEntityStore<>(
            new EntityDataStore<Persistable>(configuration));

        SchemaModifier tables = new SchemaModifier(configuration);
        tables.createTables(TableCreationMode.DROP_CREATE);
    }

    @After
    public void teardown() {
        if (data != null) {
            data.close();
        }
    }

    @Test
    public void testInsert() throws Exception {
        Person person = randomPerson();
        data.insert(person).thenAccept(new Consumer<Person>() {
            @Override
            public void accept(Person person) {
                assertTrue(person.getId() > 0);
                Person cached = data.select(Person.class)
                    .where(Person.ID.equal(person.getId())).get().first();
                assertSame(cached, person);
            }
        }).toCompletableFuture().get();
        assertEquals(transactionState, TransactionState.COMMIT);
    }

    @Test
    public void testInsertCount() throws Exception {
        Person person = randomPerson();
        data.insert(person).thenAccept(new Consumer<Person>() {
            @Override
            public void accept(Person person) {
                assertTrue(person.getId() > 0);
            }
        }).thenCompose(new Function<Void, CompletionStage<Integer>>() {
            @Override
            public CompletionStage<Integer> apply(Void aVoid) {
                return data.count(Person.class).get().toCompletableFuture();
            }
        }).toCompletableFuture().get();
    }

    @Test
    public void testInsertOneToMany() throws Exception {
        final Person person = randomPerson();
        data.insert(person).thenApply(new Function<Person, Phone>() {
            @Override
            public Phone apply(Person person) {
                Phone phone1 = randomPhone();
                phone1.setOwner(person);
                return phone1;
            }
        }).thenCompose(new Function<Phone, CompletionStage<Phone>>() {
            @Override
            public CompletionStage<Phone> apply(Phone phone) {
                return data.insert(phone);
            }
        }).toCompletableFuture().get();
        HashSet<Phone> set = new HashSet<>(person.getPhoneNumbers().toList());
        assertEquals(1, set.size());
    }

    @Test
    public void testQueryUpdate() throws ExecutionException, InterruptedException {
        Person person = randomPerson();
        person.setAge(100);
        data.insert(person).toCompletableFuture().get();
        CompletableFuture<Integer> rowCount = data.update(Person.class)
            .set(Person.ABOUT, "nothing")
            .set(Person.AGE, 50)
            .where(Person.AGE.equal(100)).get()
            .toCompletableFuture(Executors.newSingleThreadExecutor());
        assertEquals(1, rowCount.get().intValue());
    }

    @Test
    public void testInsertBlocking() throws Exception {
        final Person person = randomPerson();
        data.toBlocking().insert(person);
        assertTrue(person.getId() > 0);
    }

    @Test
    public void testQueryStream() throws Exception {
        for (int i = 0; i < 30; i++) {
            Person person = randomPerson();
            data.insert(person).toCompletableFuture().get();
        }
        final List<Person> people = new ArrayList<>();
        data.select(Person.class).orderBy(Person.NAME.asc().nullsLast()).limit(50).get()
            .stream().forEach(new Consumer<Person>() {
            @Override
            public void accept(Person person) {
                people.add(person);
            }
        });
        assertSame(30, people.size());
    }
}
