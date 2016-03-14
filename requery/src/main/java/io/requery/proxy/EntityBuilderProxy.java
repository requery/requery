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

package io.requery.proxy;

import io.requery.meta.Attribute;
import io.requery.meta.Type;
import io.requery.util.function.Supplier;

/**
 * Proxies a builder class that has attributes that can be set yielding a final constructed entity
 * using {@link #build()}.
 *
 * @param <B> type of the builder
 * @param <E> type of entity being constructed
 *
 * @author Nikhil Purushe
 */
@SuppressWarnings("unchecked") // builder class is purposefully not stored in Type
public class EntityBuilderProxy<B, E> implements Settable<E> {

    private final Type<E> type;
    private final B builder;

    public EntityBuilderProxy(Type<E> type) {
        Supplier<B> supplier = type.builderFactory();
        this.builder = supplier.get();
        this.type = type;
    }

    @Override
    public <V> void set(Attribute<E, V> attribute, V value) {
        set(attribute, value, PropertyState.LOADED);
    }

    @Override
    public <V> void set(Attribute<E, V> attribute, V value, PropertyState state) {
        setObject(attribute, value, state);
    }

    @Override
    public void setObject(Attribute<E, ?> attribute, Object value, PropertyState state) {
        Property<B, Object> property = (Property<B, Object>) attribute.builderProperty();
        property.set(builder, value);
    }

    @Override
    public void setBoolean(Attribute<E, Boolean> attribute, boolean value, PropertyState state) {
        BooleanProperty<B> property = (BooleanProperty<B>) attribute.builderProperty();
        property.setBoolean(builder, value);
    }

    @Override
    public void setDouble(Attribute<E, Double> attribute, double value, PropertyState state) {
        DoubleProperty<B> property = (DoubleProperty<B>) attribute.builderProperty();
        property.setDouble(builder, value);
    }

    @Override
    public void setFloat(Attribute<E, Float> attribute, float value, PropertyState state) {
        FloatProperty<B> property = (FloatProperty<B>) attribute.builderProperty();
        property.setFloat(builder, value);
    }

    @Override
    public void setByte(Attribute<E, Byte> attribute, byte value, PropertyState state) {
        ByteProperty<B> property = (ByteProperty<B>) attribute.builderProperty();
        property.setByte(builder, value);
    }

    @Override
    public void setShort(Attribute<E, Short> attribute, short value, PropertyState state) {
        ShortProperty<B> property = (ShortProperty<B>) attribute.builderProperty();
        property.setShort(builder, value);
    }

    @Override
    public void setInt(Attribute<E, Integer> attribute, int value, PropertyState state) {
        IntProperty<B> property = (IntProperty<B>) attribute.builderProperty();
        property.setInt(builder, value);
    }

    @Override
    public void setLong(Attribute<E, Long> attribute, long value, PropertyState state) {
        LongProperty<B> property = (LongProperty<B>) attribute.builderProperty();
        property.setLong(builder, value);
    }

    public E build() {
        return type.buildFunction().apply(builder);
    }
}
