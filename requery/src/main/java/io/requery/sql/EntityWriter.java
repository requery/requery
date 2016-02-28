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

import io.requery.CascadeAction;
import io.requery.EntityCache;
import io.requery.PersistenceException;
import io.requery.Queryable;
import io.requery.meta.Attribute;
import io.requery.meta.Cardinality;
import io.requery.meta.EntityModel;
import io.requery.meta.QueryAttribute;
import io.requery.meta.Type;
import io.requery.proxy.CollectionChanges;
import io.requery.proxy.EntityProxy;
import io.requery.proxy.PropertyState;
import io.requery.query.Deletion;
import io.requery.query.Expression;
import io.requery.query.FieldExpression;
import io.requery.query.Scalar;
import io.requery.query.Update;
import io.requery.query.Where;
import io.requery.query.element.QueryElement;
import io.requery.query.element.QueryType;
import io.requery.util.FilteringIterator;
import io.requery.util.Objects;
import io.requery.util.ObservableCollection;
import io.requery.util.function.Function;
import io.requery.util.function.Predicate;
import io.requery.util.function.Supplier;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Handles insert/update/delete operations for {@link io.requery.Entity} instances.
 *
 * @param <E> specific entity type
 * @param <S> generic type from which all entities extend
 *
 * @author Nikhil Purushe
 */
class EntityWriter<E extends S, S> implements ParameterBinder<E> {

    private final EntityCache cache;
    private final EntityModel model;
    private final Type<E> type;
    private final EntityContext<S> context;
    private final Mapping mapping;
    private final Queryable<S> queryable;
    private final boolean hasGeneratedKey;
    private final boolean hasForeignKeys;
    private final Attribute<E, ?> keyAttribute;
    private final Attribute<E, ?> versionAttribute;
    private final Attribute<E, ?>[] bindableAttributes;
    private final Attribute<E, ?>[] associativeAttributes;
    private final String[] generatedColumnNames;
    private final Class<E> entityClass;
    private final Function<E, EntityProxy<E>> proxyProvider;
    private final boolean cacheable;
    private final boolean stateless;

    EntityWriter(Type<E> type, EntityContext<S> context, Queryable<S> queryable) {
        this.type = Objects.requireNotNull(type);
        this.context = Objects.requireNotNull(context);
        this.queryable = Objects.requireNotNull(queryable);
        this.cache = this.context.cache();
        this.model = this.context.model();
        this.mapping = this.context.mapping();
        // check type attributes
        boolean hasGeneratedKey = false;
        boolean hasForeignKeys = false;
        Attribute<E, ?> versionAttribute = null;
        for (Attribute<E, ?> attribute : type.attributes()) {
            if (attribute.isKey() && attribute.isGenerated()) {
                hasGeneratedKey = true;
            }
            if (attribute.isVersion()) {
                versionAttribute = attribute;
            }
            if (attribute.isForeignKey()) {
                hasForeignKeys = true;
            }
        }
        this.hasGeneratedKey = hasGeneratedKey;
        this.hasForeignKeys = hasForeignKeys;
        this.versionAttribute = versionAttribute;
        this.keyAttribute = type.singleKeyAttribute();
        Collection<Attribute<E, ?>> keys = type.keyAttributes();
        generatedColumnNames = new String[keys.size()];
        int i = 0;
        for (Attribute attribute : keys) {
            generatedColumnNames[i++] = attribute.name();
        }
        entityClass = type.classType();
        proxyProvider = type.proxyProvider();
        cacheable = !type.keyAttributes().isEmpty() && type.isCacheable();
        stateless = type.isStateless();

        // create bindable attributes as an array for performance
        bindableAttributes = attributesToArray(new Predicate<Attribute<E, ?>>() {
            @Override
            public boolean test(Attribute<E, ?> value) {
                boolean isGeneratedKey = value.isGenerated() && value.isKey();
                boolean isSystemVersion = value.isVersion() && hasSystemVersionColumn();
                boolean isAssociation = value.isAssociation() && !value.isForeignKey();
                return !(isGeneratedKey || isSystemVersion) && !isAssociation;
            }
        });
        associativeAttributes = attributesToArray(new Predicate<Attribute<E, ?>>() {
            @Override
            public boolean test(Attribute<E, ?> value) {
                return value.isAssociation() && !value.isForeignKey();
            }
        });
    }

    private Attribute<E, ?>[] attributesToArray(Predicate<Attribute<E, ?>> predicate) {
        LinkedHashSet<Attribute> attributes = new LinkedHashSet<>();
        for (Attribute<E, ?> attribute : type.attributes()) {
            if (predicate.test(attribute)) {
                attributes.add(attribute);
            }
        }
        @SuppressWarnings("unchecked")
        Attribute<E, ?>[] array = new Attribute[attributes.size()];
        return attributes.toArray(array);
    }

    private void checkRowsAffected(int count, E entity, EntityProxy<E> proxy) {
        if (proxy != null && versionAttribute != null && count == 0) {
            Object version = proxy.get(versionAttribute);
            throw new OptimisticLockException(entity, version);
        } else if (count != 1) {
            throw new RowCountException(1, count);
        }
    }

    private boolean hasSystemVersionColumn() {
        return !context.platform().versionColumnDefinition().createColumn();
    }

    private boolean canBatchInStatement() {
        boolean supportsBatchStatement = context.supportsBatchUpdates();
        boolean supportsBatchGeneratedKey = context.platform().supportsGeneratedKeysInBatchUpdate();
        return hasGeneratedKey ?
            supportsBatchStatement && supportsBatchGeneratedKey : supportsBatchStatement;
    }

    private void cascadeBatch(Map<Class<? extends S>, List<S>> map) {
        for (Map.Entry<Class<? extends S>, List<S>> entry : map.entrySet()) {
            Class<? extends S> key = entry.getKey();
            context.write(key).batchInsert(entry.getValue());
        }
    }

    @SuppressWarnings("unchecked")
    private S foreignKeyReference(EntityProxy<E> proxy, Attribute<E, ?> attribute) {
        if (attribute.isForeignKey() && attribute.isAssociation()) {
            return (S) proxy.get(attribute);
        }
        return null;
    }

    private void findCascadePreInserts(EntityProxy<E> proxy,
                                       Map<Class<? extends S>, List<S>> elements) {
        for (Attribute<E, ?> attribute : type.attributes()) {
            S referenced = foreignKeyReference(proxy, attribute);
            if (referenced != null) {
                EntityProxy<S> otherProxy = context.proxyOf(referenced, false);
                if (otherProxy != null && !otherProxy.isLinked()) {
                    Class<? extends S> key = otherProxy.type().classType();
                    List<S> values = elements.get(key);
                    if (values == null) {
                        elements.put(key, values = new ArrayList<>());
                    }
                    values.add(referenced);
                }
            }
        }
    }

    void batchInsert(Iterable<E> entities) {
        // true if using JDBC batching
        final boolean batchInStatement = canBatchInStatement();
        final int batchSize = context.batchUpdateSize();
        final EntityReader<E, S> reader = context.read(entityClass);
        final Iterator<E> iterator = entities.iterator();

        int collectionSize = entities instanceof Collection ? ((Collection)entities).size() : -1;
        @SuppressWarnings("unchecked")
        final E[] elements = (E[]) new Object[Math.min(collectionSize, batchSize)];

        while (iterator.hasNext()) {
            int index = 0;
            Map<Class<? extends S>, List<S>> associations = new HashMap<>();
            while (iterator.hasNext() && index < batchSize) {
                E entity = iterator.next();
                EntityProxy<E> proxy = proxyProvider.apply(entity);
                elements[index] = entity;
                if (hasForeignKeys) {
                    findCascadePreInserts(proxy, associations);
                }
                if (versionAttribute != null && !hasSystemVersionColumn()) {
                    incrementVersion(proxy);
                }
                context.stateListener().preInsert(entity, proxy);
                index++;
            }
            cascadeBatch(associations);

            final int count = index;
            GeneratedResultReader generatedKeyReader = null;
            if (hasGeneratedKey) {
                generatedKeyReader = new GeneratedResultReader() {
                    @Override
                    public void read(int index, ResultSet results) throws SQLException {
                        if (batchInStatement) {
                            // reading batch keys
                            int keysRead = 0;
                            for (int i = 0; i < count ; i++) {
                                if (!results.next()) {
                                    throw new IllegalStateException();
                                }
                                EntityProxy<E> proxy = proxyProvider.apply(elements[i]);
                                readGeneratedKeys(proxy, results);
                                keysRead++;
                            }
                            if (keysRead != count) {
                                throw new IllegalStateException();
                            }
                        } else if (results.next()) {
                            // reading single generated key
                            E entity = elements[index];
                            EntityProxy<E> proxy = proxyProvider.apply(entity);
                            readGeneratedKeys(proxy, results);
                        }
                    }
                    @Override
                    public String[] generatedColumns() {
                        return generatedColumnNames;
                    }
                };
            }
            BatchUpdateOperation<E> updater = new BatchUpdateOperation<>(
                context, elements, count, this, generatedKeyReader, batchInStatement);

            QueryElement<int[]> query = new QueryElement<>(QueryType.INSERT, model, updater);
            query.from(entityClass);
            for (Attribute attribute : bindableAttributes) {
                query.value((Expression)attribute, null);
            }
            int[] updates = query.get();
            for (int i = 0; i < updates.length; i++) {
                E entity = elements[i];
                EntityProxy<E> proxy = proxyProvider.apply(entity);
                checkRowsAffected(updates[i], entity, proxy);
                proxy.link(reader);
                updateAssociations(entity, proxy);
                context.stateListener().postInsert(entity, proxy);
                // cache entity
                if (cacheable) {
                    cache.put(entityClass, proxy.key(), entity);
                }
            }
        }
    }

    private void readGeneratedKeys(EntityProxy<E> proxy, ResultSet results) throws SQLException {
        // optimal case (1 key)
        if (keyAttribute != null) {
            readKeyFromResult(keyAttribute, proxy, results);
        } else {
            for (Attribute<E, ?> key : type.keyAttributes()) {
                readKeyFromResult(key, proxy, results);
            }
        }
    }

    @SuppressWarnings("unchecked") // checked by primitiveKind()
    private void readKeyFromResult(Attribute<E, ?> key, EntityProxy<E> proxy, ResultSet results)
        throws SQLException {

        Object generatedKey;
        String column = key.name();
        int resultIndex = 1;
        try {
            // try find column if driver supports it
            resultIndex = results.findColumn(column);
        } catch (SQLException ignored) {
        }
        if (key.primitiveKind() != null) {
            switch (key.primitiveKind()) {
                case INT:
                    int intValue = mapping.readInt(results, resultIndex);
                    proxy.setInt((Attribute<E, Integer>) key, intValue, PropertyState.LOADED);
                    break;
                case LONG:
                    long longValue = mapping.readLong(results, resultIndex);
                    proxy.setLong((Attribute<E, Long>) key, longValue, PropertyState.LOADED);
                    break;
            }
        } else {
            generatedKey = mapping.read((Expression) key, results, resultIndex);
            if (generatedKey == null) {
                throw new MissingKeyException(proxy);
            }
            proxy.setObject(key, generatedKey, PropertyState.LOADED);
        }
    }

    public void bindParameters(E element, PreparedStatement statement) throws SQLException {
        int i = 0;
        for (Attribute<E, ?> attribute : bindableAttributes) {
            EntityProxy<E> proxy = type.proxyProvider().apply(element);
            if (attribute.isAssociation()) {
                // get the referenced value
                Object value = proxy.get(attribute);
                if (value != null) {
                    Attribute<Object, Object> referenced =
                        Attributes.get(attribute.referencedAttribute());
                    Function<Object, EntityProxy<Object>> proxyProvider =
                        referenced.declaringType().proxyProvider();
                    value = proxyProvider.apply(value).get(referenced);
                }
                mapping.write((Expression) attribute, statement, i + 1, value);
            } else {
                if (attribute.primitiveKind() != null) {
                    mapPrimitiveType(proxy, attribute, statement, i + 1);
                } else {
                    Object value = proxy.get(attribute);
                    mapping.write((Expression) attribute, statement, i + 1, value);
                }
            }
            // optimistically setting to loaded
            proxy.setState(attribute, PropertyState.LOADED);
            i++;
        }
    }

    @SuppressWarnings("unchecked") // checked by primitiveKind()
    private void mapPrimitiveType(EntityProxy<E> proxy,
                                  Attribute<E, ?> attribute,
                                  PreparedStatement statement, int index) throws SQLException {
        switch (attribute.primitiveKind()) {
            case INT:
                int intValue = proxy.getInt((Attribute<E, Integer>) attribute);
                mapping.writeInt(statement, index, intValue);
                break;
            case LONG:
                long longValue = proxy.getLong((Attribute<E, Long>) attribute);
                mapping.writeLong(statement, index, longValue);
                break;
            case SHORT:
                short shortValue = proxy.getShort((Attribute<E, Short>) attribute);
                mapping.writeShort(statement, index, shortValue);
                break;
            case BOOLEAN:
                boolean booleanValue = proxy.getBoolean((Attribute<E, Boolean>) attribute);
                mapping.writeBoolean(statement, index, booleanValue);
                break;
            case FLOAT:
                float floatValue = proxy.getFloat((Attribute<E, Float>) attribute);
                mapping.writeFloat(statement, index, floatValue);
                break;
            case DOUBLE:
                double doubleValue = proxy.getDouble((Attribute<E, Double>) attribute);
                mapping.writeDouble(statement, index, doubleValue);
                break;
        }
    }

    public void insert(E entity, final EntityProxy<E> proxy) {
        GeneratedResultReader keyReader = null;
        if (hasGeneratedKey) {
            keyReader = new GeneratedResultReader() {
                @Override
                public void read(int index, ResultSet results) throws SQLException {
                    if (results.next()) {
                        readGeneratedKeys(proxy, results);
                    }
                }
                @Override
                public String[] generatedColumns() {
                    return generatedColumnNames;
                }
            };
        }
        InsertOperation<E> insert = new InsertOperation<>(context, entity, this, keyReader);
        QueryElement<Scalar<Integer>> query = new QueryElement<>(QueryType.INSERT, model, insert);
        query.from(entityClass);

        for (Attribute<E, ?> attribute : type.attributes()) {
            // persist the foreign key object if needed
            S referenced = foreignKeyReference(proxy, attribute);
            if (referenced != null) {
                cascadeInsert(referenced);
                proxy.setState(attribute, PropertyState.LOADED);
            }
        }
        if (versionAttribute != null && !hasSystemVersionColumn()) {
            incrementVersion(proxy);
        }
        for (Attribute attribute : bindableAttributes) {
            query.value((Expression)attribute, null);
        }
        context.stateListener().preInsert(entity, proxy);

        checkRowsAffected(query.get().value(), entity, null);
        proxy.link(context.read(entityClass));
        updateAssociations(entity, proxy);

        context.stateListener().postInsert(entity, proxy);

        // cache entity
        if (cacheable) {
            cache.put(entityClass, proxy.key(), entity);
        }
    }

    public void update(E entity, final EntityProxy<E> proxy) {
        context.stateListener().preUpdate(entity, proxy);
        FilteringIterator<Attribute<E, ?>> filterator = new FilteringIterator<>(
            type.attributes().iterator(),
            new Predicate<Attribute<E, ?>>() {
            @Override
            public boolean test(Attribute<E, ?> value) {
                return stateless || (value.isVersion() && !hasSystemVersionColumn()) ||
                    ((proxy.getState(value) == PropertyState.MODIFIED) &&
                        (!value.isAssociation() || value.isForeignKey()));
            }
        });
        if (filterator.hasNext()) {
            // handle versioning property
            Object version = null;
            if (versionAttribute != null) {
                version = proxy.get(versionAttribute, true);
                if (version == null) {
                    throw new MissingVersionException(proxy);
                }
                if (!hasSystemVersionColumn()) {
                    incrementVersion(proxy);
                }
            }

            Update<Scalar<Integer>> update = queryable.update(entityClass);
            while (filterator.hasNext()) {
                Attribute<E, ?> attribute = filterator.next();
                // persist the foreign key object if needed
                S referenced = foreignKeyReference(proxy, attribute);
                if (referenced != null) {
                    cascadeInsert(referenced);
                }
                Expression<Object> expression = Attributes.query(attribute);
                update.set(expression, proxy.get(attribute));
                proxy.setState(attribute, PropertyState.LOADED);
            }
            for (Attribute<E, ?> attribute : type.keyAttributes()) {
                QueryAttribute<E, Object> id = Attributes.query(attribute);
                update.where(id.equal(proxy.get(attribute)));
            }
            addVersionCondition(update, version);
            checkRowsAffected(update.get().value(), entity, proxy);
        }
        updateAssociations(entity, proxy);
        context.stateListener().postUpdate(entity, proxy);
    }

    private void addVersionCondition(Where<?> where, Object version) {
        if (versionAttribute != null) {
            QueryAttribute<E, Object> queryAttribute = Attributes.query(versionAttribute);
            VersionColumnDefinition versionColumnDefinition =
                context.platform().versionColumnDefinition();
            String columnName = versionColumnDefinition.columnName();
            if (!versionColumnDefinition.createColumn() && columnName != null) {
                FieldExpression<Object> expression = (FieldExpression<Object>)
                        queryAttribute.as(columnName);
                where.where(expression.equal(version));
            } else {
                where.where(queryAttribute.equal(version));
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void updateAssociations(E entity, EntityProxy<E> proxy) {
        for (Attribute<E, ?> attribute : associativeAttributes) {
            boolean isModified = stateless || proxy.getState(attribute) == PropertyState.MODIFIED;
            if (!isModified) {
                continue;
            }
            Cardinality cardinality = attribute.cardinality();
            switch (cardinality) {
                case ONE_TO_ONE:
                    S value = (S) proxy.get(attribute, false);
                    if (value == null) {
                        throw new PersistenceException(
                                "1-1 relationship can only be removed from the owning side");
                    }
                    Attribute<S, Object> mapped = Attributes.get(attribute.mappedAttribute());
                    EntityProxy<S> refProxy = context.proxyOf(value, true);
                    refProxy.set(mapped, entity, PropertyState.MODIFIED);
                    context.write(mapped.declaringType().classType()).update(value, refProxy);
                    break;
                case ONE_TO_MANY:
                    ObservableCollection<S> collection =
                        (ObservableCollection<S>) proxy.get(attribute, false);
                    CollectionChanges<?, S> changes =
                        (CollectionChanges<?, S>) collection.observer();
                    for (S added : changes.addedElements()) {
                        updateInverseAssociation(added, attribute, entity);
                    }
                    for (S removed : changes.removedElements()) {
                        updateInverseAssociation(removed, attribute, null);
                    }
                    changes.clear();
                    break;
                case MANY_TO_MANY:
                    Class referencedClass = attribute.referencedClass();
                    Type<?> referencedType = model.typeOf(referencedClass);
                    QueryAttribute<S, Object> tKey = null;
                    QueryAttribute<S, Object> uKey = null;
                    for (Attribute a : referencedType.attributes()) {
                        if (entityClass.isAssignableFrom(a.referencedClass())) {
                            tKey = Attributes.query(a);
                        } else if (attribute.elementClass().isAssignableFrom(a.referencedClass())) {
                            uKey = Attributes.query(a);
                        }
                    }
                    Objects.requireNotNull(tKey);
                    Objects.requireNotNull(uKey);
                    Attribute<E, Object> tRef = Attributes.get(tKey.referencedAttribute());
                    Attribute<S, Object> uRef = Attributes.get(uKey.referencedAttribute());

                    collection = (ObservableCollection<S>) proxy.get(attribute, false);
                    changes = (CollectionChanges<?, S>) collection.observer();
                    for (S added : changes.addedElements()) {
                        S junction = (S) referencedType.factory().get();
                        EntityProxy<S> junctionProxy = context.proxyOf(junction, false);
                        EntityProxy<S> uProxy = context.proxyOf(added, false);

                        if (attribute.cascadeActions().contains(CascadeAction.SAVE)) {
                            cascadeSave(added, uProxy);
                        }
                        Object tValue = proxy.get(tRef);
                        Object uValue = uProxy.get(uRef);

                        junctionProxy.set(tKey, tValue, PropertyState.MODIFIED);
                        junctionProxy.set(uKey, uValue, PropertyState.MODIFIED);

                        cascadeInsert(junction);
                    }

                    Object keyValue = proxy.get(tRef);
                    for (S removed : changes.removedElements()) {
                        EntityProxy<S> toRemove = context.proxyOf(removed, false);
                        Object otherValue = toRemove.get(uRef);
                        Class<? extends S> removeType = (Class<? extends S>)
                            referencedType.classType();

                        Supplier<Scalar<Integer>> query = queryable.delete(removeType)
                                .where(tKey.equal(keyValue))
                                .and(uKey.equal(otherValue));
                        int count = query.get().value();
                        if (count != 1) {
                            throw new RowCountException(1, count);
                        }
                        //if (attribute.cascadeActions().contains(CascadeAction.DELETE)) {
                            //cascadeDelete(removed);
                        //}
                    }
                    changes.clear();
                    break;
                case MANY_TO_ONE:
                default:
                    throw new IllegalStateException();
            }
            context.read(type.classType()).refresh(entity, proxy, attribute);
        }
    }

    private void incrementVersion(EntityProxy<E> proxy) {
        Object version = proxy.get(versionAttribute);
        Class<?> type = versionAttribute.classType();
        if (type == Long.class || type == long.class) {
            if (version == null) {
                version = 1L;
            } else {
                Long value = (Long) version;
                version = value + 1;
            }
        } else if (type == Integer.class || type == int.class) {
            if (version == null) {
                version = 1;
            } else {
                Integer value = (Integer) version;
                version = value + 1;
            }
        } else if (type == Timestamp.class) {
            version = new Timestamp(System.currentTimeMillis());
        } else {
            throw new PersistenceException("Unsupported version: " + versionAttribute.classType());
        }
        proxy.setObject(versionAttribute, version, PropertyState.MODIFIED);
    }

    private <U extends S> void updateInverseAssociation(U entity, Attribute p, Object value) {
        EntityProxy<U> proxy = context.proxyOf(entity, false);
        Attribute<U, Object> inverse = Attributes.get(p.mappedAttribute());
        proxy.set(inverse, value, PropertyState.MODIFIED);
        cascadeSave(entity, proxy);
    }

    public void delete(E entity, EntityProxy<E> proxy) {
        context.stateListener().preDelete(entity, proxy);
        if (cacheable) {
            cache.invalidate(entityClass, proxy.key());
        }
        clearAssociations(entity, proxy);

        Deletion<Scalar<Integer>> deletion = queryable.delete(entityClass);
        for (Attribute<E, ?> attribute : type.keyAttributes()) {
            QueryAttribute<E, Object> id = Attributes.query(attribute);
            deletion.where(id.equal(proxy.get(attribute)));
        }
        if (versionAttribute != null) {
            Object version = proxy.get(versionAttribute, true);
            if (version == null) {
                throw new MissingVersionException(proxy);
            }
            addVersionCondition(deletion, version);
        }
        checkRowsAffected(deletion.get().value(), entity, proxy);
        proxy.unlink();
        context.stateListener().postDelete(entity, proxy);
    }

    private void clearAssociations(E entity, EntityProxy<E> proxy) {
        for (Attribute<E, ?> attribute : type.attributes()) {
            if (!attribute.isAssociation()) {
                continue;
            }
            Object value = proxy.get(attribute, false);
            boolean delete = attribute.cascadeActions().contains(CascadeAction.DELETE);

            // if cascade delete and the property is not loaded (load it)
            if (delete && (stateless || proxy.getState(attribute) == PropertyState.FETCH)) {
                context.read(type.classType()).refresh(entity, proxy, attribute);
            }
            if (value != null) {
                switch (attribute.cardinality()) {
                    case ONE_TO_ONE:
                    case MANY_TO_ONE:
                        @SuppressWarnings("unchecked")
                        S element = (S) value;
                        cascadeRemove(entity, element, delete);
                        break;
                    case ONE_TO_MANY:
                    case MANY_TO_MANY:
                        if (value instanceof Iterable) {
                            Iterable iterable = (Iterable) value;
                            List<S> elements = new ArrayList<>();
                            for (Object item : iterable) {
                                @SuppressWarnings("unchecked")
                                S e = (S) item;
                                elements.add(e);
                            }
                            // avoid modifying the results while iterating
                            for (S item : elements) {
                                cascadeRemove(entity, item, delete);
                            }
                        }
                        break;
                }
            }
            proxy.set(attribute, null, PropertyState.LOADED);
        }
    }

    void batchDelete(Iterable<E> entities) {

        final int batchSize = context.batchUpdateSize();
        final Iterator<E> iterator = entities.iterator();

        while (iterator.hasNext()) {
            final List<Object> ids = new LinkedList<>();
            while (iterator.hasNext() && ids.size() < batchSize) {
                E entity = iterator.next();
                EntityProxy<E> proxy = proxyProvider.apply(entity);
                if (versionAttribute != null || type.keyAttributes().size() > 1) {
                    // not optimized if version column has to be checked, or multiple primary keys
                    // TODO could use JDBC batching
                    delete(entity, proxy);
                } else {
                    context.stateListener().preDelete(entity, proxy);
                    clearAssociations(entity, proxy);

                    Object key = proxy.key();
                    if (cacheable) {
                        cache.invalidate(entityClass, key);
                    }
                    ids.add(key);
                    proxy.unlink();

                    context.stateListener().postDelete(entity, proxy);
                }
            }
            // optimized case: delete from T where key in (keys...)
            if (ids.size() > 0) {
                Deletion<Scalar<Integer>> deletion = queryable.delete(entityClass);
                for (Attribute<E, ?> attribute : type.keyAttributes()) {
                    QueryAttribute<E, Object> id = Attributes.query(attribute);
                    deletion.where(id.in(ids));
                }
                int rowsAffected = deletion.get().value();
                if (rowsAffected != ids.size()) {
                    throw new RowCountException(ids.size(), rowsAffected);
                }
            }
        }
    }

    private <U extends S> void cascadeInsert(U entity) {
        EntityProxy<U> proxy = context.proxyOf(entity, false);
        if (proxy != null && !proxy.isLinked()) {
            EntityWriter<U, S> writer = context.write(proxy.type().classType());
            writer.insert(entity, proxy);
        }
    }

    private <U extends S> void cascadeSave(U entity, EntityProxy<U> proxy) {
        if (entity != null) {
            if (proxy == null) {
                proxy = context.proxyOf(entity, false);
            }
            EntityWriter<U, S> writer = context.write(proxy.type().classType());
            if (proxy.isLinked()) {
                writer.update(entity, proxy);
            } else {
                writer.insert(entity, proxy);
            }
        }
    }

    private <U extends S> void cascadeRemove(E entity, U element, boolean delete) {
        EntityProxy<U> proxy = context.proxyOf(element, false);
        if (proxy != null) {
            EntityWriter<U, S> writer = context.write(proxy.type().classType());
            if (delete) {
                writer.delete(element, proxy);
            } else {
                writer.removeEntity(proxy, entity);
            }
        }
    }

    private void removeEntity(EntityProxy<E> proxy, Object entity) {
        for (Attribute<E, ?> attribute : type.attributes()) {
            if (attribute.isAssociation()) {
                Object value = proxy.get(attribute, false);
                switch (attribute.cardinality()) {
                    case ONE_TO_ONE:
                    case MANY_TO_ONE:
                        if (value == entity) {
                            proxy.set(attribute, null, PropertyState.LOADED);
                        }
                        break;
                    case ONE_TO_MANY:
                    case MANY_TO_MANY:
                        if (value instanceof Collection) {
                            Collection collection = (Collection) value;
                            collection.remove(entity);
                        }
                        break;
                }
            }
        }
    }
}
