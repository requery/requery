package io.requery.android.example.app

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.requery.Persistable
import io.requery.android.example.app.model.*
import io.requery.sql.KotlinEntityDataStore

/**
 * Simple activity allowing you to edit a Person entity using data binding.
 */
class PersonEditActivity : AppCompatActivity() {

    private lateinit var data: KotlinEntityDataStore<Persistable>
    private lateinit var person: Person

    companion object {
        internal val EXTRA_PERSON_ID = "personId"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_person)
        if (supportActionBar != null) {
            supportActionBar!!.title = "Edit Person"
        }
        data = (application as PeopleApplication).data
        val personId = intent.getIntExtra(EXTRA_PERSON_ID, -1)
        if (personId == -1) {
            person = PersonEntity() // creating a new person
            setPerson(person)
        } else {
            Observable.fromCallable {
                data.findByKey(PersonEntity::class, personId)
            } .subscribeOn(AndroidSchedulers.mainThread())
              .subscribe { person -> setPerson(person) }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_edit, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_save -> {
                savePerson()
                return true
            }
        }
        return false
    }

    private fun setPerson(p : Person) {
        this.person = p
        setViewText(R.id.name, p.name)
        setViewText(R.id.email, p.email)

        if (!p.phoneNumbers.isEmpty()) {
            setViewText(R.id.phone, p.phoneNumbers.iterator().next().phoneNumber)
        }
        if (p.address != null) {
            setViewText(R.id.street, p.address!!.line1)
            setViewText(R.id.city, p.address!!.line2)
            setViewText(R.id.zip, p.address!!.zip)
            setViewText(R.id.state, p.address!!.state)
        }
    }

    private fun getViewText(id : Int) : String {
        return (findViewById(id) as TextView).text.toString()
    }

    private fun setViewText(id : Int, text : String) {
        (findViewById(id) as TextView).text = text
    }

    private fun savePerson() {
        person.name = getViewText(R.id.name)
        person.email = getViewText(R.id.email)
        val phone: Phone
        if (person.phoneNumbers.isEmpty()) {
            phone = PhoneEntity()
            phone.owner = person
            person.phoneNumbers.add(phone)
        } else {
            phone = person.phoneNumbers.iterator().next()
        }
        phone.phoneNumber = (findViewById(R.id.phone) as TextView).text.toString()
        var address = person.address
        if (address == null) {
            address = AddressEntity()
            person.address = address
        }
        address.line1 = getViewText(R.id.street)
        address.line2 = getViewText(R.id.city)
        address.zip = getViewText(R.id.zip)
        address.state = getViewText(R.id.state)
        // save the person
        if (person.id === 0) {
            Observable.fromCallable { data.insert(person) }.subscribe({ finish() })
        } else {
            Observable.fromCallable { data.update(person) }.subscribe({ finish() })
        }
    }
}
