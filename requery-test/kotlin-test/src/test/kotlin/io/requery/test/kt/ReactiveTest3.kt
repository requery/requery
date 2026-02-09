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

import io.requery.kotlin.eq
import io.requery.reactivex3.KotlinReactiveEntityStore
import io.requery.sql.KotlinConfiguration
import io.requery.sql.KotlinEntityDataStore
import io.requery.sql.SchemaModifier
import io.requery.sql.TableCreationMode
import org.h2.jdbcx.JdbcDataSource
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.sql.SQLException

class ReactiveTest3 {

    lateinit var instance: KotlinEntityDataStore<Any>
    val data: KotlinReactiveEntityStore<Any> get() = KotlinReactiveEntityStore(instance)

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
                .maybe()
                .test()
                .assertValue(person)
                .assertComplete()
    }

    @Test
    fun testInsert() {
        val person = randomPerson()
        data.insert(person)
                .test()
                .assertValue { it.id > 0 }
                .assertNoErrors()
                .assertComplete()

        data.select(Person::class)
                .where(Person::id.eq(person.id))
                .get()
                .maybe()
                .test()
                .assertValue(person)
                .assertComplete()
    }

    @Test
    fun testDelete() {
        val person = randomPerson()

        data.insert(person)
                .test()
                .assertComplete()

        data.delete(person)
                .test()
                .assertNoErrors()
                .assertComplete()

        data.select(Person::class)
                .where(Person::id.eq(person.id))
                .get()
                .maybe()
                .test()
                .assertComplete()
                .assertNoValues()
    }

    @Test
    fun testInsertCount() {
        val person = randomPerson()
        data.insert(person)
                .test()
                .assertValue { it.id > 0 }
                .assertComplete()

        data.count(Person::class)
                .get()
                .single()
                .test()
                .assertValue(1)
                .assertComplete()
    }

    @Test
    fun testQueryEmpty() {
        data.select(Person::class)
                .get()
                .maybe()
                .test()
                .assertComplete()
                .assertNoValues()
    }

    @Test
    fun testQueryObservable() {
        for (i in 0..29) {
            val person = randomPerson()
            data.insert(person)
                    .test()
                    .assertComplete()
        }

        data.select(Person::class)
                .limit(50)
                .get()
                .observable()
                .toList()
                .test()
                .assertValue { it.size == 30 }
    }

    @Test
    fun testWithTransaction() {
        data.withTransaction {
            for (i in 1..10) {
                val person = randomPerson()
                insert(person)
            }
        } .test()
        data.select(Person::class)
                .get()
                .observable()
                .toList()
                .test()
                .assertValue { it.size == 10 }
    }
}