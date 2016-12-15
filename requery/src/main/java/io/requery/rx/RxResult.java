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

import io.requery.TransactionListenable;
import io.requery.TransactionListener;
import io.requery.query.Result;
import io.requery.query.ResultDelegate;
import io.requery.query.element.QueryElement;
import io.requery.query.element.QueryWrapper;
import io.requery.util.function.Supplier;
import rx.Observable;

import javax.annotation.CheckReturnValue;

/**
 * {@link Result} type with RxJava conversion methods.
 *
 * @param <E> element type
 */
public class RxResult<E> extends ResultDelegate<E> implements QueryWrapper, TransactionListenable {

    RxResult(Result<E> delegate) {
        super(delegate);
    }

    /**
     * Converts the result stream to a {@link rx.Observable}. When the observable terminates this
     * result instance will be closed.
     *
     * @return observable stream of the results of this query.
     */
    @CheckReturnValue
    public Observable<E> toObservable() {
        return Observable.create(new OnSubscribeFromQuery<>(this));
    }

    /**
     * Creates an observable that emits this result initially and then again whenever commits that
     * may affect the query result are made from within the same {@link io.requery.EntityStore}
     * from where this instance originated.
     *
     * @return observable instance of this result that is triggered whenever changes that may
     * affect the query are made.
     */
    @CheckReturnValue
    public Observable<RxResult<E>> toSelfObservable() {
        return RxSupport.toResultObservable(this);
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
