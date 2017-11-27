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

package io.requery.sql;

import io.requery.Converter;
import io.requery.converter.CurrencyConverter;
import io.requery.converter.EnumStringConverter;
import io.requery.converter.LocalDateConverter;
import io.requery.converter.LocalDateTimeConverter;
import io.requery.converter.LocalTimeConverter;
import io.requery.converter.OffsetDateTimeConverter;
import io.requery.converter.URIConverter;
import io.requery.converter.URLConverter;
import io.requery.converter.UUIDConverter;
import io.requery.converter.ZonedDateTimeConverter;
import io.requery.meta.Attribute;
import io.requery.query.Expression;
import io.requery.query.ExpressionType;
import io.requery.query.function.Function;
import io.requery.sql.type.BigIntType;
import io.requery.sql.type.BinaryType;
import io.requery.sql.type.BlobType;
import io.requery.sql.type.BooleanType;
import io.requery.sql.type.ClobType;
import io.requery.sql.type.DateType;
import io.requery.sql.type.DecimalType;
import io.requery.sql.type.FloatType;
import io.requery.sql.type.IntegerType;
import io.requery.sql.type.JavaDateType;
import io.requery.sql.type.PrimitiveBooleanType;
import io.requery.sql.type.PrimitiveByteType;
import io.requery.sql.type.PrimitiveDoubleType;
import io.requery.sql.type.PrimitiveFloatType;
import io.requery.sql.type.PrimitiveIntType;
import io.requery.sql.type.PrimitiveLongType;
import io.requery.sql.type.PrimitiveShortType;
import io.requery.sql.type.RealType;
import io.requery.sql.type.SmallIntType;
import io.requery.sql.type.TimeStampType;
import io.requery.sql.type.TimeType;
import io.requery.sql.type.TinyIntType;
import io.requery.sql.type.VarBinaryType;
import io.requery.sql.type.VarCharType;
import io.requery.util.ClassMap;
import io.requery.util.LanguageVersion;
import io.requery.util.Objects;

import java.math.BigDecimal;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Comparator;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Default mapping of Java types to Persisted types.
 *
 * @author Nikhil Purushe
 */
public class GenericMapping implements Mapping {

    private static final Comparator<Class<?>> CLASS_NAME_COMPARATOR = new Comparator<Class<?>>() {
        @Override
        public int compare(Class<?> o1, Class<?> o2) {
            return o1.getName().compareTo(o2.getName());
        }
    };

    private final ClassMap<FieldType> types;
    private final ClassMap<FieldType> fixedTypes;
    private final ClassMap<Converter<?, ?>> converters;
    private final Map<Attribute, FieldType> resolvedTypes;
    private final ClassMap<Function.Name> functionTypes;
    private PrimitiveIntType primitiveIntType;
    private PrimitiveLongType primitiveLongType;
    private PrimitiveShortType primitiveShortType;
    private PrimitiveByteType primitiveByteType;
    private PrimitiveBooleanType primitiveBooleanType;
    private PrimitiveFloatType primitiveFloatType;
    private PrimitiveDoubleType primitiveDoubleType;

    public GenericMapping() {
        types = new ClassMap<>();
        primitiveIntType = new IntegerType(int.class);
        primitiveLongType = new BigIntType(long.class);
        primitiveShortType = new SmallIntType(short.class);
        primitiveBooleanType = new BooleanType(boolean.class);
        primitiveFloatType = new FloatType(float.class);
        primitiveDoubleType = new RealType(double.class);
        primitiveByteType = new TinyIntType(byte.class);
        types.put(boolean.class, new BooleanType(boolean.class));
        types.put(Boolean.class, new BooleanType(Boolean.class));
        types.put(int.class, new IntegerType(int.class));
        types.put(Integer.class, new IntegerType(Integer.class));
        types.put(short.class, new SmallIntType(short.class));
        types.put(Short.class, new SmallIntType(Short.class));
        types.put(byte.class, new TinyIntType(byte.class));
        types.put(Byte.class, new TinyIntType(Byte.class));
        types.put(long.class, new BigIntType(long.class));
        types.put(Long.class, new BigIntType(Long.class));
        types.put(float.class, new FloatType(float.class));
        types.put(Float.class, new FloatType(Float.class));
        types.put(double.class, new RealType(double.class));
        types.put(Double.class, new RealType(Double.class));
        types.put(BigDecimal.class, new DecimalType());
        types.put(byte[].class, new VarBinaryType());
        types.put(java.util.Date.class, new JavaDateType());
        types.put(java.sql.Date.class, new DateType());
        types.put(Time.class, new TimeType());
        types.put(Timestamp.class, new TimeStampType());
        types.put(String.class, new VarCharType());
        types.put(Blob.class, new BlobType());
        types.put(Clob.class, new ClobType());

        fixedTypes = new ClassMap<>();
        fixedTypes.put(byte[].class, new BinaryType());
        functionTypes = new ClassMap<>();
        converters = new ClassMap<>();
        resolvedTypes = new IdentityHashMap<>();
        Set<Converter> converters = new HashSet<>();
        converters.add(new EnumStringConverter<>(Enum.class));
        converters.add(new UUIDConverter());
        converters.add(new URIConverter());
        converters.add(new URLConverter());
        converters.add(new CurrencyConverter());
        if (LanguageVersion.current().atLeast(LanguageVersion.JAVA_1_8)) {
            converters.add(new LocalDateConverter());
            converters.add(new LocalTimeConverter());
            converters.add(new LocalDateTimeConverter());
            converters.add(new ZonedDateTimeConverter());
            converters.add(new OffsetDateTimeConverter());
        }
        for (Converter converter : converters) {
            Class mapped = converter.getMappedType();
            if (!types.containsKey(mapped)) {
                this.converters.put(mapped, converter);
            }
        }
    }

    @Override
    public Mapping aliasFunction(Function.Name name, Class<? extends Function> function) {
        functionTypes.put(function, name);
        return this;
    }

    @Override
    public <T> Mapping replaceType(int sqlType, FieldType<T> replacementType) {
        Objects.requireNotNull(replacementType);
        replace(types, sqlType, replacementType);
        replace(fixedTypes, sqlType, replacementType);
        return this;
    }

    private void replace(ClassMap<FieldType> map, int sqlType, FieldType replace) {
        Set<Class<?>> keys = new LinkedHashSet<>();
        for (Map.Entry<Class<?>, FieldType> entry : map.entrySet()) {
            if (entry.getValue().getSqlType() == sqlType) {
                keys.add(entry.getKey());
            }
        }
        for (Class<?> type : keys) {
            map.put(type, replace);
        }
        // check if the replacement type replaces any of the primitive types
        if (sqlType == primitiveIntType.getSqlType() && replace instanceof PrimitiveIntType) {
            primitiveIntType = (PrimitiveIntType) replace;
        } else if (
            sqlType == primitiveLongType.getSqlType() && replace instanceof PrimitiveLongType) {
            primitiveLongType = (PrimitiveLongType) replace;
        } else if (
            sqlType == primitiveShortType.getSqlType() && replace instanceof PrimitiveShortType) {
            primitiveShortType = (PrimitiveShortType) replace;
        } else if (
            sqlType == primitiveBooleanType.getSqlType() && replace instanceof PrimitiveBooleanType) {
            primitiveBooleanType = (PrimitiveBooleanType) replace;
        } else if (
            sqlType == primitiveFloatType.getSqlType() && replace instanceof PrimitiveFloatType) {
            primitiveFloatType = (PrimitiveFloatType) replace;
        } else if (
            sqlType == primitiveDoubleType.getSqlType() && replace instanceof PrimitiveDoubleType) {
            primitiveDoubleType = (PrimitiveDoubleType) replace;
        } else if (
            sqlType == primitiveByteType.getSqlType() && replace instanceof PrimitiveByteType) {
            primitiveByteType = (PrimitiveByteType) replace;
        }
    }

    @Override
    public <T> Mapping putType(Class<? super T> type, FieldType<T> fieldType) {
        if (type == null) {
            throw new IllegalArgumentException();
        }
        if (fieldType == null) {
            throw new IllegalArgumentException();
        }
        types.put(type, fieldType);
        return this;
    }

    Converter<?, ?> converterForType(Class<?> type) {
        Converter<?, ?> converter = converters.get(type);
        if (converter == null && type.isEnum()) {
            converter = converters.get(Enum.class);
        }
        return converter;
    }

    @Override
    public FieldType mapAttribute(Attribute<?, ?> attribute) {
        FieldType fieldType = resolvedTypes.get(attribute);
        if (fieldType != null) {
            return fieldType;
        }
        Class<?> type = attribute.getClassType();
        if (attribute.isAssociation() && attribute.getReferencedAttribute() != null) {
            type = attribute.getReferencedAttribute().get().getClassType();
        }
        if (attribute.getConverter() != null) {
            Converter<?, ?> converter = attribute.getConverter();
            type = converter.getPersistedType();
        }
        fieldType = getSubstitutedType(type);
        resolvedTypes.put(attribute, fieldType);
        return fieldType;
    }

    @Override
    public Function.Name mapFunctionName(Function<?> function) {
        Function.Name name = functionTypes.get(function.getClass());
        return name != null ? name : function.getFunctionName();
    }

    @Override
    public Set<Class<?>> typesOf(int sqlType) {
        Set<Class<?>> types = new TreeSet<>(CLASS_NAME_COMPARATOR);
        for (Map.Entry<Class<?>, FieldType> entry : this.types.entrySet()) {
            if (entry.getValue().getSqlType() == sqlType) {
                types.add(entry.getKey());
            }
        }
        if (types.isEmpty()) {
            types.add(String.class); // fall back to string value
        }
        return types;
    }

    private FieldType getSubstitutedType(Class<?> type) {
        FieldType fieldType = null;
        // check conversion
        Converter<?, ?> converter = converterForType(type);
        if (converter != null) {
            if (converter.getPersistedSize() != null) {
                fieldType = fixedTypes.get(converter.getPersistedType());
            }
            type = converter.getPersistedType();
        }
        if (fieldType == null) {
            fieldType = types.get(type);
        }
        return fieldType == null ? new VarCharType(): fieldType;
    }

    @Override
    public <A> A read(Expression<A> expression, ResultSet results, int column) throws SQLException {
        Class<A> type;
        Converter<?, ?> converter = null;
        FieldType fieldType;
        if (expression.getExpressionType() == ExpressionType.ATTRIBUTE) {
            @SuppressWarnings("unchecked")
            Attribute<?, A> attribute = (Attribute) expression;
            converter = attribute.getConverter();
            type = attribute.getClassType();
            fieldType = mapAttribute(attribute);
        } else if (expression.getExpressionType() == ExpressionType.ALIAS) {
            @SuppressWarnings("unchecked")
            Attribute<?, A> attribute = (Attribute) expression.getInnerExpression();
            converter = attribute.getConverter();
            type = attribute.getClassType();
            fieldType = mapAttribute(attribute);
        } else {
            type = expression.getClassType();
            fieldType = getSubstitutedType(type);
        }
        boolean isPrimitive = type.isPrimitive();
        if (converter == null && !isPrimitive) {
            converter = converterForType(type);
        }
        Object value = fieldType.read(results, column);
        // if the type is primitive the wasNull check isn't performed by the type, check here
        if (isPrimitive && results.wasNull()) {
            value = null;
        }
        if (converter != null) {
            value = toMapped((Converter) converter, type, value);
        }
        if (isPrimitive) {
            // cast primitive types only into their boxed type
            @SuppressWarnings("unchecked")
            A boxed = (A) value;
            return boxed;
        } else {
            return type.cast(value);
        }
    }

    @Override
    public boolean readBoolean(ResultSet results, int column) throws SQLException {
        return primitiveBooleanType.readBoolean(results, column);
    }

    @Override
    public byte readByte(ResultSet results, int column) throws SQLException {
        return primitiveByteType.readByte(results, column);
    }

    @Override
    public short readShort(ResultSet results, int column) throws SQLException {
        return primitiveShortType.readShort(results, column);
    }

    @Override
    public int readInt(ResultSet results, int column) throws SQLException {
        return primitiveIntType.readInt(results, column);
    }

    @Override
    public long readLong(ResultSet results, int column) throws SQLException {
        return primitiveLongType.readLong(results, column);
    }

    @Override
    public float readFloat(ResultSet results, int column) throws SQLException {
        return primitiveFloatType.readFloat(results, column);
    }

    @Override
    public double readDouble(ResultSet results, int column) throws SQLException {
        return primitiveDoubleType.readDouble(results, column);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <A> void write(Expression<A> expression, PreparedStatement statement, int index,
                          A value) throws SQLException {
        Class<?> type;
        Converter converter = null;
        FieldType fieldType;
        if (expression.getExpressionType() == ExpressionType.ATTRIBUTE) {
            Attribute<?, A> attribute = (Attribute) expression;
            converter = attribute.getConverter();
            fieldType = mapAttribute(attribute);
            type = attribute.isAssociation() ?
                    attribute.getReferencedAttribute().get().getClassType() :
                    attribute.getClassType();
        } else {
            type = expression.getClassType();
            fieldType = getSubstitutedType(type);
        }
        if (converter == null && !type.isPrimitive()) {
            converter = converterForType(type);
        }
        Object converted = value;
        if (converter != null) {
            converted = converter.convertToPersisted(value);
        }
        fieldType.write(statement, index, converted);
    }

    @Override
    public void writeBoolean(PreparedStatement statement, int index, boolean value)
        throws SQLException {
        primitiveBooleanType.writeBoolean(statement, index, value);
    }

    @Override
    public void writeByte(PreparedStatement statement, int index, byte value) throws SQLException {
        primitiveByteType.writeByte(statement, index, value);
    }

    @Override
    public void writeShort(PreparedStatement statement, int index, short value)
        throws SQLException {
        primitiveShortType.writeShort(statement, index, value);
    }

    @Override
    public void writeInt(PreparedStatement statement, int index, int value) throws SQLException {
        primitiveIntType.writeInt(statement, index, value);
    }

    @Override
    public void writeLong(PreparedStatement statement, int index, long value) throws SQLException {
        primitiveLongType.writeLong(statement, index, value);
    }

    @Override
    public void writeFloat(PreparedStatement statement, int index, float value)
        throws SQLException {
        primitiveFloatType.writeFloat(statement, index, value);
    }

    @Override
    public void writeDouble(PreparedStatement statement, int index, double value)
        throws SQLException {
        primitiveDoubleType.writeDouble(statement, index, value);
    }

    public void addConverter(Converter<?, ?> converter, Class<?>... classes) {
        converters.put(converter.getMappedType(), converter);
        // optional additional mapped classes
        for (Class<?> type : classes) {
            converters.put(type, converter);
        }
    }

    private static <A, B> A toMapped(Converter<A, B> converter, Class<? extends A> type, B value) {
        return converter.convertToMapped(type, value);
    }
}
