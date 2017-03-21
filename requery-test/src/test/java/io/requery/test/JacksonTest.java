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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.ValueInstantiator;
import io.requery.Persistable;
import io.requery.cache.WeakEntityCache;
import io.requery.jackson.EntityMapper;
import io.requery.meta.EntityModel;
import io.requery.sql.Configuration;
import io.requery.sql.ConfigurationBuilder;
import io.requery.sql.EntityDataStore;
import io.requery.sql.SchemaModifier;
import io.requery.sql.TableCreationMode;
import io.requery.sql.platform.SQLite;
import io.requery.test.model3.Event;
import io.requery.test.model3.LocationEntity;
import io.requery.test.model3.Models;
import io.requery.test.model3.Place;
import io.requery.test.model3.Tag;
import org.junit.Before;
import org.junit.Test;

import javax.sql.CommonDataSource;
import java.io.IOException;
import java.io.StringWriter;
import java.sql.SQLException;
import java.util.UUID;
import java.util.concurrent.Executors;

import static org.junit.Assert.assertSame;

public class JacksonTest {

    private EntityDataStore<Persistable> data;

    @Before
    public void setup() throws SQLException {
        CommonDataSource dataSource = DatabaseType.getDataSource(new SQLite());
        EntityModel model = Models.MODEL3;
        Configuration configuration = new ConfigurationBuilder(dataSource, model)
            .useDefaultLogging()
            .setEntityCache(new WeakEntityCache())
            .setWriteExecutor(Executors.newSingleThreadExecutor())
            .build();

        SchemaModifier tables = new SchemaModifier(configuration);
        tables.createTables(TableCreationMode.DROP_CREATE);
        data = new EntityDataStore<>(configuration);
    }

    @Test
    public void testOneToManySerialize() {
        Event event = new Event();
        UUID id = UUID.randomUUID();
        event.setId(id);
        event.setName("test");
        Tag t1 = new Tag();
        t1.setId(UUID.randomUUID());
        Tag t2 = new Tag();
        t2.setId(UUID.randomUUID());
        event.getTags().add(t1);
        event.getTags().add(t2);
        Place p = new Place();
        p.setId("SF");
        p.setName("San Francisco, CA");
        event.setPlace(p);
        data.insert(event);

        ObjectMapper mapper = new EntityMapper(Models.MODEL3, data);
        StringWriter writer = new StringWriter();
        try {
            mapper.writeValue(writer, event);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        String value = writer.toString();
        System.out.println(value);

        try {
            Event read = mapper.readValue(value, Event.class);
            assertSame(event, read);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testManyToManySerialize() {
        Tag t1 = new Tag();
        t1.setId(UUID.randomUUID());
        for (int i = 0; i < 3; i++) {
            Event event = new Event();
            UUID id = UUID.randomUUID();
            event.setId(id);
            event.setName("event" + i);
            t1.getEvents().add(event);
        }
        data.insert(t1);

        ObjectMapper mapper = new EntityMapper(Models.MODEL3, data);
        StringWriter writer = new StringWriter();
        try {
            mapper.writeValue(writer, t1);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        String value = writer.toString();
        System.out.println(value);

        try {
            Tag tag = mapper.readValue(value, Tag.class);
            assertSame(t1, tag);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testEmbedSerialize() {
        LocationEntity t1 = new LocationEntity();
        t1.setId(1);
        data.insert(t1);

        ObjectMapper mapper = new EntityMapper(Models.MODEL3, data);
        StringWriter writer = new StringWriter();
        try {
            mapper.writeValue(writer, t1);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        String value = writer.toString();
        System.out.println(value);

        try {
            LocationEntity location = mapper.readValue(value, LocationEntity.class);
            assertSame(t1, location);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    ValueInstantiator s;
}
