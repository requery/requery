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

package io.requery.android.sqlite;

import android.annotation.TargetApi;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.location.Location;
import android.os.Build;
import android.os.ParcelUuid;
import android.text.TextUtils;
import io.requery.android.LoggingListener;
import io.requery.android.ParcelConverter;
import io.requery.android.UriConverter;
import io.requery.meta.Attribute;
import io.requery.meta.EntityModel;
import io.requery.meta.Type;
import io.requery.sql.Configuration;
import io.requery.sql.ConfigurationBuilder;
import io.requery.sql.ConnectionProvider;
import io.requery.sql.GenericMapping;
import io.requery.sql.Mapping;
import io.requery.sql.Platform;
import io.requery.sql.SchemaModifier;
import io.requery.sql.TableCreationMode;
import io.requery.sql.platform.SQLite;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLNonTransientConnectionException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
public class DatabaseSource extends SQLiteOpenHelper implements ConnectionProvider {

    private final Platform platform;
    private final EntityModel model;
    private final Mapping mapping;
    private SQLiteDatabase db;
    private Configuration configuration;
    private boolean configured;
    private boolean loggingEnabled;

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
    public DatabaseSource(Context context, EntityModel model, String name, int version) {
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
                          String name,
                          SQLiteDatabase.CursorFactory factory,
                          int version) {
        super(context, name, factory, version);
        if (model == null) {
            throw new IllegalArgumentException("null model");
        }
        this.platform = new SQLite();
        this.mapping = onCreateMapping();
        this.model = model;
    }

    /**
     * Enables statement logging. Not use for debugging only as it impacts performance.
     *
     * @param enable true to enable, false otherwise default is false.
     */
    public void setLoggingEnabled(boolean enable) {
        this.loggingEnabled = enable;
    }

    private static String getDefaultDatabaseName(Context context, EntityModel model) {
        return TextUtils.isEmpty(model.name()) ?
                context.getPackageName() : model.name();
    }

    /**
     * Override to change the default {@link Mapping}.
     *
     * @return the configured mapping.
     */
    protected Mapping onCreateMapping() {
        GenericMapping mapping = new GenericMapping(platform);
        mapping.addConverter(new UriConverter());
        mapping.addConverter(new ParcelConverter<>(ParcelUuid.class, ParcelUuid.CREATOR));
        mapping.addConverter(new ParcelConverter<>(Location.class, Location.CREATOR));
        return mapping;
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
            return new DatabaseConnection(db);
        }
    }

    public Configuration getConfiguration() {
        if (configuration == null) {
            ConfigurationBuilder builder = new ConfigurationBuilder(this, model)
                .setMapping(mapping)
                .setPlatform(platform)
                .setStatementCacheSize(0)
                .setQuoteColumnNames(false)
                .setQuoteTableNames(false);
            onConfigure(builder);
            configuration = builder.build();

            if (!(configuration.platform() instanceof SQLite)) {
                throw new IllegalStateException();
            }
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
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        this.db = db;
        SchemaModifier schema = new SchemaModifier(getConfiguration());
        schema.createTables(TableCreationMode.CREATE_NOT_EXISTS);
        // check for missing columns
        List<Attribute> missingAttributes = new ArrayList<>();
        for (Type<?> type : model.allTypes()) {
            String tableName = type.name();
            Cursor cursor = db.rawQuery("PRAGMA table_info(" + tableName + ")", null);
            Map<String, Attribute> map = new LinkedHashMap<>();
            for (Attribute attribute : type.attributes()) {
                if (attribute.isAssociation() && !attribute.isForeignKey()) {
                    continue;
                }
                map.put(attribute.name(), attribute);
            }
            if (cursor.getCount() > 0) {
                int nameIndex = cursor.getColumnIndex("name");
                while (cursor.moveToNext()) {
                    String name = cursor.getString(nameIndex);
                    map.remove(name);
                }
            }
            cursor.close();
            // whats left in the map are are the missing columns for this type
            missingAttributes.addAll(map.values());
        }
        // foreign keys are created last
        Collections.sort(missingAttributes, new Comparator<Attribute>() {
            @Override
            public int compare(Attribute lhs, Attribute rhs) {
                if (lhs.isForeignKey() && rhs.isForeignKey()) {
                    return 0;
                }
                if (lhs.isForeignKey()) {
                    return 1;
                }
                return -1;
            }
        });
        for (Attribute<?, ?> attribute : missingAttributes) {
            schema.addColumn(attribute);
        }
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
