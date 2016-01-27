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

package io.requery.sql.platform;

import io.requery.PersistenceException;
import io.requery.sql.Platform;
import io.requery.util.function.Function;

import java.sql.Connection;
import java.sql.SQLException;

class PlatformFromConnection implements Function<Connection, Platform> {

    @Override
    public Platform apply(Connection connection) {
        Platform platform;
        String product;
        try {
            product = connection.getMetaData().getDatabaseProductName();
        } catch (SQLException e) {
            throw new PersistenceException(e);
        }
        if (product.contains("PostgreSQL")) {
            platform = new PostgresSQL();
        } else if (product.contains("SQLite")) {
            platform = new SQLite();
        } else if (product.contains("MySQL")) {
            platform = new MySQL();
        } else if (product.contains("H2")) {
            platform = new H2();
        } else if (product.contains("HSQL Database Engine")) {
            platform = new HSQL();
        } else if (product.contains("Apache Derby")) {
            platform = new Derby();
        } else if (product.contains("Oracle")) {
            platform = new Oracle();
        } else if (product.contains("Microsoft SQL Server")) {
            platform = new SQLServer();
        } else {
            platform = new Generic();
        }
        return platform;
    }
}
