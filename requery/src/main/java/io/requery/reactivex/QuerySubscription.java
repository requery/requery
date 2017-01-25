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

import io.requery.query.Result;
import io.requery.util.CloseableIterator;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public class QuerySubscription<T> implements Subscription {

    private final Result<T> result;
    private final Subscriber<? super T> subscriber;
    private final AtomicBoolean canceled;
    private final AtomicLong emitted;
    private final AtomicLong requested;

    public QuerySubscription(Result<T> result, Subscriber<? super T> subscriber) {
        this.result = result;
        this.subscriber = subscriber;
        canceled = new AtomicBoolean();
        emitted = new AtomicLong();
        requested = new AtomicLong();
    }

    @Override
    public void request(long n) {
        try {
            if (n == Long.MAX_VALUE && requested.compareAndSet(0, Long.MAX_VALUE)) {
                requestAll();
            } else if (n > 0 && add(requested, n) == 0) {
                requestN(n);
            }
        } catch (Throwable e) {
            subscriber.onError(e);
        }
    }

    private void requestAll() {
        // emitting all elements
        try (CloseableIterator<T> iterator = result.iterator()) {
            while (!canceled.get()) {
                if (iterator.hasNext()) {
                    subscriber.onNext(iterator.next());
                    emitted.incrementAndGet();
                } else {
                    subscriber.onComplete();
                    break;
                }
            }
        }
    }

    private void requestN(long n) {
        // emitting with limit/offset
        long count = n;
        while (count > 0) {
            try (CloseableIterator<T> iterator = result.iterator(emitted.intValue(), (int) n)) {
                long i = 0;
                while (!canceled.get() && iterator.hasNext()) {
                    if (i++ < count) {
                        subscriber.onNext(iterator.next());
                    } else {
                        break;
                    }
                }
                emitted.addAndGet(i);
                // no more items
                if (!canceled.get() && i < count) {
                    subscriber.onComplete();
                    break;
                }
                count = requested.addAndGet(-count);
            }
        }
    }

    @Override
    public void cancel() {
        canceled.compareAndSet(false, true);
    }

    private static long add(AtomicLong requested, long n) {
        while (true) {
            long value = requested.get();

            if (value == Long.MAX_VALUE) {
                return Long.MAX_VALUE;
            }

            long update = value + n;
            if (update < 0L) {
                update = Long.MAX_VALUE;
            }

            if (requested.compareAndSet(value, update)) {
                return value;
            }
        }
    }
}
