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
import io.requery.android.sqlite.SchemaUpdater;
import io.requery.meta.EntityModel;
import io.requery.sql.Configuration;
import io.requery.sql.ConfigurationBuilder;
import io.requery.sql.ConnectionProvider;
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

public class SecureDatabaseSource extends SQLiteOpenHelper implements ConnectionProvider {

    private final Platform platform;
    private final EntityModel model;
    private final Mapping mapping;
    private final String password;
    private SQLiteDatabase db;
    private Configuration configuration;
    private boolean loggingEnabled;

    public SecureDatabaseSource(Context context,
                                EntityModel model,
                                String name,
                                String password,
                                int version) {
        super(context, name, null, version);
        if (model == null) {
            throw new IllegalArgumentException("null model");
        }
        this.platform = new SQLite();
        this.mapping = onCreateMapping();
        this.model = model;
        this.password = password;
        SQLiteDatabase.loadLibs(context);
    }

    public void setLoggingEnabled(boolean enable) {
        this.loggingEnabled = enable;
    }

    protected Mapping onCreateMapping() {
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
    public void onUpgrade(final SQLiteDatabase db, int oldVersion, int newVersion) {
        this.db = db;
        SchemaUpdater updater = new SchemaUpdater(getConfiguration(),
            new Function<String, Cursor>() {
            @Override
            public Cursor apply(String s) {
                return db.rawQuery(s, null);
            }
        });
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
}
