package io.requery.android.example.app

import io.reactivex.Observable
import io.requery.Persistable
import io.requery.android.example.app.model.AddressEntity
import io.requery.android.example.app.model.Person
import io.requery.android.example.app.model.PersonEntity
import io.requery.sql.KotlinEntityDataStore
import java.util.*
import java.util.concurrent.Callable

class CreatePeople(val data: KotlinEntityDataStore<Persistable>) : Callable<Observable<Iterable<Person>>> {

    override fun call(): Observable<Iterable<Person>> {
        val firstNames = arrayOf("Alice", "Bob", "Carol", "Chloe", "Dan", "Emily", "Emma", "Eric",
                "Eva", "Frank", "Gary", "Helen", "Jack", "James", "Jane", "Kevin", "Laura", "Leon",
                "Lilly", "Mary", "Maria", "Mia", "Nick", "Oliver", "Olivia", "Patrick", "Robert",
                "Stan", "Vivian", "Wesley", "Zoe")
        val lastNames = arrayOf("Hall", "Hill", "Smith", "Lee", "Jones", "Taylor", "Williams",
                "Jackson", "Stone", "Brown", "Thomas", "Clark", "Lewis", "Miller", "Walker", "Fox",
                "Robinson", "Wilson", "Cook", "Carter", "Cooper", "Martin")
        val random = Random()

        val people = TreeSet(Comparator<Person> { lhs, rhs -> lhs.name.compareTo(rhs.name) })
        // creating many people (but only with unique names)

        for (i in 0..2999) {
            val person = PersonEntity()
            val first = firstNames[random.nextInt(firstNames.size)]
            val last = lastNames[random.nextInt(lastNames.size)]
            person.name = first + " " + last
            person.uuid = UUID.randomUUID()
            person.email = Character.toLowerCase(first[0]) + last.toLowerCase() + "@gmail.com"

            val address = AddressEntity()
            address.line1 = "123 Market St"
            address.zip = "94105"
            address.city = "San Francisco"
            address.state = "CA"
            address.country = "US"
            person.address = address
            people.add(person)
        }
        return Observable.fromCallable { data.insert(people) }
    }
}
