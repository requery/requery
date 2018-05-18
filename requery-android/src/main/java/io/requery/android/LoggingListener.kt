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

import android.util.Log
import io.requery.proxy.PostLoadListener
import io.requery.proxy.PostInsertListener
import io.requery.proxy.PostDeleteListener
import io.requery.proxy.PostUpdateListener
import io.requery.proxy.PreInsertListener
import io.requery.proxy.PreDeleteListener
import io.requery.proxy.PreUpdateListener
import io.requery.sql.BoundParameters
import io.requery.sql.StatementListener

import java.sql.Statement

class LoggingListener @JvmOverloads constructor(private val tag: String = "requery") :
        StatementListener,
        PostLoadListener<Any>,
        PostInsertListener<Any>,
        PostDeleteListener<Any>,
        PostUpdateListener<Any>,
        PreInsertListener<Any>,
        PreDeleteListener<Any>,
        PreUpdateListener<Any> {

    override fun postLoad(entity: Any) {
        Log.i(tag, String.format("postLoad %s", entity))
    }

    override fun postInsert(entity: Any) {
        Log.i(tag, String.format("postInsert %s", entity))
    }

    override fun postDelete(entity: Any) {
        Log.i(tag, String.format("postDelete %s", entity))
    }

    override fun postUpdate(entity: Any) {
        Log.i(tag, String.format("postUpdate %s", entity))
    }

    override fun preInsert(entity: Any) {
        Log.i(tag, String.format("preInsert %s", entity))
    }

    override fun preDelete(entity: Any) {
        Log.i(tag, String.format("preDelete %s", entity))
    }

    override fun preUpdate(entity: Any) {
        Log.i(tag, String.format("preUpdate %s", entity))
    }

    override fun beforeExecuteUpdate(statement: Statement, sql: String, parameters: BoundParameters?) {
        Log.i(tag, String.format("beforeExecuteUpdate sql: %s", sql))
    }

    override fun afterExecuteUpdate(statement: Statement, count: Int) {
        Log.i(tag, String.format("afterExecuteUpdate %d", count))
    }

    override fun beforeExecuteBatchUpdate(statement: Statement, sql: String) {
        Log.i(tag, String.format("beforeExecuteUpdate sql: %s", sql))
    }

    override fun afterExecuteBatchUpdate(statement: Statement, count: IntArray) {
        Log.i(tag, "afterExecuteBatchUpdate")
    }

    override fun beforeExecuteQuery(statement: Statement, sql: String, parameters: BoundParameters?) {
        Log.i(tag, String.format("beforeExecuteQuery sql: %s", sql))
    }

    override fun afterExecuteQuery(statement: Statement) {
        Log.i(tag, "afterExecuteQuery")
    }
}
