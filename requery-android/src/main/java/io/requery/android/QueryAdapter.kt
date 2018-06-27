/*
 * Copyright 2018 requery.io
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

package io.requery.android

import android.database.Cursor
import android.os.Handler
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import io.requery.meta.EntityModel
import io.requery.meta.Type
import io.requery.proxy.EntityProxy
import io.requery.query.Result
import io.requery.sql.EntityDataStore
import io.requery.sql.ResultSetIterator
import io.requery.util.function.Function

import java.io.Closeable
import java.sql.SQLException
import java.util.concurrent.Callable
import java.util.concurrent.Executor
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future

/**
 * An implementation of [android.widget.BaseAdapter] specifically for displaying items from a
 * [EntityDataStore] query. To use extend this class and implement [.performQuery]
 * and [.getView].
 *
 * @param <E> entity element type
 * @param type entity type
 *
 * @author Nikhil Purushe
 */
abstract class QueryAdapter<E> @JvmOverloads protected constructor(type: Type<E>? = null) : BaseAdapter(), Closeable {

    private val handler: Handler = Handler()
    private val proxyProvider: Function<E, EntityProxy<E>>? = type?.proxyProvider
    private var iterator: ResultSetIterator<E>? = null
    private var createdExecutor: Boolean = false
    private var executor: ExecutorService? = null
    private var queryFuture: Future<Result<E>>? = null

    /**
     * Creates a new adapter instance.
     *
     * @param model database entity model
     * @param type entity class type
     */
    protected constructor(model: EntityModel, type: Class<E>) : this(model.typeOf<E>(type))

    /**
     * Call this to clean up the underlying result.
     */
    override fun close() {
        if (queryFuture != null) {
            queryFuture!!.cancel(true)
        }
        iterator?.close()
        iterator = null
    }

    /**
     * Sets the [Executor] used for running the query off the main thread.
     *
     * @param executor ExecutorService to use for the background query.
     */
    fun setExecutor(executor: ExecutorService) {
        if (createdExecutor && this.executor != null) {
            this.executor!!.shutdown()
        }
        this.executor = executor
    }

    /**
     * Sets the results the adapter should display.
     *
     * @param iterator result set iterator
     */
    fun setResult(iterator: ResultSetIterator<E>) {
        close()
        this.iterator = iterator
        notifyDataSetChanged()
    }

    /**
     * Schedules the query to be run in the background. After completion
     * [.setResult] will be called to update the contents of the adapter.
     * Note if an [Executor] was not specified one will be created automatically to run the
     * query.
     */
    fun queryAsync() {
        if (this.executor == null) {
            this.executor = Executors.newSingleThreadExecutor()
            createdExecutor = true
        }
        if (queryFuture != null && !queryFuture!!.isDone) {
            queryFuture!!.cancel(true)
        }
        queryFuture = executor!!.submit(Callable {
            val result = performQuery()
            handler.post { setResult(result.iterator() as ResultSetIterator<E>) }
            result
        })
    }

    /**
     * Implement this method with your query operation. Note this method is executed on a
     * background thread not the main thread.
     *
     * @see .setExecutor
     * @return query result set
     */
    abstract fun performQuery(): Result<E>

    override fun getItem(position: Int): E? {
        return if (iterator == null) {
            null
        } else iterator!!.get(position)
    }

    override fun getView(position: Int, convertView: View, parent: ViewGroup): View {
        val item = getItem(position)
        return getView(item, convertView, parent)
    }

    /**
     * Called to display the data at the specified position for the given item. The item is
     * retrieved from the result iterator.
     *
     * @param item entity element to bind to the view
     * @param convertView view being recycled
     * @param parent parent view
     */
    abstract fun getView(item: E?, convertView: View, parent: ViewGroup): View

    override fun getItemId(position: Int): Long {
        val item = iterator!!.get(position)
        var key: Any? = null
        if (proxyProvider != null) {
            val proxy = proxyProvider.apply(item)
            key = proxy.key()
        }
        return (if (key == null) item!!.hashCode() else key.hashCode()).toLong()
    }

    override fun getCount(): Int {
        if (iterator == null) {
            return 0
        }
        try {
            val cursor = iterator!!.unwrap(Cursor::class.java)
            return cursor.count
        } catch (e: SQLException) {
            throw RuntimeException(e)
        }

    }
}
