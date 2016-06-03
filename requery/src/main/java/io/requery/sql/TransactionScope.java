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

import io.requery.meta.Type;
import io.requery.util.function.Supplier;

import java.util.Set;

/**
 * If there is no current transaction, starts one otherwise does nothing.
 */
class TransactionScope implements AutoCloseable {

    private final EntityTransaction transaction;
    private final boolean enteredTransaction;

    TransactionScope(Supplier<? extends EntityTransaction> supplier) {
        this(supplier, null);
    }

    TransactionScope(Supplier<? extends EntityTransaction> supplier, Set<Type<?>> types) {
        this.transaction = supplier.get();
        if (!transaction.active()) {
            transaction.begin();
            enteredTransaction = true;
            if (types != null) {
                transaction.addToTransaction(types);
            }
        } else {
            enteredTransaction = false;
        }
    }

    public void commit() {
        if (enteredTransaction) {
            transaction.commit();
        }
    }

    @Override
    public void close() {
        if (enteredTransaction) {
            transaction.close();
        }
    }
}
