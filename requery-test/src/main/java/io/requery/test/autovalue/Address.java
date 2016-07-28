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

package io.requery.test.autovalue;


import com.google.auto.value.AutoValue;

import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

@AutoValue
@Entity
public abstract class Address {

    @AutoValue.Builder
    public static abstract class Builder {
        public abstract Builder setId(int id);
        public abstract Builder setLine1(String line1);
        public abstract Builder setLine2(String line1);
        public abstract Builder setState(String state);
        public abstract Builder setCity(String city);
        public abstract Builder setCoordinate(Coordinate coordinate);
        public abstract Builder setCountry(String country);
        public abstract Builder setZip(String zip);
        public abstract Address build();
    }

    public static Builder builder() {
        return new AutoValue_Address.Builder().setId(-1);
    }

    public abstract Builder toBuilder();

    @Id @GeneratedValue
    public abstract int getId();

    public abstract String getLine1();
    public abstract String getLine2();
    public abstract String getState();

    @Embedded
    public abstract Coordinate getCoordinate();

    @Column(length = 5)
    public abstract String getZip();

    @Column(length = 2)
    public abstract String getCountry();

    public abstract String getCity();
}
