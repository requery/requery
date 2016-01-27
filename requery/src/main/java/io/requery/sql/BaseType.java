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

import io.requery.util.Objects;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Base {@link FieldType} implementation, providing basic read/write operations that can be
 * overridden.
 *
 * @param <T> mapped type
 *
 * @author Nikhil Purushe
 */
public abstract class BaseType<T> implements FieldType<T> {

    private final Class<T> type;
    private final int sqlType;

    protected BaseType(Class<T> type, int sqlType) {
        this.type = type;
        this.sqlType = sqlType;
    }

    @Override
    public T read(ResultSet results, int column) throws SQLException {
        T value = type.cast(results.getObject(column));
        if (results.wasNull()) {
            return null;
        }
        return value;
    }

    @Override
    public void write(PreparedStatement statement, int index, T value) throws SQLException {
        if (value == null) {
            statement.setNull(index, sqlType());
        } else {
            statement.setObject(index, value, sqlType());
        }
    }

    @Override
    public int sqlType() {
        return sqlType;
    }

    @Override
    public boolean hasLength() {
        return false;
    }

    @Override
    public Integer defaultLength() {
        return null;
    }

    public abstract Object identifier();

    @Override
    public String identifierSuffix() {
        return null;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof FieldType) {
            FieldType other = (FieldType) obj;
            return Objects.equals(identifier(), other.identifier()) &&
                sqlType() == other.sqlType() &&
                hasLength() == other.hasLength() &&
                Objects.equals(identifierSuffix(), other.identifierSuffix()) &&
                Objects.equals(defaultLength(), other.defaultLength());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            identifier(), sqlType(), defaultLength(), identifierSuffix());
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(identifier());
        if (hasLength()) {
            sb.append("(");
            sb.append(defaultLength());
            sb.append(")");
        }
        if (identifierSuffix() != null) {
            sb.append(" ");
            sb.append(identifierSuffix());
        }
        return sb.toString();
    }
}
