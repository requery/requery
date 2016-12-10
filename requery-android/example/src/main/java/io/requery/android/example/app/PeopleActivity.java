/*
 * Copyright 2016 requery.io
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.requery.android.example.app;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import io.reactivex.Observable;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import io.requery.Persistable;
import io.requery.android.QueryRecyclerAdapter;
import io.requery.android.example.app.databinding.PersonItemBinding;
import io.requery.android.example.app.model.Person;
import io.requery.android.example.app.model.PersonEntity;
import io.requery.query.Result;
import io.requery.reactivex.ReactiveEntityStore;

import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Activity displaying a list of random people. You can tap on a person to edit their record.
 * Shows how to use a query with a {@link RecyclerView} and {@link QueryRecyclerAdapter} and RxJava
 */
public class PeopleActivity extends AppCompatActivity {

    private ReactiveEntityStore<Persistable> data;
    private ExecutorService executor;
    private PersonAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("People");
        }
        setContentView(R.layout.activity_people);
        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.recyclerView);
        data = ((PeopleApplication) getApplication()).getData();
        executor = Executors.newSingleThreadExecutor();
        adapter = new PersonAdapter();
        adapter.setExecutor(executor);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        data.count(Person.class).get().single()
            .subscribe(new Consumer<Integer>() {
            @Override
            public void accept(Integer integer) {
                if (integer == 0) {
                    Observable.fromCallable(new CreatePeople(data))
                        .flatMap(new Function<Observable<Iterable<Person>>, Observable<?>>() {
                            @Override
                            public Observable<?> apply(Observable<Iterable<Person>> o) {
                                return o;
                            }
                        })
                        .observeOn(Schedulers.computation())
                        .subscribe(new Consumer<Object>() {
                            @Override
                            public void accept(Object o) {
                                adapter.queryAsync();
                            }
                        });
                }
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_plus:
                Intent intent = new Intent(this, PersonEditActivity.class);
                startActivity(intent);
                return true;
        }
        return false;
    }

    @Override
    protected void onResume() {
        adapter.queryAsync();
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        executor.shutdown();
        adapter.close();
        super.onDestroy();
    }

    private class PersonAdapter extends QueryRecyclerAdapter<PersonEntity,
            BindingHolder<PersonItemBinding>> implements View.OnClickListener {

        private final Random random = new Random();
        private final int[] colors = { Color.RED, Color.BLUE, Color.GREEN, Color.MAGENTA };

        PersonAdapter() {
            super(PersonEntity.$TYPE);
        }

        @Override
        public Result<PersonEntity> performQuery() {
            // this is all persons in the db sorted by their name
            // note this method in executed in a background thread.
            // (Alternatively RxJava w/ RxBinding could be used)
            return data.select(PersonEntity.class).orderBy(PersonEntity.NAME.lower()).get();
        }

        @Override
        public void onBindViewHolder(PersonEntity item, BindingHolder<PersonItemBinding> holder,
                                     int position) {
            holder.binding.setPerson(item);
            holder.binding.picture.setBackgroundColor(colors[random.nextInt(colors.length)]);
        }

        @Override
        public BindingHolder<PersonItemBinding> onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            PersonItemBinding binding = PersonItemBinding.inflate(inflater);
            binding.getRoot().setTag(binding);
            binding.getRoot().setOnClickListener(this);
            return new BindingHolder<>(binding);
        }

        @Override
        public void onClick(View v) {
            PersonItemBinding binding = (PersonItemBinding) v.getTag();
            if (binding != null) {
                Intent intent = new Intent(PeopleActivity.this, PersonEditActivity.class);
                intent.putExtra(PersonEditActivity.EXTRA_PERSON_ID, binding.getPerson().getId());
                startActivity(intent);
            }
        }
    }
}
