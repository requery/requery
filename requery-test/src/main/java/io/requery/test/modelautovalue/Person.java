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

package io.requery.test.modelautovalue;


import com.google.auto.value.AutoValue;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import java.io.Serializable;
import java.net.URL;
import java.util.Date;
import java.util.UUID;

@AutoValue
@Entity
public abstract class Person implements Serializable {

    @AutoValue.Builder
    public static abstract class Builder {
        public abstract Builder setId(int id);
        public abstract Builder setName(String name);
        public abstract Builder setEmail(String email);
        public abstract Builder setBirthday(Date date);
        public abstract Builder setAge(int age);
        public abstract Builder setAbout(String about);
        public abstract Builder setUUID(UUID uuid);
        public abstract Builder setHomepage(URL url);
        public abstract Person build();
    }

    public static Builder builder() {
        return new AutoValue_Person.Builder().setId(-1);
    }

    @Id @GeneratedValue
    public abstract int getId();

    public abstract String getName();
    public abstract String getEmail();
    public abstract Date getBirthday();
    @Column(nullable = true)
    public abstract int getAge();

    public abstract String getAbout();

    @Column(unique = true)
    public abstract UUID getUUID();

    public abstract URL getHomepage();
}
