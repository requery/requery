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

package io.requery.test.modeljpa;


import io.requery.query.MutableResult;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ForeignKey;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import java.io.Serializable;
import java.net.URL;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Entity
public interface Person extends Serializable {

    @Id @GeneratedValue
    int getId();

    String getName();
    //@Index(name = "email_index")
    String getEmail();
    Date getBirthday();
    @Column(nullable = true)
    int getAge();

    @OneToOne
    @JoinColumn(foreignKey = @ForeignKey)
    Address getAddress();

    @OneToMany(mappedBy = "owner", cascade =
            {CascadeType.REMOVE, CascadeType.PERSIST})
    MutableResult<Phone> getPhoneNumbers();

    @OneToMany(mappedBy = "owner")
    Set<Phone> getPhoneNumbersSet();

    @OneToMany
    List<Phone> getPhoneNumbersList();

    @ManyToMany(mappedBy = "persons")
    MutableResult<Group> getGroups();

    String getAbout();

    @Column(unique = true)
    UUID getUUID();

    URL getHomepage();
}
