package io.requery.android.example.app

import android.app.Application
import android.os.StrictMode
import io.requery.Persistable
import io.requery.android.example.app.model.Models
import io.requery.android.sqlite.DatabaseSource
import io.requery.rx.RxSupport
import io.requery.rx.SingleEntityStore
import io.requery.sql.Configuration
import io.requery.sql.EntityDataStore
import io.requery.sql.TableCreationMode

class PeopleApplication : Application() {

    val data: SingleEntityStore<Persistable> by lazy {
        val source = DatabaseSource(this, Models.DEFAULT, 1)
        source.setTableCreationMode(TableCreationMode.DROP_CREATE)
        RxSupport.toReactiveStore(EntityDataStore<Persistable>(source.configuration))
    }
}
