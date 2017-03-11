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

import io.requery.query.Scalar;
import io.requery.query.ScalarDelegate;
import rx.Single;

import javax.annotation.CheckReturnValue;

/**
 * {@link Scalar} type with RxJava conversion methods.
 *
 * @param <E> element type
 */
public class RxScalar<E> extends ScalarDelegate<E> {

    RxScalar(Scalar<E> delegate) {
        super(delegate);
    }

    /**
     * Converts this Scalar computation to a single {@link rx.Single}.
     *
     * @return {@link rx.Single} for the result of this query.
     */
    @CheckReturnValue
    public rx.Single<E> toSingle() {
        return Single.fromCallable(this);
    }
}
