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
import io.requery.query.BaseResult;
import io.requery.query.Result;
import io.requery.query.Scalar;
import io.requery.query.element.QueryElement;
import io.requery.util.function.Supplier;
import rx.Observable;
import rx.Scheduler;
import rx.Single;
import rx.functions.Action0;
import rx.functions.Func1;

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

    public static <T> Observable<Result<T>> toResultObservable(final Result<T> result) {
        if (!(result instanceof ObservableResult)) {
            throw new UnsupportedOperationException();
        }
        ObservableResult observableResult = (ObservableResult) result;
        final QueryElement element = observableResult.unwrapQuery();
        // ensure the transaction listener is added in the target data store
        observableResult.addTransactionListener(typeChanges);
        return typeChanges.commitSubject().filter(new Func1<Type<?>, Boolean>() {
            @Override
            public Boolean call(Type<?> type) {
                return element.entityTypes().contains(type);
            }
        }).map(new Func1<Type<?>, Result<T>>() {
            @Override
            public Result<T> call(Type<?> type) {
                return result;
            }
        }).startWith(result);
    }

    public static <E> Observable<E> toObservable(final BaseResult<E> result, Integer limit) {
        // if a limit on the query is set then just create a plain observable via iterator.
        // Otherwise create a Observable with a custom subscriber that can modify the limit
        if (limit == null) {
            return Observable.create(new OnSubscribeFromQuery<>(result));
        } else {
            return Observable.from(result).doOnTerminate(new Action0() {
                @Override
                public void call() {
                    result.close();
                }
            });
        }
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
