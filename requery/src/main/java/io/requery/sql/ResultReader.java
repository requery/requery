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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Set;

/**
 * Maps a {@link ResultSet} row to a given type.
 *
 * @param <R> type of object being read
 *
 * @author Nikhil Purushe
 */
interface ResultReader<R> {

    /**
     * Read a element from a {@link ResultSet} the result set will already be positioned
     * correctly. Implementations should not change the position.
     *
     * @param results   result set instance to read
     * @param selection set of selected expressions
     * @return the mapped element
     * @throws SQLException if data cannot be read from the result set
     */
    R read(ResultSet results, Set<? extends Expression<?>> selection) throws SQLException;
}
