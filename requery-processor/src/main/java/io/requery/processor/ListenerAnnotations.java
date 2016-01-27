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

package io.requery.processor;

import io.requery.PostDelete;
import io.requery.PostInsert;
import io.requery.PostLoad;
import io.requery.PostUpdate;
import io.requery.PreDelete;
import io.requery.PreInsert;
import io.requery.PreUpdate;

import java.lang.annotation.Annotation;
import java.util.stream.Stream;

class ListenerAnnotations {
    private ListenerAnnotations() {
    }

    static Stream<Class<? extends Annotation>> all() {
        return Stream.concat(annotationClasses(), jpaAnnotationClasses());
    }

    static Stream<Class<? extends Annotation>> jpaAnnotationClasses() {
        return Stream.of(
            javax.persistence.PostLoad.class,
            javax.persistence.PostPersist.class,
            javax.persistence.PostRemove.class,
            javax.persistence.PostUpdate.class,
            javax.persistence.PrePersist.class,
            javax.persistence.PreRemove.class,
            javax.persistence.PreUpdate.class);
    }

    static Stream<Class<? extends Annotation>> annotationClasses() {
        return Stream.of(
            PostLoad.class,
            PostInsert.class,
            PostDelete.class,
            PostUpdate.class,
            PreInsert.class,
            PreDelete.class,
            PreUpdate.class);
    }
}
