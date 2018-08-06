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

package io.requery.sql;

import io.requery.EntityCache;
import io.requery.TransactionIsolation;
import io.requery.TransactionListener;
import io.requery.meta.EntityModel;
import io.requery.query.element.QueryElement;
import io.requery.sql.gen.Generator;
import io.requery.util.function.Supplier;

import java.util.Set;
import java.util.concurrent.Executor;

public interface RuntimeConfiguration extends ConnectionProvider {

    Mapping getMapping();

    EntityModel getModel();

    EntityCache getCache();

    Platform getPlatform();

    Generator<QueryElement<?>> getStatementGenerator();

    boolean supportsBatchUpdates();

    int getBatchUpdateSize();

    StatementListener getStatementListener();

    Set<Supplier<TransactionListener>> getTransactionListenerFactories();

    TransactionMode getTransactionMode();

    TransactionIsolation getTransactionIsolation();

    TransactionProvider getTransactionProvider();

    Executor getWriteExecutor();

    QueryBuilder.Options getQueryBuilderOptions();
}
