/*
 * Copyright 2017 requery.io
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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

public class BinaryType extends BasicType<byte[]> {

    public BinaryType() {
        super(byte[].class, Types.BINARY);
    }

    @Override
    public byte[] fromResult(ResultSet results, int column) throws SQLException {
        return results.getBytes(column);
    }

    @Override
    public Keyword getIdentifier() {
        return Keyword.BINARY;
    }

    @Override
    public boolean hasLength() {
        return true;
    }
}
