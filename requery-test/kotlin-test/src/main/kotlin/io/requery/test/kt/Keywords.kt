package io.requery.test.kt

import io.requery.Entity
import io.requery.Generated
import io.requery.Key

/**
 * This class is a test itself. If project compilation succeeds, it means that forbidden names
 * (e.g. java keywords) are processed properly.
 */
@Entity
interface Keywords {
    @get:Key
    @get:Generated
    var id: Int

    var isNotAJvmKeyword: String

    var isNew: Boolean
    var isDefault: String
    var getAbstract: String
}