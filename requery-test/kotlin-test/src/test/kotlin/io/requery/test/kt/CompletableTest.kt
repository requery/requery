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

package io.requery.test.kt

import io.requery.async.KotlinCompletableEntityStore
import io.requery.kotlin.eq
import io.requery.query.Result
import io.requery.sql.KotlinConfiguration
import io.requery.sql.KotlinEntityDataStore
import io.requery.sql.SchemaModifier
import io.requery.sql.TableCreationMode
import org.h2.jdbcx.JdbcDataSource
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.sql.SQLException
import java.util.concurrent.Executors
import kotlin.properties.Delegates

class CompletableTest {

    val executor = Executors.newSingleThreadExecutor()
    var instance: KotlinEntityDataStore<Any> by Delegates.notNull()
    val data: KotlinCompletableEntityStore<Any> get() = KotlinCompletableEntityStore(instance, executor)

    internal fun randomPerson(): Person {
        return FunctionalTest.randomPerson()
    }

    @Before
    @Throws(SQLException::class)
    fun setup() {
        val model = Models.KT
        val dataSource = JdbcDataSource()
        dataSource.setUrl("jdbc:h2:~/testh2")
        dataSource.user = "sa"
        dataSource.password = "sa"

        val configuration = KotlinConfiguration(
                dataSource = dataSource,
                model = model,
                statementCacheSize = 0,
                useDefaultLogging = true)
        instance = KotlinEntityDataStore(configuration)
        val tables = SchemaModifier(configuration)
        tables.dropTables()
        val mode = TableCreationMode.CREATE
        tables.createTables(mode)
    }

    @After
    fun teardown() {
        data.close()
    }

    @Test
    fun testGet() {
        val person = randomPerson()
        val personId = instance.insert(person).id

        data.select(Person::class)
                .where(Person::id.eq(personId))
                .get()
                .toCompletableFuture(Result<Person>::firstOrNull)
                .get()
                .let {
                    assertEquals(person, it)
                }
    }

    @Test
    fun testGetUsingExecute() {
        val person = randomPerson()
        val personId = instance.insert(person).id

        data.execute {
            data.select(Person::class)
                    .where(Person::id.eq(personId))
                    .get()
                    .firstOrNull()
        }
                .get()
                .let {
                    assertEquals(person, it)
                }

    }

    @Test
    fun testInsertCount() {
        val person = randomPerson()
        data.insert(person)
                .get()
                .let {
                    assertTrue(it.id > 0)
                }

        data.count(Person::class)
                .get()
                .toCompletableFuture()
                .get()
                .let {
                    assertEquals(1, it)
                }
    }
}
