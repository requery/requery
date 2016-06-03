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

package io.requery.sql;

import io.requery.query.Result;
import io.requery.query.element.QueryElement;
import io.requery.query.element.QueryOperation;

/**
 * Executes an select query operation.
 *
 * @author Nikhil Purushe
 */
class SelectOperation<E> implements QueryOperation<Result<E>> {

    private final RuntimeConfiguration configuration;
    private final ResultReader<E> reader;

    SelectOperation(RuntimeConfiguration configuration, ResultReader<E> reader) {
        this.configuration = configuration;
        this.reader = reader;
    }

    @Override
    public Result<E> evaluate(QueryElement<Result<E>> query) {
        return new SelectResult<>(configuration, query, reader);
    }
}
