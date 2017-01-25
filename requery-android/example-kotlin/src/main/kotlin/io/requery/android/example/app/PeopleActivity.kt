package io.requery.android.example.app

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.RecyclerView.ViewHolder
import android.view.*
import android.widget.ImageView
import android.widget.TextView
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import io.requery.Persistable
import io.requery.android.QueryRecyclerAdapter
import io.requery.android.example.app.model.Models
import io.requery.android.example.app.model.Person
import io.requery.android.example.app.model.PersonEntity
import io.requery.kotlin.lower
import io.requery.query.Result
import io.requery.sql.KotlinEntityDataStore
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Activity displaying a list of random people. You can tap on a person to edit their record.
 * Shows how to use a query with a [RecyclerView] and [QueryRecyclerAdapter] and RxJava
 */
class PeopleActivity : AppCompatActivity() {

    private lateinit var data: KotlinEntityDataStore<Persistable>
    private lateinit var executor: ExecutorService
    private lateinit var adapter: PersonAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.title = "People"
        setContentView(R.layout.activity_people)
        val recyclerView = findViewById(R.id.recyclerView) as RecyclerView
        data = (application as PeopleApplication).data
        executor = Executors.newSingleThreadExecutor()
        adapter = PersonAdapter()
        adapter.setExecutor(executor)
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(this)

        data.count(Person::class).get().single().subscribe { integer ->
            if (integer === 0) {
                Observable.fromCallable(CreatePeople(data))
                    .flatMap { o -> o }
                    .observeOn(Schedulers.computation())
                    .subscribe({ adapter.queryAsync() })
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_plus -> {
                val intent = Intent(this, PersonEditActivity::class.java)
                startActivity(intent)
                return true
            }
        }
        return false
    }

    override fun onResume() {
        adapter.queryAsync()
        super.onResume()
    }

    override fun onDestroy() {
        executor.shutdown()
        adapter.close()
        super.onDestroy()
    }

    internal inner class PersonHolder(itemView: View) : ViewHolder(itemView) {
        var image : ImageView? = null
        var name : TextView? = null
    }

    private inner class PersonAdapter internal constructor() :
            QueryRecyclerAdapter<Person, PersonHolder>(Models.DEFAULT, Person::class.java),
            View.OnClickListener {

        private val random = Random()
        private val colors = intArrayOf(Color.RED, Color.BLUE, Color.GREEN, Color.MAGENTA)

        override fun performQuery(): Result<Person> {
            return ( data select(Person::class) orderBy Person::name.lower() ).get()
        }

        override fun onBindViewHolder(item: Person, holder: PersonHolder, position: Int) {
            holder.name!!.text = item.name
            holder.image!!.setBackgroundColor(colors[random.nextInt(colors.size)])
            holder.itemView.tag = item
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PersonHolder {
            val inflater = LayoutInflater.from(parent.context)
            val view = inflater.inflate(R.layout.person_item, null)
            val holder = PersonHolder(view)
            holder.name = view.findViewById(R.id.name) as TextView
            holder.image = view.findViewById(R.id.picture) as ImageView
            view.setOnClickListener(this)
            return holder
        }

        override fun onClick(v: View) {
            val person = v.tag as PersonEntity?
            if (person != null) {
                val intent = Intent(this@PeopleActivity, PersonEditActivity::class.java)
                intent.putExtra(PersonEditActivity.Companion.EXTRA_PERSON_ID, person.id)
                startActivity(intent)
            }
        }
    }
}
