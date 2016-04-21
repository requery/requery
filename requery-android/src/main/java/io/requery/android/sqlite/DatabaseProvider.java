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

import io.requery.sql.Configuration;
import io.requery.sql.ConnectionProvider;
import io.requery.sql.TableCreationMode;

/**
 * Common interface for Android database providers.
 *
 * @param <T> database type
 */
public interface DatabaseProvider<T> extends ConnectionProvider {

    /**
     * Enables statement logging. Not use for debugging only as it impacts performance.
     *
     * @param enable true to enable, false otherwise default is false.
     */
    void setLoggingEnabled(boolean enable);

    /**
     * Sets the {@link TableCreationMode} to use when the database is created or upgraded.
     *
     * @param mode to use
     */
    void setTableCreationMode(TableCreationMode mode);

    /**
     * @return {@link Configuration} used by the provider
     */
    Configuration getConfiguration();

    /**
     * Callback for when the database schema is to be created.
     *
     * @param db instance
     */
    void onCreate(T db);

    /**
     * Callback for when the database should be configured.
     *
     * @param db instance
     */
    void onConfigure(T db);

    /**
     * Callback for when the database should be upgraded from an previous version to a new version.
     *
     * @param db instance
     */
    void onUpgrade(T db, int oldVersion, int newVersion);

    /**
     * @return read only database instance
     */
    T getReadableDatabase();

    /**
     * @return readable and writable database instance
     */
    T getWritableDatabase();
}
