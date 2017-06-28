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

package io.requery.test.kt

import io.requery.kotlin.*
import io.requery.sql.KotlinConfiguration
import io.requery.sql.KotlinEntityDataStore
import io.requery.sql.SchemaModifier
import io.requery.sql.TableCreationMode
import org.h2.jdbcx.JdbcDataSource
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.net.URL
import java.util.*

class FunctionalTest {

    lateinit var instance : KotlinEntityDataStore<Any>
    val data : KotlinEntityDataStore<Any> get() = instance

    companion object {
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
    fun testSelectPartial() {
        val person = randomPerson()
        data.invoke {
            insert(person)
            assertTrue(person.id > 0)
            val result = select(Person::class, Person::id, Person::name) limit 1
            val first = result().first()
            assertNotNull(first.name)
        }
    }

    @Test
    fun testSelectTuple() {
        val person = randomPerson()
        data.invoke {
            insert(person)
            assertTrue(person.id > 0)
            val result = select(Person::id, Person::name) limit 1
            val first = result().first()
            assertNotNull(first[0])
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

    @Test
    fun testQueryJoinOrderBy() {
        val person = randomPerson()
        person.address = AddressEntity()
        person.address.city = "San Francisco"
        person.address.country = "US"
        person.address.state = "CA"
        data.insert(person)
        // not a useful query just tests the sql output
        val result = data.select(Address::class)
                .join(Person::class).on(Person::address.eq(Person::id))
                .where(Person::id.eq(person.id))
                .orderBy(Address::city.desc())
                .get()
        assertTrue(result.toList().size > 0)
    }

    @Test
    fun testQueryRawEntity() {
        val person = randomPerson()
        data.insert(person)
        // not a useful query just tests the sql output
        val result = data.raw(Person::class, "SELECT * FROM person")
        assertSame(result.first(), person)
    }

    @Test
    fun testQueryUpdate() {
        val person = randomPerson()
        person.age = 100
        data.insert(person)
        val rowCount = data.update<Person>(Person::class)
                .set(Person::about, "nothing")
                .set(Person::age, 50)
                .where(Person::age.eq(100)).get().value()
        assertEquals(1, rowCount.toLong())
    }

    @After
    fun teardown() {
        data.close()
    }
}
