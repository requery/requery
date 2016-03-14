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

public class TinyIntType extends BasicType<Byte> implements PrimitiveByteType {

    public TinyIntType(Class<Byte> type) {
        super(type, Types.TINYINT);
    }

    @Override
    public Byte fromResult(ResultSet results, int column) throws SQLException {
        return results.getByte(column);
    }

    @Override
    public Keyword identifier() {
        return Keyword.TINYINT;
    }

    @Override
    public byte readByte(ResultSet results, int column) throws SQLException {
        return results.getByte(column);
    }

    @Override
    public void writeByte(PreparedStatement statement, int index, byte value) throws SQLException {
        statement.setByte(index, value);
    }
}
