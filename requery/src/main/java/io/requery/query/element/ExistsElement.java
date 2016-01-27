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

package io.requery.query.element;

import io.requery.query.Return;
import io.requery.query.Exists;
import io.requery.util.Objects;

/**
 * Exists query element.
 *
 * @param <E> result type
 */
public class ExistsElement<E> implements Exists<E> {

    private final E parent;
    private Return<?> subQuery;
    private boolean notExists;

    ExistsElement(E query) {
        this.parent = query;
    }

    @Override
    public E exists(Return<?> query) {
        subQuery = Objects.requireNotNull(query);
        return parent;
    }

    @Override
    public E notExists(Return<?> query) {
        notExists = true;
        subQuery = Objects.requireNotNull(query);
        return parent;
    }

    public Return<?> getQuery() {
        return subQuery;
    }

    public boolean isNotExists() {
        return notExists;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ExistsElement) {
            ExistsElement other = (ExistsElement) obj;
            return parent == other.parent && notExists == other.notExists;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(parent, notExists);
    }
}
