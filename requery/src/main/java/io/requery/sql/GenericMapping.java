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

import io.requery.Converter;
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
import io.requery.util.ClassMap;
import io.requery.util.LanguageVersion;
import io.requery.util.Objects;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Default mapping of Java types to Persisted types.
 *
 * @author Nikhil Purushe
 */
public class GenericMapping implements Mapping {

    private final ClassMap<FieldType> types;
    private final ClassMap<FieldType> fixedTypes;
    private final ClassMap<FieldType> substitutedTypes;
    private final ClassMap<Converter<?, ?>> converters;

    public GenericMapping(Platform platform) {
        types = new ClassMap<>();
        types.put(boolean.class, BasicTypes.BOOLEAN);
        types.put(Boolean.class, BasicTypes.BOOLEAN);
        types.put(int.class, BasicTypes.INTEGER);
        types.put(Integer.class, BasicTypes.INTEGER);
        types.put(short.class, BasicTypes.SMALLINT);
        types.put(Short.class, BasicTypes.SMALLINT);
        types.put(byte.class, BasicTypes.TINYINT);
        types.put(Byte.class, BasicTypes.TINYINT);
        types.put(long.class, BasicTypes.INTEGER);
        types.put(Long.class, BasicTypes.INTEGER);
        types.put(float.class, BasicTypes.FLOAT);
        types.put(Float.class, BasicTypes.FLOAT);
        types.put(double.class, BasicTypes.REAL);
        types.put(Double.class, BasicTypes.REAL);
        types.put(BigInteger.class, BasicTypes.BIGINT);
        types.put(BigDecimal.class, BasicTypes.DECIMAL);
        types.put(byte[].class, BasicTypes.VARBINARY);
        types.put(java.util.Date.class, BasicTypes.JAVA_DATE);
        types.put(java.sql.Date.class, BasicTypes.DATE);
        types.put(Time.class, BasicTypes.TIME);
        types.put(Timestamp.class, BasicTypes.TIMESTAMP);
        types.put(String.class, BasicTypes.VARCHAR);
        types.put(Blob.class, BasicTypes.BLOB);
        types.put(Clob.class, BasicTypes.CLOB);

        substitutedTypes = new ClassMap<>();
        fixedTypes = new ClassMap<>();
        fixedTypes.put(byte[].class, BasicTypes.BINARY);
        converters = new ClassMap<>();
        Set<Converter> converters = new HashSet<>();
        converters.add(new EnumStringConverter<>(Enum.class));
        converters.add(new UUIDConverter());
        converters.add(new URIConverter());
        converters.add(new URLConverter());
        if (LanguageVersion.current().atLeast(LanguageVersion.JAVA_1_8)) {
            converters.add(new LocalDateConverter());
            converters.add(new LocalTimeConverter());
            converters.add(new LocalDateTimeConverter());
            converters.add(new ZonedDateTimeConverter());
            converters.add(new OffsetDateTimeConverter());
        }
        platform.addMappings(this);
        for (Converter converter : converters) {
            Class mapped = converter.mappedType();
            if (!types.containsKey(mapped)) {
                this.converters.put(mapped, converter);
            }
        }
    }

    @Override
    public <T> Mapping replaceType(FieldType<T> basicType, FieldType<T> replacementType) {
        Objects.requireNotNull(basicType);
        Objects.requireNotNull(replacementType);
        replace(types, basicType, replacementType);
        replace(fixedTypes, basicType, replacementType);
        return this;
    }

    private static void replace(ClassMap<FieldType> map, FieldType current, FieldType replace) {
        Set<Class<?>> keys = new LinkedHashSet<>();
        for (Map.Entry<Class<?>, FieldType> entry : map.entrySet()) {
            if (entry.getValue().equals(current)) {
                keys.add(entry.getKey());
            }
        }
        for (Class<?> type : keys) {
            map.put(type, replace);
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
        Class<?> type = attribute.classType();
        if (attribute.isForeignKey()) {
            type = attribute.isAssociation() ?
                attribute.referencedAttribute().get().classType() :
                attribute.classType();
        }
        if (attribute.converter() != null) {
            Converter<?, ?> converter = attribute.converter();
            type = converter.persistedType();
        }
        return getSubstitutedType(type);
    }

    @Override
    public List<FieldType> mapCollectionAttribute(Attribute<?, ?> attribute) {
        List<FieldType> fieldTypes = new LinkedList<>();
        if (attribute.elementClass() != null) {
            if (attribute.mapKeyClass() != null) {
                FieldType keyType = types.get(attribute.mapKeyClass());
                fieldTypes.add(Objects.requireNotNull(keyType));
            }
            FieldType valueType = types.get(attribute.elementClass());
            fieldTypes.add(Objects.requireNotNull(valueType));
        }
        return fieldTypes;
    }

    private FieldType getSubstitutedType(Class<?> type) {
        // prefer platform substituted type
        FieldType fieldType = substitutedTypes.get(type);
        // check conversion
        if (fieldType == null) {
            Converter<?,?> converter = converterForType(type);
            if (converter != null) {
                if(converter.persistedSize() != null) {
                    fieldType = fixedTypes.get(converter.persistedType());
                }
                type = converter.persistedType();
            }
        }
        if (fieldType == null) {
            fieldType = types.get(type);
        }
        return fieldType == null ? BasicTypes.VARCHAR : fieldType;
    }

    @Override
    public <A> A read(Expression<A> expression, ResultSet results, int column) throws SQLException {
        Class<A> type;
        Converter<?, ?> converter = null;
        FieldType fieldType;
        if (expression instanceof Attribute) {
            Attribute<?, A> attribute = Attributes.query((Attribute) expression);
            converter = attribute.converter();
            type = attribute.classType();
            fieldType = mapAttribute(attribute);
        } else {
            type = expression.classType();
            fieldType = getSubstitutedType(type);
        }
        if (converter == null) {
            converter = converterForType(type);
        }
        Object value = fieldType.read(results, column);
        if (converter != null) {
            value = toMapped((Converter) converter, type, value);
        }
        return type.cast(value);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <A> void write(Expression<A> expression, PreparedStatement statement, int index,
                          A value) throws SQLException {
        Class<?> type;
        Converter converter = null;
        FieldType fieldType;
        if (expression instanceof Attribute) {
            Attribute<?, A> attribute = Attributes.query((Attribute) expression);
            converter = attribute.converter();
            fieldType = mapAttribute(attribute);
            type = attribute.isAssociation() ?
                    attribute.referencedAttribute().get().classType() :
                    attribute.classType();
        } else {
            type = expression.classType();
            fieldType = getSubstitutedType(type);
        }
        if (converter == null) {
            converter = converterForType(type);
        }
        Object converted;
        if (converter == null) {
            converted = value;
        } else {
            converted = converter.convertToPersisted(value);
        }
        fieldType.write(statement, index, converted);
    }

    public void addConverter(Converter<?, ?> converter, Class<?>... classes) {
        for (Class<?> type : classes) {
            converters.put(type, converter);
        }
    }

    private static <A, B> A toMapped(Converter<A, B> converter, Class<? extends A> type, B value) {
        return converter.convertToMapped(type, value);
    }
}
