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

package io.requery.android.sqlcipher;

import android.content.Context;
import android.database.Cursor;
import io.requery.android.DefaultMapping;
import io.requery.android.LoggingListener;
import io.requery.android.sqlite.DatabaseProvider;
import io.requery.android.sqlite.SchemaUpdater;
import io.requery.meta.EntityModel;
import io.requery.sql.Configuration;
import io.requery.sql.ConfigurationBuilder;
import io.requery.sql.Mapping;
import io.requery.sql.Platform;
import io.requery.sql.SchemaModifier;
import io.requery.sql.TableCreationMode;
import io.requery.sql.platform.SQLite;
import io.requery.util.function.Function;
import net.sqlcipher.database.SQLiteDatabase;
import net.sqlcipher.database.SQLiteOpenHelper;

import java.sql.Connection;
import java.sql.SQLException;

public class SqlCipherDatabaseSource extends SQLiteOpenHelper implements
    DatabaseProvider<SQLiteDatabase> {

    private final Platform platform;
    private final EntityModel model;
    private final Mapping mapping;
    private final String password;
    private SQLiteDatabase db;
    private Configuration configuration;
    private boolean loggingEnabled;
    private TableCreationMode mode;

    public SqlCipherDatabaseSource(Context context,
                                   EntityModel model,
                                   String name,
                                   String password,
                                   int version) {
        super(context, name, null, version);
        if (model == null) {
            throw new IllegalArgumentException("null model");
        }
        this.platform = new SQLite();
        this.mapping = onCreateMapping(platform);
        this.model = model;
        this.password = password;
        this.mode = TableCreationMode.CREATE_NOT_EXISTS;
        SQLiteDatabase.loadLibs(context);
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
            return new SqlCipherConnection(db);
        }
    }

    @Override
    public Configuration getConfiguration() {
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
        if (!db.isReadOnly()) {
            db.execSQL("PRAGMA foreign_keys = ON");
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
                db = getWritableDatabase(password);
            }
            return getConnection(db);
        }
    }

    @Override
    public SQLiteDatabase getReadableDatabase() {
        return getReadableDatabase(password);
    }

    @Override
    public SQLiteDatabase getWritableDatabase() {
        return getWritableDatabase(password);
    }
}
