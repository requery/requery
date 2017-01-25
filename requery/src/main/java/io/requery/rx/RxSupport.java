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

package io.requery.rx;

import io.requery.BlockingEntityStore;
import io.requery.meta.Type;
import io.requery.meta.Types;
import io.requery.query.element.QueryElement;
import rx.Observable;
import rx.functions.Func1;

import java.util.Collections;
import java.util.Set;

/**
 * Support utility class for use with RxJava
 *
 * @author Nikhil Purushe
 */
public final class RxSupport {

    private static final TypeChangeListener typeChanges = new TypeChangeListener();

    private RxSupport() {
    }

    public static <S> SingleEntityStore<S> toReactiveStore(BlockingEntityStore<S> store) {
        return new SingleEntityStoreFromBlocking<>(store);
    }

    static <T> Observable<RxResult<T>> toResultObservable(final RxResult<T> result) {
        final QueryElement<?> element = result.unwrapQuery();
        // ensure the transaction listener is added in the target data store
        result.addTransactionListener(typeChanges);
        return typeChanges.commitSubject()
            .filter(new Func1<Set<Type<?>>, Boolean>() {
                @Override
                public Boolean call(Set<Type<?>> types) {
                    return !Collections.disjoint(element.entityTypes(), types) ||
                        Types.referencesType(element.entityTypes(), types);
                }
            }).map(new Func1<Set<Type<?>>, RxResult<T>>() {
                @Override
                public RxResult<T> call(Set<Type<?>> types) {
                    return result;
                }
            }).startWith(result);
    }
}
