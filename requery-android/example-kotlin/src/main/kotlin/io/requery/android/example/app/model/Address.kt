
package io.requery.android.example.app.model

import android.os.Parcelable
import io.requery.*

@Entity
interface Address : Parcelable, Persistable {
    @get:Key
    @get:Generated
    val id: Int

    var line1: String

    var line2: String

    var zip: String

    var country: String

    var city: String

    var state: String

    @get:OneToOne(mappedBy = "address")
    val person: Person
}
