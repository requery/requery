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

import io.requery.Queryable;
import io.requery.meta.EntityModel;
import io.requery.meta.QueryAttribute;
import io.requery.query.Deletion;
import io.requery.query.Expression;
import io.requery.query.Insertion;
import io.requery.query.Result;
import io.requery.query.Scalar;
import io.requery.query.Selection;
import io.requery.query.Tuple;
import io.requery.query.Update;

import javax.sql.DataSource;
import java.util.Set;

/**
 * An implementation of {@link Queryable} that executes queries against a SQL database.
 *
 * @param <T> entity base type
 */
public class QueryableStore<T> implements Queryable<T>, AutoCloseable {

    private final EntityDataStore<T> data;

    /**
     * Creates a new {@link QueryableStore} with the given {@link DataSource} and
     * {@link EntityModel}.
     *
     * @param dataSource to use
     * @param model      to use
     */
    public QueryableStore(DataSource dataSource, EntityModel model) {
        this(dataSource, model, null);
    }

    /**
     * Creates a new {@link QueryableStore} with the given {@link DataSource},{@link EntityModel}
     * and {@link Mapping}.
     *
     * @param dataSource to use
     * @param model      to use
     * @param mapping    to use
     */
    public QueryableStore(DataSource dataSource, EntityModel model, Mapping mapping) {
        this(new ConfigurationBuilder(dataSource, model)
            .setMapping(mapping)
            .build());
    }

    /**
     * Creates a new {@link QueryableStore} with the given configuration.
     *
     * @param configuration to use
     */
    public QueryableStore(Configuration configuration) {
        this.data = new EntityDataStore<>(configuration);
    }

    @Override
    public void close() {
        data.close();
    }

    @Override
    public <E extends T> Selection<Result<E>> select(Class<E> type,
                                                     QueryAttribute<?, ?>... attributes) {
        return data.select(type, attributes);
    }

    @Override
    public <E extends T> Selection<Result<E>> select(
        Class<E> type, Set<? extends QueryAttribute<E, ?>> attributes) {
        return data.select(type, attributes);
    }

    @Override
    public <E extends T> Insertion<Result<Tuple>> insert(Class<E> type) {
        return data.insert(type);
    }

    @Override
    public <E extends T> Update<Scalar<Integer>> update(Class<E> type) {
        return data.update(type);
    }

    @Override
    public <E extends T> Deletion<Scalar<Integer>> delete(Class<E> type) {
        return data.delete(type);
    }

    @Override
    public <E extends T> Selection<Scalar<Integer>> count(Class<E> type) {
        return data.count(type);
    }

    @Override
    public Selection<Scalar<Integer>> count(QueryAttribute<?, ?>... attributes) {
        return data.count(attributes);
    }

    @Override
    public Selection<Result<Tuple>> select(Expression<?>... expressions) {
        return data.select(expressions);
    }

    @Override
    public Selection<Result<Tuple>> select(Set<? extends Expression<?>> expressions) {
        return data.select(expressions);
    }

    @Override
    public Update<Scalar<Integer>> update() {
        return data.update();
    }

    @Override
    public Deletion<Scalar<Integer>> delete() {
        return data.delete();
    }
}
