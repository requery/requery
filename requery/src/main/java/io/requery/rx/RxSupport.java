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
import io.requery.TransactionListenable;
import io.requery.meta.Attribute;
import io.requery.meta.Type;
import io.requery.query.BaseResult;
import io.requery.query.Result;
import io.requery.query.Scalar;
import io.requery.query.element.QueryElement;
import io.requery.query.element.QueryWrapper;
import io.requery.util.function.Supplier;
import rx.Observable;
import rx.Scheduler;
import rx.Single;
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
        return toReactiveStore(store, null);
    }

    public static <S> SingleEntityStore<S> toReactiveStore(BlockingEntityStore<S> store,
                                                           Scheduler subscribeOn) {
        return new SingleEntityStoreFromBlocking<>(store, subscribeOn);
    }

    public static <T> Observable<Result<T>> toResultObservable(final Result<T> result) {
        if (!(result instanceof TransactionListenable)) {
            throw new UnsupportedOperationException();
        }
        TransactionListenable listenable = (TransactionListenable) result;
        final QueryElement<?> element = ((QueryWrapper) result).unwrapQuery();
        // ensure the transaction listener is added in the target data store
        listenable.addTransactionListener(typeChanges);
        return typeChanges.commitSubject()
            .filter(new Func1<Set<Type<?>>, Boolean>() {
                @Override
                public Boolean call(Set<Type<?>> types) {
                    return !Collections.disjoint(element.entityTypes(), types) ||
                        referencesType(element.entityTypes(), types);
                }
            }).map(new Func1<Set<Type<?>>, Result<T>>() {
                @Override
                public Result<T> call(Set<Type<?>> types) {
                    return result;
                }
            }).startWith(result);
    }

    public static boolean referencesType(Set<Type<?>> source, Set<Type<?>> changed) {
        for (Type<?> type : source) {
            for (Attribute<?, ?> attribute : type.getAttributes()) {
                // find if any referencing types that maybe affected by changes to the type
                if (attribute.isAssociation()) {
                    Attribute referenced = null;
                    if (attribute.getReferencedAttribute() != null) {
                        referenced = attribute.getReferencedAttribute().get();
                    }
                    if (attribute.getMappedAttribute() != null) {
                        referenced = attribute.getMappedAttribute().get();
                    }
                    if (referenced != null) {
                        Type<?> declared = referenced.getDeclaringType();
                        if (changed.contains(declared)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    public static <E> Observable<E> toObservable(final BaseResult<E> result) {
        return Observable.create(new OnSubscribeFromQuery<>(result));
    }

    public static <E> Single<E> toSingle(final Scalar<E> scalar) {
        return Single.create(new SingleOnSubscribeFromSupplier<>(scalar.toSupplier()));
    }

    static <E> Single<E> toSingle(Supplier<E> supplier, Scheduler subscribeOn) {
        Single<E> single = Single.create(new SingleOnSubscribeFromSupplier<>(supplier));
        if (subscribeOn != null) {
            return single.subscribeOn(subscribeOn);
        }
        return single;
    }
}
