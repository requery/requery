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

import io.requery.EntityCache;
import io.requery.meta.Type;
import io.requery.proxy.EntityProxy;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

class TransactionEntitiesSet extends LinkedHashSet<EntityProxy<?>> {

    private final EntityCache cache;
    private final Set<Type<?>> types;

    TransactionEntitiesSet(EntityCache cache) {
        this.cache = cache;
        this.types = new HashSet<>();
    }

    @Override
    public boolean add(EntityProxy<?> proxy) {
        if (super.add(proxy)) {
            types.add(proxy.type());
            return true;
        }
        return false;
    }

    @Override
    public void clear() {
        super.clear();
        types.clear();
    }

    void clearAndInvalidate() {
        for (EntityProxy<?> proxy : this) {
            proxy.unlink();
            Object key = proxy.key();
            if (key != null) {
                cache.invalidate(proxy.type().getClassType(), key);
            }
        }
        clear();
    }

    Set<Type<?>> types() {
        return types;
    }
}
