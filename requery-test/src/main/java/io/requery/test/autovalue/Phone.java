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

import javax.persistence.Entity;
import javax.persistence.ForeignKey;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;

@Entity
@AutoValue
public abstract class Phone {

    public static Phone create(int id, String phoneNumber, boolean normalized, int ownerId) {
        return new AutoValue_Phone(id, phoneNumber, normalized, ownerId);
    }

    @Id @GeneratedValue
    public abstract int getId();
    public abstract String getPhoneNumber();
    public abstract boolean isNormalized();

    @JoinColumn(foreignKey = @ForeignKey, table = "Person")
    public abstract int getOwnerId();

    // this method should not be processed
    public boolean isValid() {
        return getPhoneNumber() != null && isNormalized();
    }
}
