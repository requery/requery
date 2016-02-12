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

package io.requery.meta;

import io.requery.ManyToMany;
import io.requery.ManyToOne;
import io.requery.OneToMany;
import io.requery.OneToOne;

import java.lang.annotation.Annotation;

public enum Cardinality {
    ONE_TO_ONE,
    ONE_TO_MANY,
    MANY_TO_ONE,
    MANY_TO_MANY;

    public Class<? extends Annotation> annotationClass() {
        switch (this) {
            case ONE_TO_ONE:
                return OneToOne.class;
            case ONE_TO_MANY:
                return OneToMany.class;
            case MANY_TO_ONE:
                return ManyToOne.class;
            case MANY_TO_MANY:
                return ManyToMany.class;
            default:
                throw new IllegalStateException();
        }
    }
}
