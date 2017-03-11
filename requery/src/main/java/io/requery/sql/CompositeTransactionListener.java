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

import io.requery.TransactionIsolation;
import io.requery.TransactionListener;
import io.requery.meta.Type;
import io.requery.util.function.Supplier;

import java.util.HashSet;
import java.util.Set;

class CompositeTransactionListener extends HashSet<TransactionListener> implements
    TransactionListener {

    CompositeTransactionListener(Set<Supplier<TransactionListener>> listenerFactories) {
        for (Supplier<TransactionListener> supplier : listenerFactories) {
            TransactionListener listener = supplier.get();
            if (listener != null) {
                add(listener);
            }
        }
    }

    @Override
    public void beforeBegin(TransactionIsolation isolation) {
        for (TransactionListener listener : this) {
            listener.beforeBegin(isolation);
        }
    }

    @Override
    public void afterBegin(TransactionIsolation isolation) {
        for (TransactionListener listener : this) {
            listener.afterBegin(isolation);
        }
    }

    @Override
    public void beforeCommit(Set<Type<?>> types) {
        for (TransactionListener listener : this) {
            listener.beforeCommit(types);
        }
    }

    @Override
    public void afterCommit(Set<Type<?>> types) {
        for (TransactionListener listener : this) {
            listener.afterCommit(types);
        }
    }

    @Override
    public void beforeRollback(Set<Type<?>> types) {
        for (TransactionListener listener : this) {
            listener.beforeRollback(types);
        }
    }

    @Override
    public void afterRollback(Set<Type<?>> types) {
        for (TransactionListener listener : this) {
            listener.afterRollback(types);
        }
    }
}
