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

package io.requery.android.sqlite

import android.annotation.TargetApi
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.os.Build
import android.text.TextUtils
import io.requery.android.DefaultMapping
import io.requery.android.LoggingListener
import io.requery.meta.EntityModel
import io.requery.sql.Configuration
import io.requery.sql.ConfigurationBuilder
import io.requery.sql.Mapping
import io.requery.sql.Platform
import io.requery.sql.SchemaModifier
import io.requery.sql.TableCreationMode
import io.requery.sql.platform.SQLite
import io.requery.util.function.Function

import java.sql.Connection
import java.sql.SQLException
import java.sql.SQLNonTransientConnectionException

/**
 * Wrapper for working with Android SQLite databases. This provides a [Connection] wrapping
 * the standard java SQLite API turning it into standard JDBC APIs.
 *
 *
 *  This class extends the standard [SQLiteOpenHelper] and is used to create the database.
 * [.onCreate] will create the database tables & columns automatically.
 *
 *
 *  [.onUpgrade] will create any missing tables/columns, however
 * it will not remove any tables/columns more complex upgrades should be handled by overriding
 * [.onUpgrade] and implementing a script to handle the migration.
 *
 * @author Nikhil Purushe
 */
open class DatabaseSource
/**
 * Creates a new [DatabaseSource] instance.
 *
 * @param context context
 * @param model   the entity model
 * @param name    database filename
 * @param factory optional [android.database.sqlite.SQLiteDatabase.CursorFactory]
 * @param version the schema version
 * @param platform platform instance
 */
@JvmOverloads constructor(context: Context,
                          private val model: EntityModel?,
                          name: String?,
                          factory: SQLiteDatabase.CursorFactory?,
                          version: Int,
                          platform: SQLite = SQLite())
    : SQLiteOpenHelper(context, name, factory, version), DatabaseProvider<SQLiteDatabase> {

    private val platform: Platform
    private var mapping: Mapping? = null
    private var db: SQLiteDatabase? = null
    private var configuration: Configuration? = null
    private var configured: Boolean = false
    private var loggingEnabled: Boolean = false
    private var mode: TableCreationMode? = null

    /**
     * Creates a new [DatabaseSource] instance.
     *
     * @param context context
     * @param model   the entity model
     * @param version the schema version
     */
    constructor(context: Context, model: EntityModel, version: Int)
            : this(context, model, getDefaultDatabaseName(context, model), null, version)

    /**
     * Creates a new [DatabaseSource] instance.
     *
     * @param context context
     * @param model   the entity model
     * @param name    database filename
     * @param version the schema version
     */
    constructor(context: Context, model: EntityModel, name: String?, version: Int)
            : this(context, model, name, null, version)

    init {
        if (model == null) {
            throw IllegalArgumentException("null model")
        }
        this.platform = platform
        this.mode = TableCreationMode.CREATE_NOT_EXISTS
    }

    override fun setLoggingEnabled(enable: Boolean) {
        this.loggingEnabled = enable
    }

    override fun setTableCreationMode(mode: TableCreationMode) {
        this.mode = mode
    }

    companion object {
        private fun getDefaultDatabaseName(context: Context, model: EntityModel): String {
            return if (TextUtils.isEmpty(model.name))
                context.packageName
            else
                model.name
        }
    }

    /**
     * Override to change the default [Mapping].
     *
     * @param platform platform instance
     * @return the configured mapping.
     */
    protected fun onCreateMapping(platform: Platform): Mapping {
        return DefaultMapping(platform)
    }

    /**
     * Override to modify the configuration before it is built.
     *
     * @param builder [instance][ConfigurationBuilder] to configure.
     */
    protected fun onConfigure(builder: ConfigurationBuilder) {
        if (loggingEnabled) {
            val loggingListener = LoggingListener()
            builder.addStatementListener(loggingListener)
        }
    }

    @Throws(SQLException::class)
    private fun getConnection(db: SQLiteDatabase): Connection {
        synchronized(this) {
            if (!db.isOpen) {
                throw SQLNonTransientConnectionException()
            }
            return SqliteConnection(db)
        }
    }

    override fun getConfiguration(): Configuration {
        if (mapping == null) {
            mapping = onCreateMapping(platform)
        }
        if (mapping == null) {
            throw IllegalStateException()
        }
        if (configuration == null) {
            val builder = ConfigurationBuilder(this, model)
                    .setMapping(mapping)
                    .setPlatform(platform)
                    .setBatchUpdateSize(1000)
            onConfigure(builder)
            configuration = builder.build()
        }
        return configuration!!
    }

    override fun onCreate(db: SQLiteDatabase) {
        this.db = db
        SchemaModifier(getConfiguration()!!).createTables(TableCreationMode.CREATE)
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        if (!db.isReadOnly) {
            db.setForeignKeyConstraintsEnabled(true)
        }
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        this.db = db
        val updater = SchemaUpdater(getConfiguration(),
                Function { s -> db.rawQuery(s, null) }, mode)
        updater.update()
    }

    @Throws(SQLException::class)
    override fun getConnection(): Connection {
        synchronized(this) {
            if (db == null) {
                db = writableDatabase
            }
            if (!configured && Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
                db!!.execSQL("PRAGMA foreign_keys = ON")
                val pageSize = db!!.pageSize
                // on later Android versions page size was correctly set to 4096,
                // do the same for older versions
                if (pageSize == 1024L) {
                    db!!.pageSize = 4096
                }
                configured = true
            }
            return getConnection(db!!)
        }
    }
}
