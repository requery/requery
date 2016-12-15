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

import io.reactivex.Observable;
import io.reactivex.functions.Function;
import io.reactivex.functions.Predicate;
import io.requery.BlockingEntityStore;
import io.requery.meta.Type;
import io.requery.meta.Types;
import io.requery.query.element.QueryElement;

import java.util.Collections;
import java.util.Set;

/**
 * Support class for use with RxJava 2.0
 */
public final class ReactiveSupport {

    private static final TransactionListenerSupplier typeChanges = new TransactionListenerSupplier();

    private ReactiveSupport() {
    }

    public static <S> ReactiveEntityStore<S> toReactiveStore(BlockingEntityStore<S> store) {
        return new WrappedEntityStore<>(store);
    }

    static <T> Observable<ReactiveResult<T>> toObservableResult(final ReactiveResult<T> result) {
        final QueryElement<?> element = result.unwrapQuery();
        // ensure the transaction listener is added in the target data store
        result.addTransactionListener(typeChanges);
        return typeChanges.commitSubject()
            .filter(new Predicate<Set<Type<?>>>() {
                @Override
                public boolean test(Set<Type<?>> types) {
                    return !Collections.disjoint(element.entityTypes(), types) ||
                           Types.referencesType(element.entityTypes(), types);
                }
            }).map(new Function<Set<Type<?>>, ReactiveResult<T>>() {
                @Override
                public ReactiveResult<T> apply(Set<Type<?>> types) {
                    return result;
                }
            }).startWith(result);
    }
}
