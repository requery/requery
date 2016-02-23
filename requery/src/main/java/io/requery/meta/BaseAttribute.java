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

package io.requery.meta;

import io.requery.CascadeAction;
import io.requery.Converter;
import io.requery.ReferentialAction;
import io.requery.proxy.Getter;
import io.requery.proxy.Field;
import io.requery.proxy.Initializer;
import io.requery.proxy.Setter;
import io.requery.query.FieldExpression;
import io.requery.query.ExpressionType;
import io.requery.util.Objects;
import io.requery.util.function.Supplier;

import java.util.Collections;
import java.util.Set;

abstract class BaseAttribute<T, V> extends FieldExpression<V> implements
        QueryAttribute<T, V>, Field<T, V> {

    protected String name;
    protected Getter<T, V> getter;
    protected Setter<T, V> setter;
    protected Initializer<V> initializer;
    protected Class<V> classType;
    protected boolean isLazy;
    protected boolean isKey;
    protected boolean isUnique;
    protected boolean isGenerated;
    protected boolean isNullable;
    protected boolean isVersion;
    protected boolean isForeignKey;
    protected boolean isIndex;
    protected Integer length;
    protected String defaultValue;
    protected String indexName;
    protected String collate;
    protected Cardinality cardinality;
    protected ReferentialAction referentialAction;
    protected Set<CascadeAction> cascadeActions;
    protected Converter<V, ?> converter;
    protected Type<T> declaringType;
    protected Class<?> mapKeyClass;
    protected Class<?> elementClass;
    protected Class<?> referencedClass;
    protected Supplier<Attribute> mappedAttribute;
    protected Supplier<Attribute> referencedAttribute;
    protected int hashCode;

    @Override
    public Getter<T, V> getter() {
        return getter;
    }

    @Override
    public Setter<T, V> setter() {
        return setter;
    }

    @Override
    public Initializer<V> initializer() {
        return initializer;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public Class<V> classType() {
        return classType;
    }

    @Override
    public ExpressionType type() {
        return ExpressionType.ATTRIBUTE;
    }

    @Override
    public Type<T> declaringType() {
        return declaringType;
    }

    @Override
    public Field<T, V> fieldAccess() {
        return this;
    }

    @Override
    public boolean isLazy() {
        return isLazy;
    }

    @Override
    public Integer length() {
        return converter != null ? converter.persistedSize() : length;
    }

    @Override
    public boolean isAssociation() {
        return cardinality != null;
    }

    @Override
    public boolean isKey() {
        return isKey;
    }

    @Override
    public boolean isUnique() {
        return isUnique;
    }

    @Override
    public boolean isGenerated() {
        return isGenerated;
    }

    @Override
    public boolean isNullable() {
        return isNullable;
    }

    @Override
    public boolean isForeignKey() {
        return isForeignKey;
    }

    @Override
    public boolean isVersion() {
        return isVersion;
    }

    @Override
    public boolean isIndexed() {
        return isIndex;
    }

    @Override
    public String defaultValue() {
        return defaultValue;
    }

    @Override
    public String indexName() {
        return indexName;
    }

    @Override
    public String collate() {
        return collate;
    }

    @Override
    public Class<?> mapKeyClass() {
        return mapKeyClass;
    }

    @Override
    public Class<?> elementClass() {
        return elementClass;
    }

    @Override
    public Class<?> referencedClass() {
        return referencedClass;
    }

    @Override
    public Cardinality cardinality() {
        return cardinality;
    }

    @Override
    public ReferentialAction referentialAction() {
        return referentialAction;
    }

    @Override
    public Set<CascadeAction> cascadeActions() {
        return cascadeActions == null ? Collections.<CascadeAction>emptySet() : cascadeActions;
    }

    @Override
    public Converter<V, ?> converter() {
        return converter;
    }

    @Override
    public Supplier<Attribute> mappedAttribute() {
        return mappedAttribute;
    }

    @Override
    public Supplier<Attribute> referencedAttribute() {
        return referencedAttribute;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Attribute) {
            Attribute attribute = (Attribute) obj;
            return Objects.equals(name, attribute.name()) &&
                Objects.equals(classType, attribute.classType()) &&
                Objects.equals(declaringType, attribute.declaringType());
        }
        return false;
    }

    @Override
    public int hashCode() {
        if (hashCode == 0) {
            hashCode = Objects.hash(name, classType, declaringType);
        }
        return hashCode;
    }

    @Override
    public String toString() {
        return declaringType() == null ?
                name() : declaringType().name() + "." + name();
    }
}
