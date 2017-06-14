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
import io.requery.kotlin.invoke
import io.requery.sql.KotlinConfiguration
import io.requery.sql.KotlinEntityDataStore
import io.requery.sql.SchemaModifier
import io.requery.sql.TableCreationMode
import org.h2.jdbcx.JdbcDataSource
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.sql.SQLException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.properties.Delegates

class CompletableTest {

    val executor: ExecutorService = Executors.newSingleThreadExecutor()
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
    fun testInsert() {
        val person = FunctionalTest.randomPerson()
        val insertedPerson = data.execute {
            insert(person)
        }.get()

        assertTrue(insertedPerson.id > 0)
        data.invoke {
            val result = select(Person::class) where (Person::id eq person.id) limit 10
            assertSame(result().first(), person)
        }
    }


    @Test
    fun testGet() {
        val person = randomPerson()
        val personId = instance.insert(person).id

        val returnedPerson = data.execute {
            select(Person::class)
                    .where(Person::id.eq(personId))
                    .get()
                    .first()
        }.get()
        assertEquals(person, returnedPerson)
    }
}
