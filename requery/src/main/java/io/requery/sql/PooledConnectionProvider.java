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

import javax.sql.ConnectionPoolDataSource;
import javax.sql.PooledConnection;
import java.sql.Connection;
import java.sql.SQLException;

class PooledConnectionProvider implements ConnectionProvider {

    private final ConnectionPoolDataSource dataSource;

    PooledConnectionProvider(ConnectionPoolDataSource dataSource) {
        this.dataSource = Objects.requireNotNull(dataSource);
    }

    @Override
    public Connection getConnection() throws SQLException {
        PooledConnection connection = dataSource.getPooledConnection();
        return connection.getConnection();
    }
}
