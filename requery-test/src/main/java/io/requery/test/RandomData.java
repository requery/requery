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

package io.requery.test;

import io.requery.test.model.Address;
import io.requery.test.model.AddressType;
import io.requery.test.model.Person;
import io.requery.test.model.Phone;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

class RandomData {

    static Person randomPerson() {
        Random random = new Random();
        Person person = new Person();
        String[] firstNames = new String[]{"Alice", "Bob", "Carol"};
        String[] lastNames = new String[]{"Smith", "Lee", "Jones"};
        person.setName(firstNames[random.nextInt(firstNames.length)] + " " +
            lastNames[random.nextInt(lastNames.length)]);
        person.setEmail(person.getName().replaceAll(" ", "").toLowerCase() + "@example.com");
        person.setUUID(UUID.randomUUID());
        try {
            person.setHomepage(new URL("http://www.google.com"));
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
        Calendar calendar = Calendar.getInstance();
        //noinspection MagicConstant
        calendar.set(1900 + random.nextInt(90), random.nextInt(12), random.nextInt(30));
        person.setBirthday(calendar.getTime());
        return person;
    }

    static Set<Person> randomPersons(int count) {
        Set<Person> persons = new HashSet<>();
        for(int i = 0; i < count; i++) {
            persons.add(randomPerson());
        }
        return persons;
    }

    static Address randomAddress() {
        Random random = new Random();
        Address address = new Address();
        address.setLine1(random.nextInt(4) + " Fake St");
        address.setCity("San Francisco");
        address.setState("CA");
        address.setZip(String.valueOf(10000 + random.nextInt(70000)));
        address.setType(AddressType.HOME);
        address.setLatitude(0.0f);
        address.setLongitude(0.0f);
        return address;
    }

    static Phone randomPhone() {
        Phone phone = new Phone();
        phone.setPhoneNumber("555-" + (1000 + new Random().nextInt(8000)));
        phone.setNormalized(true);
        return phone;
    }

}
