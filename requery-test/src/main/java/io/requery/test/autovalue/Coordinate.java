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

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@AutoValue
@Embeddable
public abstract class Coordinate {

    @AutoValue.Builder
    public static abstract class Builder {
        public abstract Builder setLatitude(float latitude);
        public abstract Builder setLongitude(float longitude);
        public abstract Coordinate build();
    }

    public static Builder builder() {
        return new AutoValue_Coordinate.Builder();
    }

    @Column(nullable = false)
    public abstract float getLatitude();
    @Column(nullable = false)
    public abstract float getLongitude();
}
