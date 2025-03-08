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

import java.sql.Connection;

/**
 * Determines how transactions are started and used during persist operations.
 */
public enum TransactionMode {

    /**
     * Transactions are disabled. e.g. {@link Connection#setAutoCommit(boolean)} is set to true.
     */
    NONE,

    /**
     * Managed transaction environment with JTA e.g. {@link Connection#commit()} and
     * {@link Connection#rollback()} are NOT used. Transaction is optionally started as a
     * {@link jakarta.transaction.UserTransaction} status of the transaction is tracked with
     * {@link jakarta.transaction.Synchronization}
     */
    MANAGED,

    /**
     * The default transaction mode. In this mode standard JDBC connection transactions are used
     * via {@link Connection#commit()} and {@link Connection#rollback()}.
     */
    AUTO
}
