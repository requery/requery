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

package io.requery.query.element;

import io.requery.util.function.Function;

class ExtendQueryOperation<T extends S, S> implements QueryOperation<S> {
    private final Function<S, T> transform;
    private final QueryOperation<S> operation;

    ExtendQueryOperation(Function<S, T> transform, QueryOperation<S> operation) {
        this.transform = transform;
        this.operation = operation;
    }

    @Override
    public S evaluate(QueryElement<S> query) {
        return transform.apply(operation.evaluate(query));
    }
}
