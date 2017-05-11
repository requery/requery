/*
 * Copyright 2017 requery.io
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
import android.support.v7.widget.RecyclerView;
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
 * An implementation of {@link android.support.v7.widget.RecyclerView.Adapter} specifically for
 * displaying items from a {@link EntityDataStore} query. To use extend  this class and implement
 * {@link #performQuery()} and {@link #onBindViewHolder(Object, RecyclerView.ViewHolder, int)}.
 *
 * @param <E> entity element type
 * @param <VH> view holder type
 *
 * @author Nikhil Purushe
 */
@SuppressWarnings("WeakerAccess")
public abstract class QueryRecyclerAdapter<E, VH extends RecyclerView.ViewHolder> extends
        RecyclerView.Adapter<VH> implements Closeable {

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
    protected QueryRecyclerAdapter(EntityModel model, Class<E> type) {
        this(model.typeOf(type));
    }

    /**
     * Creates a new adapter instance without any type mapping.
     */
    protected QueryRecyclerAdapter() {
        this(null);
    }

    /**
     * Creates a new adapter instance mapped to the given type.
     *
     * @param type entity type
     */
    protected QueryRecyclerAdapter(Type<E> type) {
        setHasStableIds(true);
        proxyProvider = type == null ? null : type.getProxyProvider();
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
        setExecutor(null);
    }

    /**
     * @return the underlying iterator being used or null if none
     */
    protected ResultSetIterator<E> getIterator() {
        return iterator;
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
        if (this.iterator != null) {
            this.iterator.close();
        }
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
                // main work happens here
                final ResultSetIterator<E> iterator = (ResultSetIterator<E>) result.iterator();
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        setResult(iterator);
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
    public void onBindViewHolder(VH holder, int position) {
        E item = iterator.get(position);
        onBindViewHolder(item, holder, position);
    }

    /**
     * Called to display the data at the specified position for the given item. The item is
     * retrieved from the result iterator.
     *
     * @param item entity element to bind to the view
     * @param holder view holder to be updated
     * @param position position index of the view
     */
    public abstract void onBindViewHolder(E item, VH holder, int position);

    @Override
    public long getItemId(int position) {
        E item = iterator.get(position);
        if (item == null) {
            throw new IllegalStateException();
        }
        Object key = null;
        if (proxyProvider != null) {
            EntityProxy<? extends E> proxy = proxyProvider.apply(item);
            key = proxy.key();
        }
        return key == null ? item.hashCode() : key.hashCode();
    }

    @Override
    public int getItemCount() {
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

    @Override
    public int getItemViewType(int position) {
        E item = iterator.get(position);
        return getItemViewType(item);
    }

    /**
     * Return the view type of the item.
     *
     * @param item being checked
     * @return integer identifying the type of the view
     */
    protected int getItemViewType(E item) {
        return 0;
    }

    @Override
    public void onDetachedFromRecyclerView(RecyclerView recyclerView) {
        super.onDetachedFromRecyclerView(recyclerView);
        close();
        setExecutor(null);
    }

}
