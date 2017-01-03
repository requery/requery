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
import io.requery.cache.EmptyEntityCache;
import io.requery.meta.EntityModel;
import io.requery.sql.Configuration;
import io.requery.sql.ConfigurationBuilder;
import io.requery.sql.EntityDataStore;
import io.requery.sql.SchemaModifier;
import io.requery.sql.TableCreationMode;
import io.requery.sql.platform.HSQL;
import io.requery.sql.Platform;
import io.requery.test.model2.Event;
import io.requery.test.model2.Models;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import javax.sql.CommonDataSource;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.UUID;
import java.util.concurrent.Executors;

public class TimeConversionsTest {

    protected EntityDataStore<Persistable> data;

    @Before
    public void setup() throws SQLException {
        Platform platform = new HSQL();
        CommonDataSource dataSource = DatabaseType.getDataSource(platform);
        EntityModel model = Models.MODEL2;

        Configuration configuration = new ConfigurationBuilder(dataSource, model)
            .useDefaultLogging()
            .setEntityCache(new EmptyEntityCache())
            .setWriteExecutor(Executors.newSingleThreadExecutor())
            .build();

        SchemaModifier tables = new SchemaModifier(configuration);
        tables.createTables(TableCreationMode.DROP_CREATE);
        data = new EntityDataStore<>(configuration);
    }

    @Test
    public void testInsertReadDate() {
        Event event = new Event();
        UUID id = UUID.randomUUID();
        LocalDate localDateNow = LocalDate.now();
        event.setId(id);
        event.setLocalDate(localDateNow);

        data.insert(event);

        event = data.findByKey(Event.class, id);
        Assert.assertEquals(localDateNow, event.getLocalDate());
    }

    @Test
    public void testInsertReadLocalTime() {
        Event event = new Event();
        UUID id = UUID.randomUUID();
        LocalTime localTimeNow = LocalTime.now();
        event.setId(id);
        event.setLocalTime(localTimeNow);
        data.insert(event);
        event = data.findByKey(Event.class, id);
        Assert.assertEquals(localTimeNow.withNano(0), event.getLocalTime());
    }

    @Test
    public void testInsertReadDateTime() {
        Event event = new Event();
        UUID id = UUID.randomUUID();
        LocalDateTime localDateTimeNow = LocalDateTime.now().withNano(0);
        OffsetDateTime offsetDateTimeNow = OffsetDateTime.now(ZoneId.of("UTC")).withNano(0);
        ZonedDateTime zonedDateTimeNow = ZonedDateTime.now(ZoneId.of("UTC")).withNano(0);
        event.setId(id);
        event.setLocalDateTime(localDateTimeNow);
        event.setOffsetDateTime(offsetDateTimeNow);
        event.setZonedDateTime(zonedDateTimeNow);

        data.insert(event);

        event = data.findByKey(Event.class, id);
        Assert.assertEquals(localDateTimeNow, event.getLocalDateTime());
        Assert.assertEquals(offsetDateTimeNow, event.getOffsetDateTime());
        Assert.assertEquals(zonedDateTimeNow.toInstant(), event.getZonedDateTime().toInstant());
    }
}
