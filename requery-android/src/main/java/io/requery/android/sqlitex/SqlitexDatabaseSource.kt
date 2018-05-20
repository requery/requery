/*
 * Copyright 2018 requery.io
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

package io.requery.android.sqlitex

import android.content.Context
import android.text.TextUtils
import io.requery.android.DefaultMapping
import io.requery.android.LoggingListener
import io.requery.android.database.sqlite.SQLiteDatabase
import io.requery.android.database.sqlite.SQLiteOpenHelper
import io.requery.android.sqlite.DatabaseProvider
import io.requery.android.sqlite.SchemaUpdater
import io.requery.meta.EntityModel
import io.requery.sql.*
import io.requery.sql.platform.SQLite
import io.requery.util.function.Function

import java.sql.Connection
import java.sql.SQLException

/**
 * Wrapper for working with Android SQLite databases. That use the
 * [requery Android SQLite support library](https://github.com/requery/sqlite-android).
 *
 *
 *  This class extends the support version of [SQLiteOpenHelper] and is used to create the
 * database. [.onCreate] will create the database tables & columns
 * automatically.
 *
 *
 *  [.onUpgrade] will create any missing tables/columns, however
 * it will not remove any tables/columns more complex upgrades should be handled by overriding
 * [.onUpgrade] and implementing a script to handle the migration.
 *
 * @author Nikhil Purushe
 */
open class SqlitexDatabaseSource(context: Context,
                            private val model: EntityModel?,
                            name: String,
                            version: Int)
    : SQLiteOpenHelper(context, name, null, version), DatabaseProvider<SQLiteDatabase> {

    private val platform: Platform
    private var mapping: Mapping? = null
    private var db: SQLiteDatabase? = null
    private var _configuration: io.requery.sql.Configuration? = null
    private var loggingEnabled: Boolean = false
    private var mode: TableCreationMode? = null

    constructor(context: Context, model: EntityModel, version: Int)
            : this(context, model, getDefaultDatabaseName(context, model), version)

    init {
        if (model == null) {
            throw IllegalArgumentException("null model")
        }
        this.platform = SQLite()
        this.mode = TableCreationMode.CREATE_NOT_EXISTS
    }

    companion object {
        private fun getDefaultDatabaseName(context: Context, model: EntityModel): String {
            return if (TextUtils.isEmpty(model.name))
                context.packageName
            else
                model.name
        }
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

    @Throws(SQLException::class)
    private fun getConnection(db: SQLiteDatabase): Connection {
        synchronized(this) {
            return SqlitexConnection(db)
        }
    }

    override fun getConfiguration(): Configuration {
        if (mapping == null) {
            mapping = onCreateMapping(platform)
        }
        if (mapping == null) {
            throw IllegalStateException()
        }
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

    override fun onConfigure(db: SQLiteDatabase) {
        db.setForeignKeyConstraintsEnabled(true)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        this.db = db
        val updater = SchemaUpdater(configuration,
                Function { s -> db.rawQuery(s, null) }, mode)
        updater.update()
    }

    @Throws(SQLException::class)
    override fun getConnection(): Connection {
        synchronized(this) {
            if (db == null) {
                db = getWritableDatabase()
            }
            return getConnection(db!!)
        }
    }
}
