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

import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.requery.TransactionListenable;
import io.requery.TransactionListener;
import io.requery.query.Result;
import io.requery.query.ResultDelegate;
import io.requery.query.element.QueryElement;
import io.requery.query.element.QueryWrapper;
import io.requery.util.function.Supplier;
import org.reactivestreams.Subscriber;

import javax.annotation.CheckReturnValue;
import java.util.concurrent.Callable;

/**
 * {@link Result} type with RxJava 2.0 conversion methods.
 *
 * @param <E> element type
 */
public class ReactiveResult<E> extends ResultDelegate<E> implements QueryWrapper, TransactionListenable {

    ReactiveResult(Result<E> delegate) {
        super(delegate);
    }

    /**
     * Converts the result stream to a {@link io.reactivex.Flowable}. When the flowable terminates
     * this result instance will be closed.
     *
     * @return flowable stream of the results of this query.
     */
    @CheckReturnValue
    public Flowable<E> flowable() {
        return new Flowable<E>() {
            @Override
            protected void subscribeActual(Subscriber<? super E> s) {
                s.onSubscribe(new QuerySubscription<>(ReactiveResult.this, s));
            }
        };
    }

    /**
     * Converts the result stream to a {@link io.reactivex.Maybe} value, return the first element
     * if present or completes if no results.
     *
     * @return maybe instance of the results of this query.
     */
    @CheckReturnValue
    public Maybe<E> maybe() {
        return Maybe.fromCallable(new Callable<E>() {
            @Override
            public E call() throws Exception {
                return firstOrNull();
            }
        });
    }

    /**
     * Converts the result stream to a {@link io.reactivex.Observable}. When the observable
     * terminates this result instance will be closed.
     *
     * @return observable stream of the results of this query.
     */
    @CheckReturnValue
    public Observable<E> observable() {
        return flowable().toObservable();
    }

    /**
     * Creates an {@link io.reactivex.Observable} that emits this result initially and then again
     * whenever commits that may affect the query result are made from within the same
     * {@link io.requery.EntityStore} from where this instance originated.
     *
     * @return {@link io.reactivex.Observable} instance of this result that is triggered whenever
     * changes that may affect the query are made.
     */
    @CheckReturnValue
    public Observable<ReactiveResult<E>> observableResult() {
        return ReactiveSupport.toObservableResult(this);
    }

    @Override
    public void addTransactionListener(Supplier<TransactionListener> supplier) {
        ((TransactionListenable)delegate).addTransactionListener(supplier);
    }

    @Override
    public QueryElement unwrapQuery() {
        return ((QueryWrapper)delegate).unwrapQuery();
    }
}
