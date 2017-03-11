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

import io.requery.query.Result;
import io.requery.util.CloseableIterator;
import rx.Observable;
import rx.Producer;
import rx.Subscriber;
import rx.internal.operators.BackpressureUtils;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Implementation {@link Observable.OnSubscribe} where emitting items from a query result.
 * Subscriber can request n elements and modify the upstream query to limit the
 * number of items from a data source.
 *
 * See <a href="https://github.com/ReactiveX/RxJava/wiki/Backpressure">Backpressure</a>
 *
 * @param <T> emitted type
 *
 * @author Nikhil Purushe
 */
class OnSubscribeFromQuery<T> implements Observable.OnSubscribe<T> {

    private final Result<T> result;

    OnSubscribeFromQuery(Result<T> result) {
        this.result = result;
    }

    @Override
    public void call(Subscriber<? super T> subscriber) {
        subscriber.setProducer(new ElementProducer(subscriber));
    }

    // TODO support paging on an attribute instead of offset/limit since it could miss/overlap records
    private class ElementProducer implements Producer {

        private final Subscriber<? super T> subscriber;
        private final AtomicLong emitted;
        private final AtomicLong requested;

        ElementProducer(Subscriber<? super T> subscriber) {
            this.subscriber = subscriber;
            requested = new AtomicLong();
            emitted = new AtomicLong();
        }

        @Override
        public void request(long n) {
            if (n == Long.MAX_VALUE && requested.compareAndSet(0, Long.MAX_VALUE)) {
                // emitting all elements
                try (CloseableIterator<T> iterator = result.iterator()) {
                    while (!subscriber.isUnsubscribed()) {
                        if (iterator.hasNext()) {
                            subscriber.onNext(iterator.next());
                            emitted.incrementAndGet();
                        } else {
                            subscriber.onCompleted();
                            break;
                        }
                    }
                }
            } else if (n > 0 && BackpressureUtils.getAndAddRequest(requested, n) == 0) {
                // emitting with limit/offset
                long count = n;
                while (count > 0) {
                    try (CloseableIterator<T> iterator =
                             result.iterator(emitted.intValue(), (int) n)) {
                        long i = 0;
                        while (!subscriber.isUnsubscribed() && iterator.hasNext()) {
                            if (i++ < count) {
                                subscriber.onNext(iterator.next());
                            } else {
                                break;
                            }
                        }
                        emitted.addAndGet(i);
                        // no more items
                        if (!subscriber.isUnsubscribed() && i < count) {
                            subscriber.onCompleted();
                            break;
                        }
                        count = requested.addAndGet(-count);
                    }
                }
            }
        }
    }
}
