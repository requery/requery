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

import io.reactivex.Observable;
import io.requery.Persistable;
import io.requery.android.example.app.model.AddressEntity;
import io.requery.android.example.app.model.Person;
import io.requery.android.example.app.model.PersonEntity;
import io.requery.reactivex.ReactiveEntityStore;

import java.util.Comparator;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.Callable;

class CreatePeople implements Callable<Observable<Iterable<Person>>> {

    private final ReactiveEntityStore<Persistable> data;

    CreatePeople(ReactiveEntityStore<Persistable> data) {
        this.data = data;
    }

    @Override
    public Observable<Iterable<Person>> call() {
        String[] firstNames = new String[]{
                "Alice", "Bob", "Carol", "Chloe", "Dan", "Emily", "Emma", "Eric", "Eva",
                "Frank", "Gary", "Helen", "Jack", "James", "Jane",
                "Kevin", "Laura", "Leon", "Lilly", "Mary", "Maria",
                "Mia", "Nick", "Oliver", "Olivia", "Patrick", "Robert",
                "Stan", "Vivian", "Wesley", "Zoe"};
        String[] lastNames = new String[]{
                "Hall", "Hill", "Smith", "Lee", "Jones", "Taylor", "Williams", "Jackson",
                "Stone", "Brown", "Thomas", "Clark", "Lewis", "Miller", "Walker", "Fox",
                "Robinson", "Wilson", "Cook", "Carter", "Cooper", "Martin" };
        Random random = new Random();

        final Set<Person> people = new TreeSet<>(new Comparator<Person>() {
            @Override
            public int compare(Person lhs, Person rhs) {
                return lhs.getName().compareTo(rhs.getName());
            }
        });
        // creating many people (but only with unique names)
        for (int i = 0; i < 3000; i++) {
            PersonEntity person = new PersonEntity();
            String first = firstNames[random.nextInt(firstNames.length)];
            String last = lastNames[random.nextInt(lastNames.length)];
            person.setName(first + " " + last);
            person.setUUID(UUID.randomUUID());
            person.setEmail(Character.toLowerCase(first.charAt(0)) +
                    last.toLowerCase() + "@gmail.com");
            AddressEntity address = new AddressEntity();
            address.setLine1("123 Market St");
            address.setZip("94105");
            address.setCity("San Francisco");
            address.setState("CA");
            address.setCountry("US");
            person.setAddress(address);
            people.add(person);
        }
        return data.insert(people).toObservable();
    }
}
