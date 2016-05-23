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

package io.requery.sql;

import io.requery.meta.Attribute;
import io.requery.meta.QueryAttribute;
import io.requery.util.function.Predicate;
import io.requery.util.function.Supplier;

import java.util.Collection;
import java.util.LinkedHashSet;

@SuppressWarnings("unchecked")
final class Attributes {

    static <E, V> QueryAttribute<E, V> query(Attribute attribute) {
        if (attribute instanceof Supplier) {
            return get((Supplier) attribute);
        }
        return (QueryAttribute<E, V>) attribute;
    }

    static <E, V> QueryAttribute<E, V> get(Supplier supplier) {
        return (QueryAttribute<E, V>) supplier.get();
    }

    static <E> Attribute<E, ?>[] newArray(int size) {
        return new Attribute[size];
    }

    static <E> Attribute<E, ?>[] attributesToArray(Collection<Attribute<E, ?>> attributes,
                                                   Predicate<Attribute<E, ?>> filter) {
        LinkedHashSet<Attribute> filtered = new LinkedHashSet<>();
        for (Attribute<E, ?> attribute : attributes) {
            if (filter == null || filter.test(attribute)) {
                filtered.add(attribute);
            }
        }
        Attribute<E, ?>[] array = new Attribute[filtered.size()];
        return filtered.toArray(array);
    }

    static Object replaceForeignKeyReference(Object value, Attribute attribute) {
        if (value != null) {
            Attribute<Object, Object> referenced = get(attribute.referencedAttribute());
            value = referenced.declaringType().proxyProvider().apply(value).get(referenced);
        }
        return value;
    }
}
