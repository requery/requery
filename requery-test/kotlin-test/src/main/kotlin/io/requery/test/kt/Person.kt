package io.requery.test.kt

import io.requery.*
import java.net.URL
import java.util.Date
import java.util.UUID

@Entity(model = "kt")
interface Person {

    @get:Key
    @get:Generated
    var id: Int
    var name: String
    var email: String
    var birthday: Date
    var age: Int

    @get:ForeignKey
    @get:OneToOne
    var address: Address

    @get:ManyToMany(mappedBy = "members")
    var groups: Set<Group>

    var about: String

    @get:Column(unique = true)
    var uuid: UUID
    var homepage: URL
    var picture: String
}
