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


import io.requery.EntityCache;
import io.requery.PersistenceException;
import io.requery.Queryable;
import io.requery.meta.Attribute;
import io.requery.meta.QueryAttribute;
import io.requery.meta.Type;
import io.requery.proxy.CompositeKey;
import io.requery.proxy.EntityProxy;
import io.requery.proxy.Initializer;
import io.requery.proxy.Property;
import io.requery.proxy.PropertyLoader;
import io.requery.proxy.PropertyState;
import io.requery.proxy.QueryInitializer;
import io.requery.query.AliasedExpression;
import io.requery.query.Condition;
import io.requery.query.Expression;
import io.requery.query.Result;
import io.requery.query.Tuple;
import io.requery.util.Objects;
import io.requery.util.function.Consumer;
import io.requery.util.function.Predicate;
import io.requery.util.function.Supplier;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import static io.requery.sql.Keyword.AS;
import static io.requery.sql.Keyword.FROM;
import static io.requery.sql.Keyword.SELECT;
import static io.requery.sql.Keyword.WHERE;

/**
 * Handles refreshing and loading attributes for {@link io.requery.Entity} instances.
 *
 * @param <E> the entity type
 * @param <S> generic type from which all entities extend
 *
 * @author Nikhil Purushe
 */
class EntityReader<E extends S, S> implements PropertyLoader<E> {

    private final EntityCache cache;
    private final Type<E> type;
    private final Mapping mapping;
    private final EntityContext<S> context;
    private final Queryable<S> queryable;
    private final Set<Expression<?>> defaultSelection;
    private final Attribute<E, ?> keyAttribute;

    EntityReader(Type<E> type, EntityContext<S> context, Queryable<S> queryable) {
        this.type = Objects.requireNotNull(type);
        this.context = Objects.requireNotNull(context);
        this.queryable = Objects.requireNotNull(queryable);
        this.cache = this.context.cache();
        this.mapping = this.context.mapping();
        // compute default/minimum selections for the type
        LinkedHashSet<Expression<?>> selectAttributes = new LinkedHashSet<>();
        for (Attribute<E, ?> attribute : type.attributes()) {
            if (!attribute.isLazy() && !attribute.isAssociation()) {
                if (attribute.isVersion()) {
                    Expression<?> expression = aliasVersion(attribute);
                    selectAttributes.add(expression);
                } else {
                    selectAttributes.add(attribute);
                }
            }
        }
        defaultSelection = Collections.unmodifiableSet(selectAttributes);
        // optimization for single key attribute
        int count = type.keyAttributes().size();
        if (count == 1) {
            keyAttribute = type.keyAttributes().iterator().next();
        } else {
            keyAttribute = null;
        }
    }

    Set<Expression<?>> defaultSelection() {
        return defaultSelection;
    }

    private Expression aliasVersion(Attribute attribute) {
        // special handling for system version column
        String columnName = context.platform().versionColumnDefinition().columnName();
        if (attribute.isVersion() && columnName != null) {
            Expression<?> expression = (Expression<?>) attribute;
            return new AliasedExpression<>(expression, columnName, expression.name());
        }
        return attribute;
    }

    @Override
    public <V> void load(E entity, EntityProxy<E> proxy, Property<E, V> property) {
        refresh(entity, proxy, property.attribute());
    }

    public E refresh(E entity, EntityProxy<E> proxy) {
        // refresh only the attributes that were loaded...
        final Set<Attribute<E, ?>> refreshAttributes = new HashSet<>();
        for (Property<E, ?> property : proxy) {
            if(property.state() == PropertyState.LOADED) {
                refreshAttributes.add(property.attribute());
            }
        }
        return refresh(entity, proxy, refreshAttributes);
    }

    E refreshAll(E entity, EntityProxy<E> proxy) {
        return refresh(entity, proxy, type.attributes());
    }

    @SafeVarargs
    public final E refresh(E entity, EntityProxy<E> proxy, Attribute<E, ?>... attributes) {
        if (attributes == null || attributes.length == 0) {
            return entity;
        }
        Set<Attribute<E, ?>> elements;
        if (attributes.length == 1) {
            elements = Collections.<Attribute<E, ?>>singleton(attributes[0]);
        } else {
            elements = new LinkedHashSet<>(attributes.length);
            Collections.addAll(elements, attributes);
        }
        return refresh(entity, proxy, elements);
    }

    private E refresh(E entity, EntityProxy<E> proxy,
                      final Set<? extends Expression<?>> attributes) {

        Predicate<Property<E, ?>> basicFilter = new Predicate<Property<E, ?>>() {
            @Override
            public boolean test(Property<E, ?> value) {
                Attribute attribute = value.attribute();
                return attributes.contains(attribute) &&
                        (!attribute.isAssociation() || attribute.isForeignKey());
            }
        };

        Iterator<Property<E, ?>> iterator = proxy.filterProperties(basicFilter).iterator();
        if (iterator.hasNext()) {
            QueryBuilder qb = new QueryBuilder(context.queryBuilderOptions())
                .keyword(SELECT)
                .commaSeparated(iterator, new QueryBuilder.Appender<Property<E, ?>>() {
                    @Override
                    public void append(QueryBuilder qb, Property<E, ?> value) {
                        Attribute attribute = value.attribute();
                        String versionColumn = context.platform()
                                .versionColumnDefinition().columnName();
                        if (attribute.isVersion() && versionColumn != null) {
                            qb.append(versionColumn).space()
                                    .append(AS).space()
                                    .append(value.attribute().name()).space();
                        } else {
                            qb.attribute(value.attribute());
                        }
                    }
                })
                .keyword(FROM)
                .tableName(type.name())
                .keyword(WHERE)
                .appendWhereConditions(type.keyAttributes());

            String sql = qb.toString();
            try (Connection connection = context.connectionProvider().getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {
                int index = 1;
                for (Attribute<E, ?> attribute : type.keyAttributes()) {
                    Object value = proxy.get(attribute, false);
                    if (value == null) {
                        throw new MissingKeyException(proxy);
                    }
                    mapping.write((Expression) attribute, statement, index++, value);
                }
                context.statementListener().beforeExecuteQuery(statement, sql, null);
                ResultSet results = statement.executeQuery();
                context.statementListener().afterExecuteQuery(statement);
                if (results.next()) {
                    fromResult(entity, results, basicFilter);
                }
            } catch (SQLException e) {
                throw new PersistenceException(e);
            }
        }
        // refresh associations
        for (Expression expression : attributes) {
            if (expression instanceof Attribute) {
                @SuppressWarnings("unchecked")
                Attribute<E, ?> attribute = (Attribute) expression;
                if (attribute.isAssociation()) {
                    refreshAssociation(proxy, attribute);
                }
            }
        }
        return entity;
    }

    private <V> void refreshAssociation(EntityProxy<E> proxy, Attribute<E, V> attribute) {
        Supplier<Result<S>> query = associativeQuery(proxy, attribute);
        switch (attribute.cardinality()) {
            case ONE_TO_ONE:
                S value = query == null ? null : query.get().firstOrNull();
                proxy.set(attribute, attribute.classType().cast(value), PropertyState.LOADED);
                break;
            case ONE_TO_MANY:
            case MANY_TO_MANY:
                Initializer<V> initializer = attribute.fieldAccess().initializer();
                if (initializer instanceof QueryInitializer) {
                    @SuppressWarnings("unchecked")
                    QueryInitializer<V> queryInitializer = (QueryInitializer<V>) initializer;
                    queryInitializer.initialize(proxy.propertyOf(attribute), query);
                }
                break;
            default:
                throw new IllegalStateException();
        }
    }

    private <Q extends S> Supplier<Result<Q>> associativeQuery(EntityProxy<E> proxy,
                                                               Attribute<E, ?> attribute) {
        switch (attribute.cardinality()) {
            case ONE_TO_ONE:
            case ONE_TO_MANY: {
                Object key;
                QueryAttribute<Q, Object> keyAttribute;
                Class<Q> uType;
                if (attribute.isForeignKey()) {
                    keyAttribute = Attributes.get(attribute.referencedAttribute());
                    uType = keyAttribute.declaringType().classType();
                    Q entity = uType.cast(proxy.get(attribute, false));
                    if (entity == null) {
                        return null;
                    }
                    EntityProxy<Q> referredProxy = context.model()
                        .typeOf(uType).proxyProvider().apply(entity);

                    key = referredProxy.get(keyAttribute);
                } else {
                    keyAttribute = Attributes.get(attribute.mappedAttribute());
                    uType = keyAttribute.declaringType().classType();
                    Attribute<E, ?> referenced = Attributes.get(
                        keyAttribute.referencedAttribute());
                    key = proxy.get(referenced);
                }
                return queryable.select(uType).where(keyAttribute.equal(key));
            }
            case MANY_TO_MANY: {
                @SuppressWarnings("unchecked")
                Class<Q> uType = (Class<Q>) attribute.elementClass();
                QueryAttribute<E, Object> tKey = null;
                QueryAttribute<Q, Object> uKey = null;
                Type<?> junctionType = context.model().typeOf(attribute.referencedClass());
                for (Attribute a : junctionType.attributes()) {
                    if (type.classType().isAssignableFrom(a.referencedClass())) {
                        tKey = Attributes.query(a);
                    } else if (uType.isAssignableFrom(a.referencedClass())) {
                        uKey = Attributes.query(a);
                    }
                }
                Objects.requireNotNull(tKey);
                Objects.requireNotNull(uKey);
                QueryAttribute<E, Object> tId = Attributes.get(tKey.referencedAttribute());
                QueryAttribute<Q, Object> uId = Attributes.get(uKey.referencedAttribute());
                Object id = proxy.get(tId);
                if (id == null) {
                    throw new IllegalStateException();
                }
                // create the many to many join query
                return queryable.select(uType)
                    .join(junctionType.classType()).on(uId.equal(uKey))
                    .join(type.classType()).on(tKey.equal(tId))
                    .where(tId.equal(id));
            }
            case MANY_TO_ONE:
            default:
                throw new IllegalStateException();
        }
    }

    @SafeVarargs
    final void batchRefresh(Iterable<E> entities, Attribute<E, ?>... attributes) {
        Set<? extends Expression<?>> expressions;
        if (attributes == null || attributes.length == 0) {
            expressions = defaultSelection;
        } else {
            Set<Attribute<E, ?>> set = new LinkedHashSet<>(attributes.length);
            if (keyAttribute != null) {
                set.add(keyAttribute);
            }
            for (Attribute<E, ?> attribute : attributes) {
                if (!attribute.isAssociation()) {
                    set.add(attribute);
                }
            }
            expressions = set;
        }
        if (keyAttribute == null) {
            // non optimal case objects with multiple keys or no keys
            for (E entity : entities) {
                refresh(entity, type.proxyProvider().apply(entity), expressions);
            }
        } else {
            Map<Object, EntityProxy<E>> map = new HashMap<>();
            for (E entity : entities) {
                EntityProxy<E> proxy = type.proxyProvider().apply(entity);
                Object key = proxy.key();
                if (key == null) {
                    throw new MissingKeyException();
                }
                map.put(key, proxy);
            }
            @SuppressWarnings("unchecked")
            Set<QueryAttribute<E, ?>> selection = (Set<QueryAttribute<E, ?>>) expressions;
            Condition<Object> condition = Attributes.query(keyAttribute).in(map.keySet());
            if (type.isCacheable()) {
                final Consumer<E> empty = new Consumer<E>() {
                    @Override
                    public void accept(E e) {
                    }
                };
                // readResult will merge the results into the target object in cache mode
                try (Result<E> result =
                         queryable.select(type.classType(), selection).where(condition).get()) {

                    result.each(empty);
                }
            } else {
                try (Result<Tuple> result = queryable.select(selection).where(condition).get()) {
                    for (Tuple tuple : result) {
                        Object key = tuple.get(keyAttribute);
                        EntityProxy<E> proxy = map.get(key);
                        synchronized (proxy.syncObject()) {
                            for (Expression expression : expressions) {
                                if (expression instanceof Attribute) {
                                    Attribute<E, Object> attribute =
                                        Attributes.query((Attribute) expression);
                                    Object value = tuple.get(expression);
                                    proxy.set(attribute, value, PropertyState.LOADED);
                                }
                            }
                        }
                    }
                }
            }

            // associations TODO can be optimized
            if (attributes != null) {
                for (Attribute<E, ?> attribute : attributes) {
                    if (attribute.isAssociation()) {
                        for (EntityProxy<E> proxy : map.values()) {
                            refreshAssociation(proxy, attribute);
                        }
                    }
                }
            }
        }
    }

    private E createEntity() {
        E entity = type.factory().get();
        EntityProxy<E> proxy = type.proxyProvider().apply(entity);
        proxy.link(this);
        return entity;
    }

    private Object readCacheKey(ResultSet results) throws SQLException {
        Object key = null;
        if (keyAttribute != null) { // common case 1 primary key
            String name = keyAttribute.name();
            int index = results.findColumn(name);
            key = mapping.read((Expression) keyAttribute, results, index);
        } else {
            int count = type.keyAttributes().size();
            if (count > 1) {
                Object[] keys = new Object[count];
                int index = 0;
                for (Attribute attribute : type.keyAttributes()) {
                    String name = attribute.name();
                    Object value = mapping.read(attribute, results, results.findColumn(name));
                    keys[index++] = value;
                }
                key = new CompositeKey(keys);
            }
        }
        return key;
    }

    E fromResult(E entity, ResultSet results, Predicate<Property<E, ?>> filter)
            throws SQLException {
        // if refreshing (entity not null) overwrite the properties
        boolean overwrite = entity != null;

        // get or create the entity object
        boolean wasCached = false;
        if (entity == null) {
            Object key = null;
            synchronized (type) {
                // try lookup cached object
                if (type.isCacheable()) {
                    key = readCacheKey(results);
                    if (key != null) {
                        entity = cache.get(type.classType(), key);
                        wasCached = entity != null;
                    }
                }
                // not cached create a new one
                if (entity == null) {
                    entity = createEntity();
                    if (key != null) {
                        cache.put(type.classType(), key, entity);
                    }
                }
            }
        }

        // set the properties
        EntityProxy<E> proxy = type.proxyProvider().apply(entity);
        synchronized (proxy.syncObject()) {
            if (!proxy.isLinked()) {
                proxy.link(this);
            }
            int index = 1;
            for (Property<E, ?> property : proxy) {
                if (!filter.test(property)) {
                    continue;
                }
                Attribute attribute = property.attribute();
                // handle loading the foreign key into referenced object
                if (attribute.isAssociation() && attribute.isForeignKey()) {

                    Attribute referenced = Attributes.get(attribute.referencedAttribute());

                    Object key = mapping.read(referenced, results, index);
                    if (key != null) {
                        Object value = property.get();
                        if (value == null) {
                            // create one...
                            EntityReader reader = context.read(attribute.classType());
                            value = reader.createEntity();
                        }
                        context.proxyOf(value, false)
                            .set(Attributes.get(attribute.referencedAttribute()), key,
                                PropertyState.LOADED);

                        // leave in fetch state if only key is loaded
                        PropertyState state = property.state();
                        state = state == PropertyState.LOADED ? state : PropertyState.FETCH;
                        property.setObject(value, state);
                    }

                } else if (overwrite || property.state() != PropertyState.MODIFIED) {

                    Object value = mapping.read(property.attribute(), results, index);
                    property.setObject(value, PropertyState.LOADED);
                }
                index++;
            }
        }

        if (!wasCached) {
            context.stateListener().postLoad(entity, proxy);
        }
        return entity;
    }
}
