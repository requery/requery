package io.requery.android.example.app

import android.app.Application
import io.requery.Persistable
import io.requery.android.example.app.model.Models
import io.requery.android.sqlite.DatabaseSource
import io.requery.reactivex.KotlinReactiveEntityStore
import io.requery.sql.KotlinEntityDataStore
import io.requery.sql.TableCreationMode

class PeopleApplication : Application() {

    val data: KotlinReactiveEntityStore<Persistable> by lazy {
        val source = DatabaseSource(this, Models.DEFAULT, 1)
        source.setTableCreationMode(TableCreationMode.DROP_CREATE)
        KotlinReactiveEntityStore<Persistable>(KotlinEntityDataStore(source.configuration))
    }
}
