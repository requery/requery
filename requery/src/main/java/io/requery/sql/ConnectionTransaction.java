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

import io.requery.EntityCache;
import io.requery.Transaction;
import io.requery.TransactionException;
import io.requery.TransactionIsolation;
import io.requery.TransactionListener;
import io.requery.proxy.EntityProxy;
import io.requery.util.Objects;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * {@link Transaction} implementation using JDBC {@link Connection} operations
 * {@link Connection#commit()} and {@link Connection#rollback()}.
 *
 * @author Nikhil Purushe
 */
class ConnectionTransaction implements EntityProxyTransaction, ConnectionProvider {

    private final ConnectionProvider connectionProvider;
    private final EntityCache cache;
    private final Set<EntityProxy<?>> entities;
    private final Set<EntityProxy<?>> entitiesReadOnly;
    private final TransactionListener transactionListener;
    private final boolean supportsTransaction;
    private Connection connection;
    private Connection uncloseableConnection;
    private boolean committed;
    private boolean rolledBack;
    private int previousIsolationLevel;

    ConnectionTransaction(TransactionListener transactionListener,
                          ConnectionProvider connectionProvider,
                          EntityCache cache,
                          boolean supportsTransaction) {
        this.transactionListener = Objects.requireNotNull(transactionListener);
        this.connectionProvider = Objects.requireNotNull(connectionProvider);
        this.cache = Objects.requireNotNull(cache);
        this.supportsTransaction = supportsTransaction;
        this.entities = new LinkedHashSet<>();
        this.entitiesReadOnly = Collections.unmodifiableSet(entities);
        this.previousIsolationLevel = -1;
    }

    @Override
    public Connection getConnection() {
        return uncloseableConnection;
    }

    @Override
    public Transaction begin() {
        return begin(null);
    }

    @Override
    public Transaction begin(TransactionIsolation isolation) {
        if (active()) {
            throw new IllegalStateException("transaction already active");
        }
        try {
            transactionListener.beforeBegin(isolation);
            connection = connectionProvider.getConnection();
            uncloseableConnection = new UncloseableConnection(connection);
            if (supportsTransaction) {
                connection.setAutoCommit(false);
                if (isolation != null) {
                    previousIsolationLevel = connection.getTransactionIsolation();
                    int level;
                    switch (isolation) {
                        case NONE:
                            level = Connection.TRANSACTION_NONE;
                            break;
                        case READ_UNCOMMITTED:
                            level = Connection.TRANSACTION_READ_UNCOMMITTED;
                            break;
                        case READ_COMMITTED:
                            level = Connection.TRANSACTION_READ_COMMITTED;
                            break;
                        case REPEATABLE_READ:
                            level = Connection.TRANSACTION_REPEATABLE_READ;
                            break;
                        case SERIALIZABLE:
                            level = Connection.TRANSACTION_SERIALIZABLE;
                            break;
                        default:
                            throw new UnsupportedOperationException();
                    }
                    connection.setTransactionIsolation(level);
                }
            }
            committed = false;
            rolledBack = false;
            entities.clear();
            transactionListener.afterBegin(isolation);
        } catch (SQLException e) {
            throw new TransactionException(e);
        }
        return this;
    }

    @Override
    public void close() {
        if (connection != null) {
            if (!committed && !rolledBack) {
                try {
                    rollback();
                } catch (Exception ignored) {
                }
            }
            try {
                connection.close();
            } catch (SQLException e) {
                throw new TransactionException(e);
            } finally {
                connection = null;
            }
        }
    }

    @Override
    public void commit() {
        try {
            transactionListener.beforeCommit(entitiesReadOnly);
            if (supportsTransaction) {
                connection.commit();
                committed = true;
            }
            transactionListener.afterCommit(entitiesReadOnly);
            entities.clear();
        } catch (SQLException e) {
            throw new TransactionException(e);
        } finally {
            try {
                connection.setAutoCommit(true);
                // restore default isolation level
                if (previousIsolationLevel != -1) {
                    connection.setTransactionIsolation(previousIsolationLevel);
                }
            } catch (SQLException ignored) {
            }
            close();
        }
    }

    @Override
    public void rollback() {
        try {
            transactionListener.beforeRollback(entitiesReadOnly);
            if (supportsTransaction) {
                connection.rollback();
                rolledBack = true;
                // detach objects in this transaction
                for (EntityProxy<?> proxy : entities) {
                    proxy.unlink();
                    Object key = proxy.key();
                    if (key != null) {
                        cache.invalidate(proxy.type().classType(), key);
                    }
                }
            }
            transactionListener.afterRollback(entitiesReadOnly);
            entities.clear();
        } catch (SQLException e) {
            throw new TransactionException(e);
        } finally {
            try {
                connection.setAutoCommit(true);
            } catch (SQLException ignored) {
            }
        }
    }

    @Override
    public boolean active() {
        try {
            return connection != null && !connection.getAutoCommit();
        } catch (SQLException e) {
            return false;
        }
    }

    @Override
    public void addToTransaction(EntityProxy<?> proxy) {
        entities.add(proxy);
    }
}
