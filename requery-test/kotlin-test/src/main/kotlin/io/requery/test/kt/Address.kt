package io.requery.test.kt

import io.requery.*

@Entity(model = "kt")
interface Address {

    companion object {
        const val CONSTANT = "value"
    }

    @get:Key
    @get:Generated
    var id: Int
    var line1: String
    var line2: String
    var state: String

    @get:Column(length = 5)
    var zip: String

    @get:Column(length = 2)
    var country: String

    var city: String

    @get:OneToOne(mappedBy = "address")
    val person: Person

    @get:Transient
    var description: String
}
