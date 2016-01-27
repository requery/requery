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

import io.requery.sql.type.BigIntType;
import io.requery.sql.type.BinaryType;
import io.requery.sql.type.BlobType;
import io.requery.sql.type.BooleanType;
import io.requery.sql.type.ClobType;
import io.requery.sql.type.DateType;
import io.requery.sql.type.DecimalType;
import io.requery.sql.type.FloatType;
import io.requery.sql.type.IntegerType;
import io.requery.sql.type.NVarCharType;
import io.requery.sql.type.RealType;
import io.requery.sql.type.SmallIntType;
import io.requery.sql.type.TimeStampType;
import io.requery.sql.type.TimeType;
import io.requery.sql.type.TinyIntType;
import io.requery.sql.type.JavaDateType;
import io.requery.sql.type.VarBinaryType;
import io.requery.sql.type.VarCharType;

public class BasicTypes {
    private BasicTypes() {
    }
    public static final BigIntType BIGINT = new BigIntType();
    public static final BinaryType BINARY = new BinaryType();
    public static final BlobType BLOB = new BlobType();
    public static final BooleanType BOOLEAN = new BooleanType();
    public static final ClobType CLOB = new ClobType();
    public static final DateType DATE = new DateType();
    public static final JavaDateType JAVA_DATE = new JavaDateType();
    public static final DecimalType DECIMAL = new DecimalType();
    public static final FloatType FLOAT = new FloatType();
    public static final IntegerType INTEGER = new IntegerType();
    public static final RealType REAL = new RealType();
    public static final SmallIntType SMALLINT = new SmallIntType();
    public static final TimeType TIME = new TimeType();
    public static final TimeStampType TIMESTAMP = new TimeStampType();
    public static final TinyIntType TINYINT = new TinyIntType();
    public static final VarBinaryType VARBINARY = new VarBinaryType();
    public static final VarCharType VARCHAR = new VarCharType();
    public static final NVarCharType NVARCHAR = new NVarCharType();
}
