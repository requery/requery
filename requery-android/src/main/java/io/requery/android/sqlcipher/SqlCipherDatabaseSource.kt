/*
 * Copyright 2019 requery.io
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

package io.requery.android.sqlcipher

import android.content.Context
import io.requery.android.DefaultMapping
import io.requery.android.LoggingListener
import io.requery.android.sqlite.DatabaseProvider
import io.requery.android.sqlite.SchemaUpdater
import io.requery.meta.EntityModel
import io.requery.sql.Configuration
import io.requery.sql.ConfigurationBuilder
import io.requery.sql.Mapping
import io.requery.sql.Platform
import io.requery.sql.SchemaModifier
import io.requery.sql.TableCreationMode
import io.requery.sql.platform.SQLite
import io.requery.util.function.Function
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.database.SQLiteOpenHelper

import java.sql.Connection

open class SqlCipherDatabaseSource(context: Context,
                                   private val model: EntityModel,
                                   name: String,
                                   private val password: String,
                                   version: Int)
    : SQLiteOpenHelper(context, name, null, version), DatabaseProvider<SQLiteDatabase> {

    private val platform: Platform
    private val mapping: Mapping
    private var db: SQLiteDatabase? = null
    private var _configuration: Configuration? = null
    private var loggingEnabled: Boolean = false
    private var mode: TableCreationMode? = null

    init {
        this.platform = SQLite()
        this.mapping = onCreateMapping(platform)
        this.mode = TableCreationMode.CREATE_NOT_EXISTS
        SQLiteDatabase.loadLibs(context)
    }

    override fun setLoggingEnabled(enable: Boolean) {
        this.loggingEnabled = enable
    }

    override fun setTableCreationMode(mode: TableCreationMode) {
        this.mode = mode
    }

    protected fun onCreateMapping(platform: Platform): Mapping {
        return DefaultMapping(platform)
    }

    protected fun onConfigure(builder: ConfigurationBuilder) {
        if (loggingEnabled) {
            val loggingListener = LoggingListener()
            builder.addStatementListener(loggingListener)
        }
    }

    override fun getReadableDatabase(): SQLiteDatabase {
        return getReadableDatabase(password)
    }

    override fun getWritableDatabase(): SQLiteDatabase {
        return getWritableDatabase(password)
    }

    private fun getConnection(db: SQLiteDatabase): SqlCipherConnection {
        synchronized(this) {
            return SqlCipherConnection(db)
        }
    }

    override fun getConfiguration(): Configuration {
        if (_configuration == null) {
            val builder = ConfigurationBuilder(this, model)
                    .setMapping(mapping)
                    .setPlatform(platform)
                    .setBatchUpdateSize(1000)
            onConfigure(builder)
            _configuration = builder.build()
        }
        return _configuration!!
    }

    override fun onCreate(db: SQLiteDatabase) {
        this.db = db
        SchemaModifier(configuration).createTables(TableCreationMode.CREATE)
    }

    override fun onConfigure(db: SQLiteDatabase) {}

    override fun onOpen(db: SQLiteDatabase?) {
        super.onOpen(db)
        if (!db!!.isReadOnly) {
            db.execSQL("PRAGMA foreign_keys = ON")
        }
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        this.db = db
        val updater = SchemaUpdater(configuration,
                Function { s -> db.rawQuery(s, null) }, mode)
        updater.update()
    }

    override fun getConnection(): Connection {
        synchronized(this) {
            if (db == null) {
                db = getWritableDatabase(password)
            }
            return getConnection(db!!)
        }
    }
}
