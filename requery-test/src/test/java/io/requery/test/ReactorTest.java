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
import io.requery.cache.EntityCacheBuilder;
import io.requery.meta.EntityModel;
import io.requery.reactor.ReactorEntityStore;
import io.requery.sql.Configuration;
import io.requery.sql.ConfigurationBuilder;
import io.requery.sql.EntityDataStore;
import io.requery.sql.Platform;
import io.requery.sql.SchemaModifier;
import io.requery.sql.TableCreationMode;
import io.requery.sql.platform.HSQL;
import io.requery.test.model.Person;
import io.requery.test.model.Phone;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.cache.CacheManager;
import javax.cache.Caching;
import javax.cache.spi.CachingProvider;
import javax.sql.CommonDataSource;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ReactorTest extends RandomData {

    protected ReactorEntityStore<Persistable> data;

    @Before
    public void setup() throws SQLException {
        Platform platform = new HSQL();
        CommonDataSource dataSource = DatabaseType.getDataSource(platform);
        EntityModel model = io.requery.test.model.Models.DEFAULT;

        CachingProvider provider = Caching.getCachingProvider();
        CacheManager cacheManager = provider.getCacheManager();
        Configuration configuration = new ConfigurationBuilder(dataSource, model)
            .useDefaultLogging()
            .setWriteExecutor(Executors.newSingleThreadExecutor())
            .setEntityCache(new EntityCacheBuilder(model)
                .useReferenceCache(true)
                .useSerializableCache(true)
                .useCacheManager(cacheManager)
                .build())
            .build();

        SchemaModifier tables = new SchemaModifier(configuration);
        tables.createTables(TableCreationMode.DROP_CREATE);
        data = new ReactorEntityStore<>(new EntityDataStore<Persistable>(configuration));
    }

    @After
    public void teardown() {
        if (data != null) {
            data.close();
        }
    }

    @Test
    public void testDelete() throws Exception {
        Person person = randomPerson();
        data.insert(person).block();
        data.delete(person).block();
        Person cached = data.select(Person.class)
            .where(Person.ID.equal(person.getId())).get().firstOrNull();
        assertNull(cached);
    }

    @Test
    public void testInsertCount() throws Exception {
        Person person = randomPerson();
        Flux.just(person)
            .concatMap(new Function<Person, Flux<Person>>() {
            @Override
            public Flux<Person> apply(Person person) {
                return data.insert(person).flux();
            }
        });
        Person p = data.insert(person).block();
        assertTrue(p.getId() > 0);
        int count = data.count(Person.class).get().mono().block();
        assertEquals(1, count);
    }

    @Test
    public void testInsertOneToMany() throws Exception {
        final Person person = randomPerson();
        data.insert(person).map(new Function<Person, Phone>() {
            @Override
            public Phone apply(Person person) {
                Phone phone1 = randomPhone();
                phone1.setOwner(person);
                return phone1;
            }
        }).flatMap(new Function<Phone, Mono<?>>() {
            @Override
            public Mono<?> apply(Phone phone) {
                return data.insert(phone);
            }
        }).blockFirst();
        assertTrue(person.getPhoneNumbers().toList().size() == 1);
    }

    @Test
    public void testQueryEmpty() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        data.select(Person.class).get().flux()
                .subscribe(new Consumer<Person>() {
            @Override
            public void accept(Person person) {
                Assert.fail();
            }
        }, new Consumer<Throwable>() {
            @Override
            public void accept(Throwable throwable) {
                Assert.fail();
            }
        }, new Runnable() {
            @Override
            public void run() {
                latch.countDown();
            }
        });
        if (!latch.await(1, TimeUnit.SECONDS)) {
            Assert.fail();
        }
    }

    @Test
    public void testQueryObservable() throws Exception {
        for (int i = 0; i < 30; i++) {
            Person person = randomPerson();
            data.insert(person).block();
        }
        final List<Person> people = new ArrayList<>();
        data.select(Person.class).limit(50).get()
            .flux()
            .subscribe(new Consumer<Person>() {
            @Override
            public void accept(Person person) {
                people.add(person);
            }
        });
        assertEquals(30, people.size());
    }

    @Test
    public void testQueryObservableFromEntity() throws Exception {
        final Person person = randomPerson();
        data.insert(person).map(new Function<Person, Phone>() {
            @Override
            public Phone apply(Person person) {
                Phone phone1 = randomPhone();
                phone1.setOwner(person);
                return phone1;
            }
        }).flatMap(new Function<Phone, Mono<?>>() {
            @Override
            public Mono<?> apply(Phone phone) {
                return data.insert(phone);
            }
        }).blockFirst();
        int count = person.getPhoneNumbers().toList().size();
        assertEquals(1, count);
    }

    @Test
    public void testQueryObservablePull() throws Exception {
        for (int i = 0; i < 36; i++) {
            Person person = randomPerson();
            data.insert(person).block();
        }
        final List<Person> people = new ArrayList<>();
        data.select(Person.class).get()
            .flux()
            .subscribe(new Subscriber<Person>() {
                Subscription s;
                @Override
                public void onSubscribe(Subscription s) {
                    this.s = s;
                    s.request(10);
                }

                @Override
                public void onComplete() {
                }

                @Override
                public void onError(Throwable e) {
                }

                @Override
                public void onNext(Person person) {
                    people.add(person);
                    if (people.size() % 10 == 0 && people.size() > 1) {
                        s.request(10);
                    }
                }
            });
        assertEquals(36, people.size());
    }
}
