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
import io.requery.meta.Type;
import io.requery.proxy.EntityProxy;
import io.requery.util.Objects;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;
import javax.transaction.TransactionSynchronizationRegistry;
import javax.transaction.UserTransaction;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;

/**
 * Transaction for use when running in a container or managed environment.
 *
 * @author Nikhil Purushe
 */
class ManagedTransaction implements EntityTransaction, ConnectionProvider, Synchronization {

    private final ConnectionProvider connectionProvider;
    private final TransactionListener transactionListener;
    private final TransactionEntitiesSet entities;
    private Connection connection;
    private Connection uncloseableConnection;
    private TransactionSynchronizationRegistry registry;
    private UserTransaction userTransaction;
    private boolean committed;
    private boolean rolledBack;
    private boolean initiatedTransaction;
    private boolean completed;

    ManagedTransaction(TransactionListener transactionListener,
                       ConnectionProvider connectionProvider,
                       EntityCache cache) {
        this.transactionListener = Objects.requireNotNull(transactionListener);
        this.connectionProvider = Objects.requireNotNull(connectionProvider);
        this.entities = new TransactionEntitiesSet(cache);
    }

    private TransactionSynchronizationRegistry getSynchronizationRegistry() {
        if (registry == null) {
            try {
                registry = InitialContext.doLookup("java:comp/TransactionSynchronizationRegistry");
            } catch (NamingException e) {
                throw new TransactionException(e);
            }
        }
        return registry;
    }

    private UserTransaction getUserTransaction() {
        if (userTransaction == null) {
            try {
                userTransaction = InitialContext.doLookup("java:comp/UserTransaction");
            } catch (NamingException e) {
                throw new TransactionException(e);
            }
        }
        return userTransaction;
    }

    @Override
    public Connection getConnection() {
        return uncloseableConnection;
    }

    @Override
    public Transaction begin() {
        if (active()) {
            throw new IllegalStateException("transaction already active");
        }
        transactionListener.beforeBegin(null);
        int status = getSynchronizationRegistry().getTransactionStatus();
        if (status == Status.STATUS_NO_TRANSACTION) {
            try {
                getUserTransaction().begin();
                initiatedTransaction = true;
            } catch (NotSupportedException | SystemException e) {
                throw new TransactionException(e);
            }
        }
        getSynchronizationRegistry().registerInterposedSynchronization(this);
        try {
            connection = connectionProvider.getConnection();
        } catch (SQLException e) {
            throw new TransactionException(e);
        }
        uncloseableConnection = new UncloseableConnection(connection);
        committed = false;
        rolledBack = false;
        entities.clear();
        transactionListener.afterBegin(null);
        return this;
    }

    @Override
    public Transaction begin(TransactionIsolation isolation) {
        if (isolation != null) {
            throw new TransactionException("isolation can't be specified in managed mode");
        }
        return begin();
    }

    @Override
    public void close() {
        if (connection != null) {
            if (!committed && !rolledBack) {
                rollback();
            }
            try {
                connection.close();
            } catch (SQLException ignored) {
            } finally {
                connection = null;
            }
        }
    }

    @Override
    public void commit() {
        if (initiatedTransaction) {
            try {
                transactionListener.beforeCommit(entities.types());
                getUserTransaction().commit();
                transactionListener.afterCommit(entities.types());
            } catch (RollbackException | SystemException | HeuristicMixedException |
                HeuristicRollbackException e) {
                throw new TransactionException(e);
            }
        }
        try {
            entities.clear();
        } finally {
            close();
        }
    }

    @Override
    public void rollback() {
        if (!rolledBack) {
            try {
                if (!completed) {
                    transactionListener.beforeRollback(entities.types());
                    if (initiatedTransaction) {
                        try {
                            getUserTransaction().rollback();
                        } catch (SystemException e) {
                            throw new TransactionException(e);
                        }
                    } else if (active()) {
                        getSynchronizationRegistry().setRollbackOnly();
                    }
                    transactionListener.afterRollback(entities.types());
                }
            } finally {
                rolledBack = true;
                entities.clearAndInvalidate();
            }
        }
    }

    @Override
    public boolean active() {
        TransactionSynchronizationRegistry registry = getSynchronizationRegistry();
        return registry != null && registry.getTransactionStatus() == Status.STATUS_ACTIVE;
    }

    @Override
    public void beforeCompletion() {
    }

    @Override
    public void afterCompletion(int status) {
        switch (status) {
            case Status.STATUS_ROLLEDBACK:
            case Status.STATUS_MARKED_ROLLBACK:
            case Status.STATUS_ROLLING_BACK:
                rollback();
                close();
                break;
        }
        completed = true;
    }


    @Override
    public void addToTransaction(EntityProxy<?> proxy) {
        entities.add(proxy);
    }

    @Override
    public void addToTransaction(Collection<Type<?>> types) {
        entities.types().addAll(types);
    }
}
