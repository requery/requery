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

import io.reactivex.Observable
import io.requery.kotlin.eq
import io.requery.reactivex.KotlinReactiveEntityStore
import io.requery.sql.KotlinConfiguration
import io.requery.sql.KotlinEntityDataStore
import io.requery.sql.SchemaModifier
import io.requery.sql.TableCreationMode
import org.h2.jdbcx.JdbcDataSource
import org.junit.After
import org.junit.Assert
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.sql.SQLException
import java.util.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class ReactiveTest {

    var instance : KotlinEntityDataStore<Any>? = null
    val data : KotlinReactiveEntityStore<Any> get() = KotlinReactiveEntityStore(instance!!)

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
    @Throws(Exception::class)
    fun testInsert() {
        val person = randomPerson()
        val latch = CountDownLatch(1)
        data.insert(person).subscribe { person ->
            assertTrue(person.id > 0)
            val cached = data.select(Person::class)
                    .where(Person::id.eq(person.id)).get().first()
            assertSame(cached, person)
            latch.countDown()
        }
        latch.await()
    }

    @Test
    @Throws(Exception::class)
    fun testDelete() {
        val person = randomPerson()
        data.insert(person).blockingGet()
        data.delete(person).blockingGet()
        val cached = data.select(Person::class)
                .where(Person::id.eq(person.id)).get().firstOrNull()
        assertNull(cached)
    }

    @Test
    @Throws(Exception::class)
    fun testInsertCount() {
        val person = randomPerson()
        Observable.just(person)
                .concatMap { person -> data.insert(person).toObservable() }
        val p = data.insert(person).blockingGet()
        assertTrue(p.id > 0)
        val count = data.count(Person::class).get().single().blockingGet()
        assertEquals(1, count.toLong())
    }

    @Test
    @Throws(Exception::class)
    fun testQueryEmpty() {
        val latch = CountDownLatch(1)
        data.select(Person::class).get().observable()
                .subscribe({ Assert.fail() }, { Assert.fail() }) { latch.countDown() }
        if (!latch.await(1, TimeUnit.SECONDS)) {
            Assert.fail()
        }
    }

    @Test
    @Throws(Exception::class)
    fun testQueryObservable() {
        for (i in 0..29) {
            val person = randomPerson()
            data.insert(person).blockingGet()
        }
        val people = ArrayList<Person>()
        data.select(Person::class).limit(50).get()
                .observable()
                .subscribe { person -> people.add(person) }
        assertEquals(30, people.size.toLong())
    }
}
