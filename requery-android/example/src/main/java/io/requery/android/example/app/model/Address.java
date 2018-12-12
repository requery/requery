/*
 * Copyright 2018 requery.io
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

package io.requery.android.example.app.model;


import androidx.databinding.Bindable;
import androidx.databinding.Observable;
import android.os.Parcelable;
import io.requery.Entity;
import io.requery.Generated;
import io.requery.Key;
import io.requery.OneToOne;
import io.requery.Persistable;

@Entity
public interface Address extends Observable, Parcelable, Persistable {

    @Key
    @Generated
    int getId();

    @Bindable
    String getLine1();

    void setLine1(String line1);

    @Bindable
    String getLine2();

    void setLine2(String line2);

    @Bindable
    String getZip();

    void setZip(String zip);

    @Bindable
    String getCountry();

    void setCountry(String country);

    @Bindable
    String getCity();

    void setCity(String city);

    @Bindable
    String getState();

    void setState(String state);

    @Bindable
    @OneToOne(mappedBy = "address")
    Person getPerson();
}
