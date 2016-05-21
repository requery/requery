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
import io.requery.proxy.EntityBuilderProxy;
import io.requery.proxy.EntityProxy;
import io.requery.proxy.Initializer;
import io.requery.proxy.PropertyLoader;
import io.requery.proxy.PropertyState;
import io.requery.proxy.QueryInitializer;
import io.requery.proxy.Settable;
import io.requery.query.AliasedExpression;
import io.requery.query.Condition;
import io.requery.query.Expression;
import io.requery.query.Functional;
import io.requery.query.Result;
import io.requery.query.Tuple;
import io.requery.query.WhereAndOr;
import io.requery.query.element.QueryElement;
import io.requery.query.element.QueryType;
import io.requery.util.FilteringIterator;
import io.requery.util.Objects;
import io.requery.util.function.Consumer;
import io.requery.util.function.Predicate;
import io.requery.util.function.Supplier;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
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
    private final QueryAttribute<E, ?> keyAttribute;
    private final boolean stateless;
    private final boolean cacheable;
    private final Set<Expression<?>> defaultSelection;
    private final Attribute<E, ?>[] defaultSelectionAttributes;

    EntityReader(Type<E> type, EntityContext<S> context, Queryable<S> queryable) {
        this.type = Objects.requireNotNull(type);
        this.context = Objects.requireNotNull(context);
        this.queryable = Objects.requireNotNull(queryable);
        this.cache = this.context.cache();
        this.mapping = this.context.mapping();
        this.stateless = type.isStateless();
        this.cacheable = type.isCacheable();
        // compute default/minimum selections for the type
        LinkedHashSet<Expression<?>> selection = new LinkedHashSet<>();
        LinkedHashSet<Attribute<E, ?>> selectAttributes = new LinkedHashSet<>();
        for (Attribute<E, ?> attribute : type.attributes()) {
            if (!attribute.isLazy() && (!attribute.isAssociation() || attribute.isForeignKey())) {
                if (attribute.isVersion()) {
                    Expression<?> expression = aliasVersion(attribute);
                    selection.add(expression);
                } else {
                    selection.add((Expression)attribute);
                }
                selectAttributes.add(attribute);
            }
        }
        defaultSelection = Collections.unmodifiableSet(selection);
        // optimization for single key attribute
        keyAttribute = Attributes.query(type.singleKeyAttribute());
        // attributes converted to array for performance
        defaultSelectionAttributes = Attributes.attributesToArray(selectAttributes,
            new Predicate<Attribute<E, ?>>() {
            @Override
            public boolean test(Attribute<E, ?> value) {
                return true;
            }
        });
    }

    Set<Expression<?>> defaultSelection() {
        return defaultSelection;
    }

    Attribute<E, ?>[] defaultSelectionAttributes() {
        return defaultSelectionAttributes;
    }

    ResultReader<E> newResultReader(Attribute[] attributes) {
        if (type.isBuildable()) {
            return new BuildableEntityResultReader<>(this, attributes);
        } else {
            return new EntityResultReader<>(this, attributes);
        }
    }

    private Expression aliasVersion(Attribute attribute) {
        // special handling for system version column
        String columnName = context.platform().versionColumnDefinition().columnName();
        if (attribute.isVersion() && columnName != null) {
            Expression<?> expression = (Expression<?>) attribute;
            return new AliasedExpression<>(expression, columnName, expression.name());
        }
        return (Expression) attribute;
    }

    @Override
    public <V> void load(E entity, EntityProxy<E> proxy, Attribute<E, V> attribute) {
        refresh(entity, proxy, attribute);
    }

    public E refresh(E entity, EntityProxy<E> proxy) {
        // refresh only the attributes that were loaded...
        final Set<Attribute<E, ?>> refreshAttributes = new LinkedHashSet<>();
        for (Attribute<E, ?> attribute : type.attributes()) {
            if (stateless || proxy.getState(attribute) == PropertyState.LOADED) {
                refreshAttributes.add(attribute);
            }
        }
        return refresh(entity, proxy, refreshAttributes);
    }

    public E refreshAll(E entity, EntityProxy<E> proxy) {
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

    private E refresh(E entity, EntityProxy<E> proxy, final Set<Attribute<E, ?>> attributes) {

        Predicate<Attribute<E, ?>> basicFilter = new Predicate<Attribute<E, ?>>() {
            @Override
            public boolean test(Attribute<E, ?> value) {
                return attributes.contains(value) &&
                        (!value.isAssociation() || value.isForeignKey());
            }
        };
        FilteringIterator<Attribute<E, ?>> filterator =
            new FilteringIterator<>(attributes.iterator(), basicFilter);
        if (filterator.hasNext()) {
            QueryBuilder qb = new QueryBuilder(context.queryBuilderOptions())
                .keyword(SELECT)
                .commaSeparated(filterator, new QueryBuilder.Appender<Attribute<E, ?>>() {
                    @Override
                    public void append(QueryBuilder qb, Attribute<E, ?> value) {
                        String versionColumn = context.platform()
                                .versionColumnDefinition().columnName();
                        if (value.isVersion() && versionColumn != null) {
                            qb.append(versionColumn).space()
                                    .append(AS).space()
                                    .append(value.name()).space();
                        } else {
                            qb.attribute(value);
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
                    Attribute[] selection = new Attribute[attributes.size()];
                    attributes.toArray(selection);
                    // if the type is immutable create a new entity and return it, otherwise
                    // modify the given entity
                    if (type.isImmutable()) {
                        entity = fromBuilder(results, selection);
                    } else {
                        entity = fromResult(entity, results, selection);
                    }
                }
            } catch (SQLException e) {
                throw new PersistenceException(e);
            }
        }
        // refresh associations
        for (Attribute<E, ?> attribute : attributes) {
            // if it's a foreign key its resolved as part of the basic properties
            if (attribute.isAssociation()) {
                refreshAssociation(proxy, attribute);
            }
        }
        return entity;
    }

    private <V> void refreshAssociation(EntityProxy<E> proxy, Attribute<E, V> attribute) {
        Supplier<Result<S>> query = associativeQuery(proxy, attribute);
        switch (attribute.cardinality()) {
            case ONE_TO_ONE:
            case MANY_TO_ONE:
                S value = query == null ? null : query.get().firstOrNull();
                proxy.set(attribute, attribute.classType().cast(value), PropertyState.LOADED);
                break;
            case ONE_TO_MANY:
            case MANY_TO_MANY:
                Initializer<E, V> initializer = attribute.initializer();
                if (initializer instanceof QueryInitializer) {
                    @SuppressWarnings("unchecked")
                    QueryInitializer<E, V> queryInitializer = (QueryInitializer<E, V>) initializer;
                    V result = queryInitializer.initialize(proxy, attribute, query);
                    proxy.set(attribute, result, PropertyState.LOADED);
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
            case ONE_TO_MANY:
            case MANY_TO_ONE: {
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
                return order(queryable.select(uType).where(keyAttribute.equal(key)),
                    attribute.orderByAttribute());
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
                return order(queryable.select(uType)
                    .join(junctionType.classType()).on(uId.equal(uKey))
                    .join(type.classType()).on(tKey.equal(tId))
                    .where(tId.equal(id)), attribute.orderByAttribute());
            }
            default:
                throw new IllegalStateException();
        }
    }

    private <Q extends S> Supplier<Result<Q>> order(WhereAndOr<Result<Q>> query,
                                                    Supplier<Attribute> supplier) {
        if (supplier != null) {
            Attribute attribute = supplier.get();
            if (attribute.orderByDirection() != null && attribute instanceof Functional) {
                switch (attribute.orderByDirection()) {
                    case ASC:
                        query.orderBy(((Functional)attribute).asc());
                        break;
                    case DESC:
                        query.orderBy(((Functional)attribute).desc());
                        break;
                }
            } else {
                query.orderBy((Expression)attribute);
            }
        }
        return query;
    }

    @SafeVarargs
    final Iterable<E> batchRefresh(Iterable<E> entities, Attribute<E, ?>... attributes) {
        // if the type is immutable return a new collection with the rebuilt objects
        final Collection<E> collection = type.isImmutable() ? new ArrayList<E>() : null;

        if (keyAttribute == null) {
            // non optimal case objects with multiple keys or no keys
            for (E entity : entities) {
                entity = refresh(entity, type.proxyProvider().apply(entity), attributes);
                if (collection != null) {
                    collection.add(entity);
                }
            }
        } else {
            Set<Expression<?>> selection = new LinkedHashSet<>();
            Attribute[] selectAttributes;
            if (attributes == null || attributes.length == 0) {
                selection = defaultSelection;
                selectAttributes = defaultSelectionAttributes;
            } else {
                LinkedHashSet<Attribute> selectedAttributes = new LinkedHashSet<>();
                selection.add(keyAttribute);
                selectedAttributes.add(keyAttribute);
                for (Attribute<E, ?> attribute : attributes) {
                    if (attribute.isVersion()) {
                        selection.add(aliasVersion(attribute));
                    } else if (!attribute.isAssociation()) {
                        QueryAttribute<E, ?> queryAttribute = Attributes.query(attribute);
                        selection.add(queryAttribute);
                    }
                    selectedAttributes.add(attribute);
                }
                selectAttributes = selectedAttributes.toArray(new Attribute[selection.size()]);
            }
            Map<Object, EntityProxy<E>> map = new HashMap<>();
            for (E entity : entities) {
                EntityProxy<E> proxy = type.proxyProvider().apply(entity);
                Object key = proxy.key();
                if (key == null) {
                    throw new MissingKeyException();
                }
                map.put(key, proxy);
            }
            Condition<?, ?> condition = Attributes.query(keyAttribute).in(map.keySet());
            if (type.isCacheable()) {
                final Consumer<E> collector = new Consumer<E>() {
                    @Override
                    public void accept(E e) {
                        if (collection != null) {
                            collection.add(e);
                        }
                    }
                };
                // readResult will merge the results into the target object in cache mode
                ResultReader<E> resultReader = newResultReader(selectAttributes);

                SelectOperation<E> select = new SelectOperation<>(context, resultReader);
                QueryElement<Result<E>> query =
                    new QueryElement<>(QueryType.SELECT, context.model(), select);

                try (Result<E> result = query.select(selection).where(condition).get()) {
                    result.each(collector);
                }
            } else {
                try (Result<Tuple> result = queryable.select(selection).where(condition).get()) {
                    for (Tuple tuple : result) {
                        Object key = tuple.get((Expression) keyAttribute);
                        EntityProxy<E> proxy = map.get(key);
                        synchronized (proxy.syncObject()) {
                            for (Expression expression: selection) {
                                Object value = tuple.get(expression);
                                if (expression instanceof AliasedExpression) {
                                    AliasedExpression aliased = (AliasedExpression) expression;
                                    expression = aliased.innerExpression();
                                }
                                Attribute<E, Object> attribute =
                                    Attributes.query((Attribute) expression);
                                proxy.set(attribute, value, PropertyState.LOADED);
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
        return collection == null ? entities : collection;
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
                LinkedHashMap<Attribute<E, ?>, Object> keys = new LinkedHashMap<>(count);
                for (Attribute<E, ?> attribute : type.keyAttributes()) {
                    String name = attribute.name();
                    int column = results.findColumn(name);
                    Object value = mapping.read((Expression) attribute, results, column);
                    keys.put(attribute, value);
                }
                key = new CompositeKey<>(keys);
            }
        }
        return key;
    }

    final E fromResult(E entity, ResultSet results, Attribute[] selection) throws SQLException {
        // if refreshing (entity not null) overwrite the properties
        boolean overwrite = entity != null || stateless;
        boolean wasCached = false;

        if (entity == null) {
            // get or create the entity object
            if (cacheable) {
                synchronized (type) {
                    // try lookup cached object
                    final Object key = readCacheKey(results);
                    if (key != null) {
                        entity = cache.get(type.classType(), key);
                        wasCached = entity != null;
                    }
                    // not cached create a new one
                    if (entity == null) {
                        entity = createEntity();
                        if (key != null) {
                            cache.put(type.classType(), key, entity);
                        }
                    }
                }
            } else {
                entity = createEntity();
            }
        }

        // set the properties
        EntityProxy<E> proxy = type.proxyProvider().apply(entity);
        synchronized (proxy.syncObject()) {
            proxy.link(this);
            int index = 1;
            for (Attribute expression : selection) {
                @SuppressWarnings("unchecked")
                Attribute<E, ?> attribute = (Attribute<E, ?>) expression;

                if (attribute.isForeignKey() && attribute.isAssociation()) {
                    // handle loading the foreign key into referenced object
                    Attribute referenced = Attributes.get(attribute.referencedAttribute());

                    Object key = mapping.read((Expression) referenced, results, index);
                    if (key != null) {
                        Object value = proxy.get(attribute, false);
                        if (value == null) {
                            // create one...
                            Class classType = attribute.classType();
                            EntityReader reader = context.read(classType);
                            value = reader.createEntity();
                        }
                        context.proxyOf(value, false)
                            .set(Attributes.get(attribute.referencedAttribute()), key,
                                PropertyState.LOADED);

                        // leave in fetch state if only key is loaded
                        PropertyState state = PropertyState.LOADED;
                        if (!stateless) {
                            state = proxy.getState(attribute);
                            state = state == PropertyState.LOADED ? state : PropertyState.FETCH;
                        }
                        proxy.setObject(attribute, value, state);
                    }
                } else if (attribute.isAssociation()) {
                    continue;
                } else if (overwrite || proxy.getState(attribute) != PropertyState.MODIFIED) {
                    if (attribute.primitiveKind() != null) {
                        readPrimitiveField(proxy, attribute, results, index);
                    } else {
                        Object value = mapping.read((Expression) attribute, results, index);
                        proxy.setObject(attribute, value, PropertyState.LOADED);
                    }
                }
                index++;
            }
        }
        if (!wasCached) {
            context.stateListener().postLoad(entity, proxy);
        }
        return entity;
    }

    final <B> E fromBuilder(ResultSet results, Attribute[] selection) throws SQLException {
        EntityBuilderProxy<B, E> proxy = new EntityBuilderProxy<>(type);
        int index = 1;
        for (Attribute expression : selection) {
            @SuppressWarnings("unchecked")
            Attribute<E, ?> attribute = (Attribute<E, ?>) expression;
            if (attribute.primitiveKind() != null) {
                readPrimitiveField(proxy, attribute, results, index);
            } else {
                Object value = mapping.read((Expression) attribute, results, index);
                proxy.setObject(attribute, value, PropertyState.LOADED);
            }
            index++;
        }
        return proxy.build();
    }

    @SuppressWarnings("unchecked") // checked by primitiveKind
    private void readPrimitiveField(Settable<E> proxy,
                                    Attribute<E, ?> attribute,
                                    ResultSet results, int index) throws SQLException {
        switch (attribute.primitiveKind()) {
            case INT:
                Attribute<E, Integer> intAttribute = (Attribute<E, Integer>) attribute;
                int intValue = mapping.readInt(results, index);
                proxy.setInt(intAttribute, intValue, PropertyState.LOADED);
                break;
            case LONG:
                Attribute<E, Long> longAttribute = (Attribute<E, Long>) attribute;
                long longValue = mapping.readLong(results, index);
                proxy.setLong(longAttribute, longValue, PropertyState.LOADED);
                break;
            case SHORT:
                Attribute<E, Short> shortAttribute = (Attribute<E, Short>) attribute;
                short shortValue = mapping.readShort(results, index);
                proxy.setShort(shortAttribute, shortValue, PropertyState.LOADED);
                break;
            case BYTE:
                Attribute<E, Byte> byteAttribute = (Attribute<E, Byte>) attribute;
                byte byteValue = mapping.readByte(results, index);
                proxy.setByte(byteAttribute, byteValue, PropertyState.LOADED);
                break;
            case BOOLEAN:
                Attribute<E, Boolean> booleanAttribute = (Attribute<E, Boolean>) attribute;
                boolean booleanValue = mapping.readBoolean(results, index);
                proxy.setBoolean(booleanAttribute, booleanValue, PropertyState.LOADED);
                break;
            case FLOAT:
                Attribute<E, Float> floatAttribute = (Attribute<E, Float>) attribute;
                float floatValue = mapping.readFloat(results, index);
                proxy.setFloat(floatAttribute, floatValue, PropertyState.LOADED);
                break;
            case DOUBLE:
                Attribute<E, Double> doubleAttribute = (Attribute<E, Double>) attribute;
                double doubleValue = mapping.readDouble(results, index);
                proxy.setDouble(doubleAttribute, doubleValue, PropertyState.LOADED);
                break;
        }
    }
}
