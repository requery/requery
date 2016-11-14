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

import io.requery.Persistable
import io.requery.kotlin.*
import io.requery.sql.*
import org.h2.jdbcx.JdbcDataSource
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.net.URL
import java.util.Calendar
import java.util.Random
import java.util.UUID

class FunctionalTest {

    var instance : KotlinEntityDataStore<Persistable>? = null
    val data : KotlinEntityDataStore<Persistable> get() = instance!!

    fun randomPerson(): Person {
        val random = Random()
        val person = PersonEntity()
        val firstNames = arrayOf("Alice", "Bob", "Carol")
        val lastNames = arrayOf("Smith", "Lee", "Jones")
        person.name = (firstNames[random.nextInt(firstNames.size)] + " " +
                lastNames[random.nextInt(lastNames.size)])
        person.email = (person.name.replace(" ".toRegex(), "").toLowerCase() + "@example.com")
        person.uuid = (UUID.randomUUID())
        person.homepage = (URL("http://www.requery.io"))
        val calendar = Calendar.getInstance()
        calendar.set(1900 + random.nextInt(90), random.nextInt(12), random.nextInt(30))
        person.birthday = calendar.time
        return person
    }

    @Before
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

    @Test
    fun testInsert() {
        val person = randomPerson()
        data.invoke {
            insert(person)
            assertTrue(person.id > 0)
            val result = select(Person::class) where (Person::id eq person.id) limit 10
            assertSame(result().first(), person)
        }
    }

    @Test
    fun testSelectObjectWithPartialProperties() {
        val person = randomPerson()
        data.invoke {
            insert(person)
            assertTrue(person.id > 0)
            val result = data.select(Person::id, Person::name) limit 1
            val first = result().first()
            assertNotNull(first.name)
        }
    }

    @Test
    fun testSelectPartialWithOneColumn() {
        val person = randomPerson()
        data.invoke {
            insert(person)
            assertTrue(person.id > 0)
            val result = data.selectPartial(Person::name) limit 1
            val first = result().first()
            assertNotNull(first[0])
            assertEquals(1, first.count())
        }
    }

    @Test
    fun testSelectPartialWithTwoColumns() {
        val person = randomPerson()
        data.invoke {
            insert(person)
            assertTrue(person.id > 0)
            val result = data.selectPartial(Person::name, Person::email) limit 1
            val first = result().first()
            assertNotNull(first[0])
            assertNotNull(first[1])
            assertEquals(2, first.count())
        }
    }

    @Test
    fun testWithTransaction() {
        data.withTransaction {
            insert(randomPerson())
            insert(randomPerson())
            insert(randomPerson())
            val result = select(Person::class) limit 10
            assertSame(result().toList().size, 3)
        }
    }

    @Test
    fun testQueryCompoundConditions() {
        val person = randomPerson()
        person.age = 75
        data.insert(person)
        val person2 = randomPerson()
        person2.age = 10
        person2.name = "Carol"
        data.insert(person2)
        val person3 = randomPerson()
        person3.age = 0
        person3.name = "Bob"
        data.insert(person3)
        val result = data select Person::class where (
                        (Person::age gt 5)
                        and (Person::age lt 75)
                        and (Person::name ne "Bob" )
                        or (Person::name eq "Bob") )
        val list = result.get().toList()
        assertTrue(list.contains(person2))
        assertTrue(list.contains(person3))
    }

    @After
    fun teardown() {
        data.close()
    }
}
