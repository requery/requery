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

package io.requery.reactor;

import io.requery.query.Result;
import io.requery.query.ResultDelegate;
import io.requery.query.element.QueryElement;
import io.requery.query.element.QueryWrapper;
import io.requery.reactivex.QuerySubscription;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import reactor.core.publisher.Flux;

import javax.annotation.CheckReturnValue;

/**
 * {@link Result} type with Reactor  conversion methods.
 *
 * @param <E> element type
 */
public class ReactorResult<E> extends ResultDelegate<E> implements QueryWrapper {

    ReactorResult(Result<E> delegate) {
        super(delegate);
    }

    /**
     * Converts the result stream to a {@link Flux}.
     *
     * @return stream of the results of this query.
     */
    @CheckReturnValue
    public Flux<E> flux() {
        return Flux.from(new Publisher<E>() {
            @Override
            public void subscribe(Subscriber<? super E> s) {
                s.onSubscribe(new QuerySubscription<>(ReactorResult.this, s));
            }
        });
    }

    @Override
    public QueryElement unwrapQuery() {
        return ((QueryWrapper)delegate).unwrapQuery();
    }
}
