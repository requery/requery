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

package io.requery.android.sqlite;

import android.annotation.TargetApi;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Build;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import io.requery.android.DefaultMapping;
import io.requery.android.LoggingListener;
import io.requery.meta.EntityModel;
import io.requery.sql.Configuration;
import io.requery.sql.ConfigurationBuilder;
import io.requery.sql.Mapping;
import io.requery.sql.Platform;
import io.requery.sql.SchemaModifier;
import io.requery.sql.TableCreationMode;
import io.requery.sql.platform.SQLite;
import io.requery.util.function.Function;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLNonTransientConnectionException;

/**
 * Wrapper for working with Android SQLite databases. This provides a {@link Connection} wrapping
 * the standard java SQLite API turning it into standard JDBC APIs.
 *
 * <p> This class extends the standard {@link SQLiteOpenHelper} and is used to create the database.
 * {@link #onCreate(SQLiteDatabase)} will create the database tables & columns automatically.
 *
 * <p> {@link #onUpgrade(SQLiteDatabase, int, int)} will create any missing tables/columns, however
 * it will not remove any tables/columns more complex upgrades should be handled by overriding
 * {@link #onUpgrade(SQLiteDatabase, int, int)} and implementing a script to handle the migration.
 *
 * @author Nikhil Purushe
 */
@SuppressWarnings("WeakerAccess")
public class DatabaseSource extends SQLiteOpenHelper implements DatabaseProvider<SQLiteDatabase> {

    private final Platform platform;
    private final EntityModel model;
    private Mapping mapping;
    private SQLiteDatabase db;
    private Configuration configuration;
    private boolean configured;
    private boolean loggingEnabled;
    private TableCreationMode mode;

    /**
     * Creates a new {@link DatabaseSource} instance.
     *
     * @param context context
     * @param model   the entity model
     * @param version the schema version
     */
    public DatabaseSource(Context context, EntityModel model, int version) {
        this(context, model, getDefaultDatabaseName(context, model), null, version);
    }

    /**
     * Creates a new {@link DatabaseSource} instance.
     *
     * @param context context
     * @param model   the entity model
     * @param name    database filename
     * @param version the schema version
     */
    public DatabaseSource(Context context, EntityModel model, @Nullable String name, int version) {
        this(context, model, name, null, version);
    }

    /**
     * Creates a new {@link DatabaseSource} instance.
     *
     * @param context context
     * @param model   the entity model
     * @param name    database filename
     * @param factory optional {@link android.database.sqlite.SQLiteDatabase.CursorFactory}
     * @param version the schema version
     */
    public DatabaseSource(Context context,
                          EntityModel model,
                          @Nullable String name,
                          @Nullable SQLiteDatabase.CursorFactory factory,
                          int version) {
        this(context, model, name, factory, version, new SQLite());
    }

    /**
     * Creates a new {@link DatabaseSource} instance.
     *
     * @param context context
     * @param model   the entity model
     * @param name    database filename
     * @param factory optional {@link android.database.sqlite.SQLiteDatabase.CursorFactory}
     * @param version the schema version
     * @param platform platform instance
     */
    public DatabaseSource(Context context,
                          EntityModel model,
                          @Nullable String name,
                          @Nullable SQLiteDatabase.CursorFactory factory,
                          int version,
                          SQLite platform) {
        super(context, name, factory, version);
        if (model == null) {
            throw new IllegalArgumentException("null model");
        }
        this.platform = platform;
        this.model = model;
        this.mode = TableCreationMode.CREATE_NOT_EXISTS;
    }

    @Override
    public void setLoggingEnabled(boolean enable) {
        this.loggingEnabled = enable;
    }

    @Override
    public void setTableCreationMode(TableCreationMode mode) {
        this.mode = mode;
    }

    private static String getDefaultDatabaseName(Context context, EntityModel model) {
        return TextUtils.isEmpty(model.getName()) ?
                context.getPackageName() : model.getName();
    }

    /**
     * Override to change the default {@link Mapping}.
     *
     * @param platform platform instance
     * @return the configured mapping.
     */
    protected Mapping onCreateMapping(Platform platform) {
        return new DefaultMapping(platform);
    }

    /**
     * Override to modify the configuration before it is built.
     *
     * @param builder {@link ConfigurationBuilder instance} to configure.
     */
    protected void onConfigure(ConfigurationBuilder builder) {
        if (loggingEnabled) {
            LoggingListener loggingListener = new LoggingListener();
            builder.addStatementListener(loggingListener);
        }
    }

    private Connection getConnection(SQLiteDatabase db) throws SQLException {
        synchronized (this) {
            if (!db.isOpen()) {
                throw new SQLNonTransientConnectionException();
            }
            return new SqliteConnection(db);
        }
    }

    @Override
    public Configuration getConfiguration() {
        if (mapping == null) {
            mapping = onCreateMapping(platform);
        }
        if (mapping == null) {
            throw new IllegalStateException();
        }
        if (configuration == null) {
            ConfigurationBuilder builder = new ConfigurationBuilder(this, model)
                .setMapping(mapping)
                .setPlatform(platform)
                .setBatchUpdateSize(1000);
            onConfigure(builder);
            configuration = builder.build();
        }
        return configuration;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        this.db = db;
        new SchemaModifier(getConfiguration()).createTables(TableCreationMode.CREATE);
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        if (!db.isReadOnly()) {
            db.setForeignKeyConstraintsEnabled(true);
        }
    }

    @Override
    public void onUpgrade(final SQLiteDatabase db, int oldVersion, int newVersion) {
        this.db = db;
        SchemaUpdater updater = new SchemaUpdater(getConfiguration(),
            new Function<String, Cursor>() {
            @Override
            public Cursor apply(String s) {
                return db.rawQuery(s, null);
            }
        }, mode);
        updater.update();
    }

    @Override
    public Connection getConnection() throws SQLException {
        synchronized (this) {
            if (db == null) {
                db = getWritableDatabase();
            }
            if (!configured && Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
                db.execSQL("PRAGMA foreign_keys = ON");
                long pageSize = db.getPageSize();
                // on later Android versions page size was correctly set to 4096,
                // do the same for older versions
                if (pageSize == 1024) {
                    db.setPageSize(4096);
                }
                configured = true;
            }
            return getConnection(db);
        }
    }
}
