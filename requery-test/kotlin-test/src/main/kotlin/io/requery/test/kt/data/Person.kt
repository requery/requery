package io.requery.test.kt.data

import io.requery.*
import java.net.URL
import java.util.Date
import java.util.UUID

@Entity(model = "ktdata")
data class Person (

    @get:Key
    var id: Int,
    var name: String,
    var email: String,
    var birthday: Date,
    var age: Int,

    var about: String,

    @get:Column(unique = true)
    val uuid: UUID,
    val homepage: URL,
    val picture: String
)
