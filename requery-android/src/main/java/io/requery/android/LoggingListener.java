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

import android.util.Log;
import io.requery.proxy.PostLoadListener;
import io.requery.proxy.PostInsertListener;
import io.requery.proxy.PostDeleteListener;
import io.requery.proxy.PostUpdateListener;
import io.requery.proxy.PreInsertListener;
import io.requery.proxy.PreDeleteListener;
import io.requery.proxy.PreUpdateListener;
import io.requery.sql.BoundParameters;
import io.requery.sql.StatementListener;

import java.sql.Statement;

public class LoggingListener implements StatementListener,
        PostLoadListener<Object>,
        PostInsertListener<Object>,
        PostDeleteListener<Object>,
        PostUpdateListener<Object>,
        PreInsertListener<Object>,
        PreDeleteListener<Object>,
        PreUpdateListener<Object> {

    private final String tag;

    public LoggingListener() {
        this("requery");
    }

    public LoggingListener(String tag) {
        this.tag = tag;
    }

    @Override
    public void postLoad(Object entity) {
        Log.i(tag, String.format("postLoad %s", entity));
    }

    @Override
    public void postInsert(Object entity) {
        Log.i(tag, String.format("postInsert %s", entity));
    }

    @Override
    public void postDelete(Object entity) {
        Log.i(tag, String.format("postDelete %s", entity));
    }

    @Override
    public void postUpdate(Object entity) {
        Log.i(tag, String.format("postUpdate %s", entity));
    }

    @Override
    public void preInsert(Object entity) {
        Log.i(tag, String.format("preInsert %s", entity));
    }

    @Override
    public void preDelete(Object entity) {
        Log.i(tag, String.format("preDelete %s", entity));
    }

    @Override
    public void preUpdate(Object entity) {
        Log.i(tag, String.format("preUpdate %s", entity));
    }

    @Override
    public void beforeExecuteUpdate(Statement statement, String sql, BoundParameters parameters) {
        Log.i(tag, String.format("beforeExecuteUpdate sql: %s", sql));
    }

    @Override
    public void afterExecuteUpdate(Statement statement, int count) {
        Log.i(tag, String.format("afterExecuteUpdate %d", count));
    }

    @Override
    public void beforeExecuteBatchUpdate(Statement statement, String sql) {
        Log.i(tag, String.format("beforeExecuteUpdate sql: %s", sql));
    }

    @Override
    public void afterExecuteBatchUpdate(Statement statement, int[] count) {
        Log.i(tag, "afterExecuteBatchUpdate");
    }

    @Override
    public void beforeExecuteQuery(Statement statement, String sql, BoundParameters parameters) {
        Log.i(tag, String.format("beforeExecuteQuery sql: %s", sql));
    }

    @Override
    public void afterExecuteQuery(Statement statement) {
        Log.i(tag, "afterExecuteQuery");
    }
}
