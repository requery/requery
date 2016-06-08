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

package io.requery.android;

import android.database.Cursor;
import android.os.Handler;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import io.requery.meta.EntityModel;
import io.requery.meta.Type;
import io.requery.proxy.EntityProxy;
import io.requery.query.Result;
import io.requery.sql.EntityDataStore;
import io.requery.sql.ResultSetIterator;
import io.requery.util.function.Function;

import java.io.Closeable;
import java.sql.SQLException;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * An implementation of {@link android.widget.BaseAdapter} specifically for displaying items from a
 * {@link EntityDataStore} query. To use extend this class and implement {@link #performQuery()}
 * and {@link #getView(Object, View, ViewGroup)}.
 *
 * @param <E> entity element type
 *
 * @author Nikhil Purushe
 */
public abstract class QueryAdapter<E> extends BaseAdapter implements Closeable {

    private final Handler handler;
    private final Function<E, EntityProxy<E>> proxyProvider;
    private ResultSetIterator<E> iterator;
    private boolean createdExecutor;
    private ExecutorService executor;
    private Future<Result<E>> queryFuture;

    /**
     * Creates a new adapter instance.
     *
     * @param model database entity model
     * @param type entity class type
     */
    protected QueryAdapter(EntityModel model, Class<E> type) {
        this(model.typeOf(type));
    }

    /**
     * Creates a new adapter instance.
     *
     * @param type entity class type
     */
    protected QueryAdapter(Type<E> type) {
        proxyProvider = type.getProxyProvider();
        handler = new Handler();
    }

    /**
     * Call this to clean up the underlying result.
     */
    @Override
    public void close() {
        if (queryFuture != null) {
            queryFuture.cancel(true);
        }
        if (iterator != null) {
            iterator.close();
            iterator = null;
        }
    }

    /**
     * Sets the {@link Executor} used for running the query off the main thread.
     *
     * @param executor ExecutorService to use for the background query.
     */
    public void setExecutor(ExecutorService executor) {
        if (createdExecutor && this.executor != null) {
            this.executor.shutdown();
        }
        this.executor = executor;
    }

    /**
     * Sets the results the adapter should display.
     *
     * @param iterator result set iterator
     */
    public void setResult(ResultSetIterator<E> iterator) {
        close();
        this.iterator = iterator;
        notifyDataSetChanged();
    }

    /**
     * Schedules the query to be run in the background. After completion
     * {@link #setResult(ResultSetIterator)} will be called to update the contents of the adapter.
     * Note if an {@link Executor} was not specified one will be created automatically to run the
     * query.
     */
    public void queryAsync() {
        if (this.executor == null) {
            this.executor = Executors.newSingleThreadExecutor();
            createdExecutor = true;
        }
        if (queryFuture != null && !queryFuture.isDone()) {
            queryFuture.cancel(true);
        }
        queryFuture = executor.submit(new Callable<Result<E>>() {
            @Override
            public Result<E> call() {
                final Result<E> result = performQuery();
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        setResult((ResultSetIterator<E>) result.iterator());
                    }
                });
                return result;
            }
        });
    }

    /**
     * Implement this method with your query operation. Note this method is executed on a
     * background thread not the main thread.
     *
     * @see #setExecutor(ExecutorService)
     *
     * @return query result set
     */
    public abstract Result<E> performQuery();

    @Override
    public E getItem(int position) {
        if (iterator == null) {
            return null;
        }
        return iterator.get(position);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        E item = getItem(position);
        return getView(item, convertView, parent);
    }

    /**
     * Called to display the data at the specified position for the given item. The item is
     * retrieved from the result iterator.
     *
     * @param item entity element to bind to the view
     * @param convertView view being recycled
     * @param parent parent view
     */
    public abstract View getView(E item, View convertView, ViewGroup parent);

    @Override
    public long getItemId(int position) {
        E item = iterator.get(position);
        EntityProxy<E> proxy = proxyProvider.apply(item);
        Object key = proxy.key();
        return key == null ? item.hashCode() : key.hashCode();
    }

    @Override
    public int getCount() {
        if (iterator == null) {
            return 0;
        }
        try {
            Cursor cursor = iterator.unwrap(Cursor.class);
            return cursor.getCount();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
