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

package io.requery.sql.type;

import io.requery.sql.Keyword;
import io.requery.sql.BasicType;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Date;

public class JavaDateType extends BasicType<Date> {

    public JavaDateType() {
        super(Date.class, Types.DATE);
    }

    @Override
    public Date fromResult(ResultSet results, int column) throws SQLException {
        return results.getDate(column);
    }

    @Override
    public void write(PreparedStatement statement, int index, Date value)
        throws SQLException {
        int sqlType = getSqlType();
        if (value == null) {
            statement.setNull(index, sqlType);
        } else {
            long time = value.getTime();
            statement.setDate(index, new java.sql.Date(time));
        }
    }

    @Override
    public Keyword getIdentifier() {
        return Keyword.DATE;
    }
}
