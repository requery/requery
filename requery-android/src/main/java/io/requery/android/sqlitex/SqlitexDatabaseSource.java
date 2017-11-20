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

package io.requery.android.sqlitex;

import android.content.Context;
import android.database.Cursor;
import android.text.TextUtils;
import io.requery.android.DefaultMapping;
import io.requery.android.LoggingListener;
import io.requery.android.database.sqlite.SQLiteDatabase;
import io.requery.android.database.sqlite.SQLiteOpenHelper;
import io.requery.android.sqlite.DatabaseProvider;
import io.requery.android.sqlite.SchemaUpdater;
import io.requery.meta.EntityModel;
import io.requery.sql.ConfigurationBuilder;
import io.requery.sql.Mapping;
import io.requery.sql.Platform;
import io.requery.sql.SchemaModifier;
import io.requery.sql.TableCreationMode;
import io.requery.sql.platform.SQLite;
import io.requery.util.function.Function;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Wrapper for working with Android SQLite databases. That use the
 * <a href="https://github.com/requery/sqlite-android">requery Android SQLite support library</a>.
 *
 * <p> This class extends the support version of {@link SQLiteOpenHelper} and is used to create the
 * database. {@link #onCreate(SQLiteDatabase)} will create the database tables & columns
 * automatically.
 *
 * <p> {@link #onUpgrade(SQLiteDatabase, int, int)} will create any missing tables/columns, however
 * it will not remove any tables/columns more complex upgrades should be handled by overriding
 * {@link #onUpgrade(SQLiteDatabase, int, int)} and implementing a script to handle the migration.
 *
 * @author Nikhil Purushe
 */
public class SqlitexDatabaseSource extends SQLiteOpenHelper implements
    DatabaseProvider<SQLiteDatabase> {

    private final Platform platform;
    private final EntityModel model;
    private Mapping mapping;
    private SQLiteDatabase db;
    private io.requery.sql.Configuration configuration;
    private boolean loggingEnabled;
    private TableCreationMode mode;

    public SqlitexDatabaseSource(Context context, EntityModel model, int version) {
        this(context, model, getDefaultDatabaseName(context, model), version);
    }

    public SqlitexDatabaseSource(Context context,
                                 EntityModel model,
                                 String name,
                                 int version) {
        super(context, name, null, version);
        if (model == null) {
            throw new IllegalArgumentException("null model");
        }
        this.platform = new SQLite();
        this.model = model;
        this.mode = TableCreationMode.CREATE_NOT_EXISTS;
    }

    private static String getDefaultDatabaseName(Context context, EntityModel model) {
        return TextUtils.isEmpty(model.getName()) ?
            context.getPackageName() : model.getName();
    }

    @Override
    public void setLoggingEnabled(boolean enable) {
        this.loggingEnabled = enable;
    }

    @Override
    public void setTableCreationMode(TableCreationMode mode) {
        this.mode = mode;
    }

    protected Mapping onCreateMapping(Platform platform) {
        return new DefaultMapping(platform);
    }

    protected void onConfigure(ConfigurationBuilder builder) {
        if (loggingEnabled) {
            LoggingListener loggingListener = new LoggingListener();
            builder.addStatementListener(loggingListener);
        }
    }

    private Connection getConnection(SQLiteDatabase db) throws SQLException {
        synchronized (this) {
            return new SqlitexConnection(db);
        }
    }

    @Override
    public io.requery.sql.Configuration getConfiguration() {
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

    @Override
    public void onConfigure(SQLiteDatabase db) {
        db.setForeignKeyConstraintsEnabled(true);
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
            return getConnection(db);
        }
    }
}
