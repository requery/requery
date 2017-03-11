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

package io.requery.reactivex;

import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;
import io.requery.TransactionIsolation;
import io.requery.TransactionListener;
import io.requery.meta.Type;
import io.requery.util.function.Supplier;

import java.util.Set;

/**
 * {@link TransactionListener} for listening to entity commits and emitting the types that have
 * changed through an Rx {@link Subject}.
 *
 * @author Nikhil Purushe
 */
final class TransactionListenerSupplier implements Supplier<TransactionListener> {

    private final Subject<Set<Type<?>>> commitSubject;
    private final Subject<Set<Type<?>>> rollbackSubject;

    TransactionListenerSupplier() {
        commitSubject = PublishSubject.<Set<Type<?>>>create().toSerialized();
        rollbackSubject = PublishSubject.<Set<Type<?>>>create().toSerialized();
    }

    @Override
    public TransactionListener get() {
        return new TransactionListener() {
            @Override
            public void beforeBegin(TransactionIsolation isolation) {
            }

            @Override
            public void afterBegin(TransactionIsolation isolation) {
            }

            @Override
            public void beforeCommit(Set<Type<?>> types) {
            }

            @Override
            public void afterCommit(Set<Type<?>> types) {
                commitSubject.onNext(types);
            }

            @Override
            public void beforeRollback(Set<Type<?>> types) {
            }

            @Override
            public void afterRollback(Set<Type<?>> types) {
                rollbackSubject.onNext(types);
            }
        };
    }

    Subject<Set<Type<?>>> commitSubject() {
        return commitSubject;
    }
}
