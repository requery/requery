package io.requery.android.example.app.model

import android.os.Parcelable
import io.requery.*

@Entity
interface Phone : Parcelable, Persistable {
    @get:Key
    @get:Generated
    val id: Int

    var phoneNumber: String

    @get:ManyToOne
    var owner: Person
}
