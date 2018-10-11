package io.requery.test.kt

import io.requery.*

@Entity(model = "kt")
@Table(name = "Groups")
interface Group {

    @get:Key
    @get:Generated
    val id: Int
    @get:Key
    @get:Column(unique = true)
    var name: String
    var description: String
    var picture: ByteArray

    @get:JunctionTable
    @get:ManyToMany
    val members: MutableSet<Person>
}
