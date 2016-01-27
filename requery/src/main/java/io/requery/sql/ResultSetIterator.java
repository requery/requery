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

import io.requery.query.Expression;
import io.requery.util.CloseableIterator;
import io.requery.util.IndexAccessible;
import io.requery.util.Objects;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.Statement;
import java.sql.Wrapper;
import java.util.NoSuchElementException;
import java.util.Set;

/**
 * {@link java.util.Iterator} implementation that operates over an {@link java.sql.ResultSet}
 * providing an interface to convert the current result set row into a concrete Java object.
 *
 * @param <E> type of element returned by this iterator
 *
 * @author Nikhil Purushe
 */
public class ResultSetIterator<E> implements CloseableIterator<E>, IndexAccessible<E>, Wrapper {

    private final Set<? extends Expression<?>> selection;
    private final ResultSet results;
    private final ResultReader<E> reader;
    private final boolean closeStatement;
    private int position;
    private boolean closed;
    private boolean advanced;

    ResultSetIterator(ResultReader<E> reader,
                      ResultSet results,
                      Set<? extends Expression<?>> selection,
                      boolean closeStatement) {
        this.reader = Objects.requireNotNull(reader);
        this.results = Objects.requireNotNull(results);
        this.selection = selection;
        this.closeStatement = closeStatement;
    }

    @Override
    public boolean hasNext() {
        if (closed) {
            return false;
        }
        // result set advanced but next() not called yet
        if (advanced) {
            return true;
        } else {
            try {
                if (results.next()) {
                    return advanced = true;
                } else {
                    // close ourselves
                    close();
                    return false;
                }
            } catch (SQLException e) {
                return false;
            }
        }
    }

    @Override
    public E next() {
        if (closed) {
            throw new IllegalStateException();
        }
        try {
            if (!advanced) {
                // move forward
                if (!results.next()) {
                    advanced = false;
                    close();
                    throw new NoSuchElementException();
                }
            } // else already ready to read
            E value = reader.read(results, selection);
            position++;
            advanced = false;
            return value;
        } catch (SQLException e) {
            NoSuchElementException exception = new NoSuchElementException();
            exception.initCause(e);
            throw exception;
        }
    }

    @Override
    public E get(int index) {
        if (index < 0) {
            throw new IndexOutOfBoundsException();
        }
        try {
            // if the result set type is TYPE_FORWARD_ONLY, this will throw
            if (results.absolute(index + 1)) {
                position = index;
                if (results.rowDeleted()) {
                    return null;
                }
                return reader.read(results, selection);
            } else {
                throw new UnsupportedOperationException();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public int position() {
        return position;
    }

    @Override
    public void remove() {
        try {
            if (results.isBeforeFirst()) {
                throw new IllegalStateException();
            }
            if (results.getConcurrency() == ResultSet.CONCUR_UPDATABLE) {
                results.deleteRow();
            } else {
                throw new UnsupportedOperationException();
            }
        } catch (SQLFeatureNotSupportedException e) {
            throw new UnsupportedOperationException(e);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void close() {
        synchronized (results) {
            if (!closed) {
                Statement statement = null;
                if (closeStatement) {
                    try {
                        statement = results.getStatement();
                    } catch (SQLException ignored) {
                    }
                }
                closeSuppressed(results);
                if (statement != null) {
                    Connection connection = null;
                    try {
                        connection = statement.getConnection();
                    } catch (SQLException ignored) {
                    }
                    closeSuppressed(statement);
                    closeSuppressed(connection);
                }
                closed = true;
            }
        }
    }

    private static void closeSuppressed(AutoCloseable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (Exception ignored) {
            }
        }
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        return results.unwrap(iface);
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return results.isWrapperFor(iface);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ResultSetIterator) {
            ResultSetIterator iterator = (ResultSetIterator) obj;
            return iterator.results == results;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(results);
    }
}
