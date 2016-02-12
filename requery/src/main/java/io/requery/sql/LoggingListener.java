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

import io.requery.proxy.PostLoadListener;
import io.requery.proxy.PostInsertListener;
import io.requery.proxy.PostDeleteListener;
import io.requery.proxy.PostUpdateListener;
import io.requery.proxy.PreInsertListener;
import io.requery.proxy.PreUpdateListener;
import io.requery.proxy.PreDeleteListener;

import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;

class LoggingListener<T> implements StatementListener,
    PostLoadListener<T>,
    PostInsertListener<T>,
    PostDeleteListener<T>,
    PostUpdateListener<T>,
    PreInsertListener<T>,
    PreDeleteListener<T>,
    PreUpdateListener<T> {

    private final Logger log;
    private final Level level;

    public LoggingListener() {
        this(Logger.getLogger("requery"), Level.INFO);
    }

    public LoggingListener(Logger log, Level level) {
        this.log = log;
        this.level = level;
    }

    @Override
    public void postLoad(T entity) {
        log.log(level, "postLoad {0}", entity);
    }

    @Override
    public void postInsert(T entity) {
        log.log(level, "postInsert {0}", entity);
    }

    @Override
    public void postDelete(T entity) {
        log.log(level, "postDelete {0}", entity);
    }

    @Override
    public void postUpdate(T entity) {
        log.log(level, "postUpdate {0}", entity);
    }

    @Override
    public void preInsert(T entity) {
        log.log(level, "preInsert {0}", entity);
    }

    @Override
    public void preDelete(T entity) {
        log.log(level, "preDelete {0}", entity);
    }

    @Override
    public void preUpdate(T entity) {
        log.log(level, "preUpdate {0}", entity);
    }

    @Override
    public void beforeExecuteUpdate(Statement statement, String sql, BoundParameters parameters) {
        if (parameters != null && !parameters.isEmpty()) {
            log.log(level, "beforeExecuteUpdate {0} sql:\n{1} \n({2})",
                new Object[]{statement, sql, parameters});
        } else {
            log.log(level, "beforeExecuteUpdate {0} sql:\n{1}", new Object[]{statement, sql});
        }
    }

    @Override
    public void afterExecuteUpdate(Statement statement) {
        log.log(level, "afterExecuteUpdate");
    }

    @Override
    public void beforeExecuteQuery(Statement statement, String sql, BoundParameters parameters) {
        if (parameters != null && !parameters.isEmpty()) {
            log.log(level, "beforeExecuteQuery {0} sql:\n{1} \n({2})",
                new Object[]{statement, sql, parameters});
        } else {
            log.log(level, "beforeExecuteQuery {0} sql:\n{1}", new Object[]{statement, sql});
        }
    }

    @Override
    public void afterExecuteQuery(Statement statement) {
        log.log(level, "afterExecuteQuery");
    }
}
