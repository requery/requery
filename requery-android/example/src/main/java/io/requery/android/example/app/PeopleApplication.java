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

package io.requery.android.example.app;

import android.app.Application;
import android.os.StrictMode;
import io.requery.Persistable;
import io.requery.android.example.app.model.Models;
import io.requery.android.sqlite.DatabaseSource;
import io.requery.reactivex.ReactiveSupport;
import io.requery.reactivex.ReactiveEntityStore;
import io.requery.sql.Configuration;
import io.requery.sql.EntityDataStore;
import io.requery.sql.TableCreationMode;

public class PeopleApplication extends Application {

    private ReactiveEntityStore<Persistable> dataStore;

    @Override
    public void onCreate() {
        super.onCreate();
        StrictMode.enableDefaults();
    }

    /**
     * @return {@link EntityDataStore} single instance for the application.
     * <p/>
     * Note if you're using Dagger you can make this part of your application level module returning
     * {@code @Provides @Singleton}.
     */
    ReactiveEntityStore<Persistable> getData() {
        if (dataStore == null) {
            // override onUpgrade to handle migrating to a new version
            DatabaseSource source = new DatabaseSource(this, Models.DEFAULT, 1);
            if (BuildConfig.DEBUG) {
                // use this in development mode to drop and recreate the tables on every upgrade
                source.setTableCreationMode(TableCreationMode.DROP_CREATE);
            }
            Configuration configuration = source.getConfiguration();
            dataStore = ReactiveSupport.toReactiveStore(
                new EntityDataStore<Persistable>(configuration));
        }
        return dataStore;
    }
}
