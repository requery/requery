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
import io.requery.ReferentialAction;
import io.requery.meta.Attribute;
import io.requery.meta.EntityModel;
import io.requery.meta.QueryAttribute;
import io.requery.meta.Type;
import io.requery.proxy.CollectionChanges;
import io.requery.proxy.EntityProxy;
import io.requery.proxy.PropertyState;
import io.requery.proxy.Settable;
import io.requery.query.Deletion;
import io.requery.query.Expression;
import io.requery.query.FieldExpression;
import io.requery.query.MutableResult;
import io.requery.query.Scalar;
import io.requery.query.Where;
import io.requery.query.element.QueryElement;
import io.requery.query.element.QueryType;
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
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static io.requery.query.element.QueryType.UPDATE;

/**
 * Handles insert/update/delete operations for {@link io.requery.Entity} instances.
 *
 * @param <E> specific entity type
 * @param <S> generic type from which all entities extend
 *
 * @author Nikhil Purushe
 */
class EntityWriter<E extends S, S> implements ParameterBinder<E> {

    private enum Cascade { AUTO, INSERT, UPDATE, UPSERT }

    private final EntityCache cache;
    private final EntityModel model;
    private final Type<E> type;
    private final EntityContext<S> context;
    private final Mapping mapping;
    private final Queryable<S> queryable;
    private final boolean hasGeneratedKey;
    private final boolean hasForeignKeys;
    private final int keyCount;
    private final Attribute<E, ?> keyAttribute;
    private final Attribute<E, ?> versionAttribute;
    private final Attribute<E, ?>[] bindableAttributes;
    private final Attribute<E, ?>[] whereAttributes;
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
        this.cache = this.context.getCache();
        this.model = this.context.getModel();
        this.mapping = this.context.getMapping();
        // check type attributes
        boolean hasGeneratedKey = false;
        boolean hasForeignKeys = false;
        Attribute<E, ?> versionAttribute = null;
        for (Attribute<E, ?> attribute : type.getAttributes()) {
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
        this.keyAttribute = type.getSingleKeyAttribute();
        this.keyCount = type.getKeyAttributes().size();
        Collection<Attribute<E, ?>> keys = type.getKeyAttributes();
        generatedColumnNames = new String[keys.size()];
        int i = 0;
        for (Attribute attribute : keys) {
            generatedColumnNames[i++] = attribute.getName();
        }
        entityClass = type.getClassType();
        proxyProvider = type.getProxyProvider();
        cacheable = !type.getKeyAttributes().isEmpty() && type.isCacheable();
        stateless = type.isStateless();

        Predicate<Attribute<E, ?>> bindable = new Predicate<Attribute<E, ?>>() {
            @Override
            public boolean test(Attribute<E, ?> value) {
                boolean isGeneratedKey = value.isGenerated() && value.isKey();
                boolean isSystemVersion = value.isVersion() && hasSystemVersionColumn();
                boolean isAssociation = value.isAssociation() &&
                    !(value.isForeignKey() || value.isKey());
                return !(isGeneratedKey || isSystemVersion) && !isAssociation;
            }
        };
        // create bindable attributes as an array for performance
        bindableAttributes = Attributes.toArray(type.getAttributes(), bindable);
        associativeAttributes = Attributes.toArray(type.getAttributes(),
            new Predicate<Attribute<E, ?>>() {
            @Override
            public boolean test(Attribute<E, ?> value) {
                return value.isAssociation();
            }
        });

        // for the update/delete statement add key/version conditions
        if (keyCount == 0) {
            whereAttributes = Attributes.newArray(type.getAttributes().size());
            type.getAttributes().toArray(whereAttributes);
        } else {
            boolean hasVersion = versionAttribute != null;
            whereAttributes = Attributes.newArray(keyCount + (hasVersion ? 1 : 0));
            int index = 0;
            for (Attribute<E, ?> attribute : keys) {
                whereAttributes[index++] = attribute;
            }
            if (hasVersion) {
                whereAttributes[index] = versionAttribute;
            }
        }
    }

    private void checkRowsAffected(int count, E entity, EntityProxy<E> proxy) {
        if (proxy != null && versionAttribute != null && count == 0) {
            throw new OptimisticLockException(entity, proxy.get(versionAttribute));
        } else if (count != 1) {
            throw new RowCountException(1, count);
        }
    }

    private boolean hasSystemVersionColumn() {
        return !context.getPlatform().versionColumnDefinition().createColumn();
    }

    private boolean canBatchInStatement() {
        boolean canBatchStatement = context.supportsBatchUpdates();
        boolean canBatchGeneratedKey = context.getPlatform().supportsGeneratedKeysInBatchUpdate();
        return hasGeneratedKey ? canBatchStatement && canBatchGeneratedKey : canBatchStatement;
    }

    private void cascadeBatch(Map<Class<? extends S>, List<S>> map) {
        for (Map.Entry<Class<? extends S>, List<S>> entry : map.entrySet()) {
            Class<? extends S> key = entry.getKey();
            context.write(key).batchInsert(entry.getValue(), false);
        }
    }

    @SuppressWarnings("unchecked")
    private S foreignKeyReference(EntityProxy<E> proxy, Attribute<E, ?> attribute) {
        if (attribute.isForeignKey() && attribute.isAssociation()) {
            return (S) proxy.get(attribute);
        }
        return null;
    }

    GeneratedKeys<E> batchInsert(Iterable<E> entities, boolean returnKeys) {
        // true if using JDBC batching
        final boolean batchInStatement = canBatchInStatement();
        final int batchSize = context.getBatchUpdateSize();
        final EntityReader<E, S> reader = context.read(entityClass);
        final Iterator<E> iterator = entities.iterator();
        final boolean isImmtuable = type.isImmutable();
        final GeneratedKeys<E> keys = returnKeys && hasGeneratedKey? new GeneratedKeys<E>() : null;

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
                    for (Attribute<E, ?> attribute : associativeAttributes) {
                        S referenced = foreignKeyReference(proxy, attribute);
                        if (referenced != null) {
                            EntityProxy<S> otherProxy = context.proxyOf(referenced, false);
                            if (otherProxy != null && !otherProxy.isLinked()) {
                                Class<? extends S> key = otherProxy.type().getClassType();
                                List<S> values = associations.get(key);
                                if (values == null) {
                                    associations.put(key, values = new ArrayList<>());
                                }
                                values.add(referenced);
                            }
                        }
                    }
                }
                incrementVersion(proxy);
                context.getStateListener().preInsert(entity, proxy);
                index++;
            }
            cascadeBatch(associations);

            final int count = index;
            GeneratedResultReader keyReader = null;
            if (hasGeneratedKey) {
                keyReader = new GeneratedResultReader() {
                    @Override
                    public void read(int index, ResultSet results) throws SQLException {
                        // check if reading batch keys, otherwise read 1
                        int readCount = batchInStatement? count : 1;
                        for (int i = index; i < index + readCount ; i++) {
                            if (!results.next()) {
                                throw new IllegalStateException();
                            }
                            EntityProxy<E> proxy = proxyProvider.apply(elements[i]);
                            Settable<E> keyProxy = keys == null ?
                                proxy : keys.proxy(isImmtuable ? null : proxy);
                            readGeneratedKeys(keyProxy, results);
                        }
                    }
                    @Override
                    public String[] generatedColumns() {
                        return generatedColumnNames;
                    }
                };
            }
            BatchUpdateOperation<E> operation = new BatchUpdateOperation<>(
                context, elements, count, this, keyReader, batchInStatement);

            QueryElement<int[]> query = new QueryElement<>(QueryType.INSERT, model, operation);
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
                updateAssociations(Cascade.AUTO, entity, proxy, null);
                context.getStateListener().postInsert(entity, proxy);
                // cache entity
                if (cacheable) {
                    cache.put(entityClass, proxy.key(), entity);
                }
            }
        }
        return keys;
    }

    private void readGeneratedKeys(Settable<E> proxy, ResultSet results) throws SQLException {
        // optimal case (1 key)
        if (keyAttribute != null) {
            readKeyFromResult(keyAttribute, proxy, results);
        } else {
            for (Attribute<E, ?> key : type.getKeyAttributes()) {
                readKeyFromResult(key, proxy, results);
            }
        }
    }

    @SuppressWarnings("unchecked") // checked by primitiveKind()
    private void readKeyFromResult(Attribute<E, ?> key, Settable<E> proxy, ResultSet results)
        throws SQLException {

        Object generatedKey;
        String column = key.getName();
        int resultIndex = 1;
        try {
            // try find column if driver supports it
            resultIndex = results.findColumn(column);
        } catch (SQLException ignored) {
        }
        if (key.getPrimitiveKind() != null) {
            switch (key.getPrimitiveKind()) {
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
                throw new MissingKeyException();
            }
            proxy.setObject(key, generatedKey, PropertyState.LOADED);
        }
    }

    @Override
    public int bindParameters(PreparedStatement statement, E element,
                              Predicate<Attribute<E, ?>> filter) throws SQLException {
        int i = 0;
        EntityProxy<E> proxy = type.getProxyProvider().apply(element);
        for (Attribute<E, ?> attribute : bindableAttributes) {
            if (filter != null && !filter.test(attribute)) {
                continue;
            }
            if (attribute.isAssociation()) {
                // get the referenced value
                Object value = proxy.get(attribute, false);
                if (value != null) {
                    Attribute<Object, Object> referenced =
                        Attributes.get(attribute.getReferencedAttribute());
                    Function<Object, EntityProxy<Object>> proxyProvider =
                        referenced.getDeclaringType().getProxyProvider();
                    value = proxyProvider.apply(value).get(referenced, false);
                }
                mapping.write((Expression) attribute, statement, i + 1, value);
            } else {
                if (attribute.getPrimitiveKind() != null) {
                    mapPrimitiveType(proxy, attribute, statement, i + 1);
                } else {
                    Object value = proxy.get(attribute, false);
                    mapping.write((Expression) attribute, statement, i + 1, value);
                }
            }
            // optimistically setting to loaded
            proxy.setState(attribute, PropertyState.LOADED);
            i++;
        }
        return i;
    }

    @SuppressWarnings("unchecked") // checked by primitiveKind()
    private void mapPrimitiveType(EntityProxy<E> proxy, Attribute<E, ?> attribute,
                                  PreparedStatement statement, int index) throws SQLException {
        switch (attribute.getPrimitiveKind()) {
            case BYTE:
                byte byteValue = proxy.getByte((Attribute<E, Byte>) attribute);
                mapping.writeByte(statement, index, byteValue);
                break;
            case SHORT:
                short shortValue = proxy.getShort((Attribute<E, Short>) attribute);
                mapping.writeShort(statement, index, shortValue);
                break;
            case INT:
                int intValue = proxy.getInt((Attribute<E, Integer>) attribute);
                mapping.writeInt(statement, index, intValue);
                break;
            case LONG:
                long longValue = proxy.getLong((Attribute<E, Long>) attribute);
                mapping.writeLong(statement, index, longValue);
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

    void insert(E entity, final EntityProxy<E> proxy, GeneratedKeys<E> keys) {
        insert(entity, proxy, Cascade.AUTO, keys);
    }

    void insert(final E entity, EntityProxy<E> proxy, Cascade mode, GeneratedKeys<E> keys) {
        // if the type is immutable return the key(s) to the caller instead of modifying the object
        GeneratedResultReader keyReader = null;
        if (hasGeneratedKey) {
            final Settable<E> settable = keys == null ? proxy : keys;
            keyReader = new GeneratedResultReader() {
                @Override
                public void read(int index, ResultSet results) throws SQLException {
                    if (results.next()) {
                        readGeneratedKeys(settable, results);
                    }
                }
                @Override
                public String[] generatedColumns() {
                    return generatedColumnNames;
                }
            };
        }
        EntityUpdateOperation insert = new EntityUpdateOperation(context, keyReader) {
            @Override
            public int bindParameters(PreparedStatement statement) throws SQLException {
                return EntityWriter.this.bindParameters(statement, entity, null);
            }
        };
        QueryElement<Scalar<Integer>> query = new QueryElement<>(QueryType.INSERT, model, insert);
        query.from(entityClass);

        for (Attribute<E, ?> attribute : associativeAttributes) {
            // persist the foreign key object if needed
            cascadeKeyReference(Cascade.INSERT, proxy, attribute);
        }
        incrementVersion(proxy);
        for (Attribute attribute : bindableAttributes) {
            query.value((Expression)attribute, null);
        }
        context.getStateListener().preInsert(entity, proxy);

        checkRowsAffected(query.get().value(), entity, null);
        proxy.link(context.read(entityClass));
        updateAssociations(mode, entity, proxy, null);

        context.getStateListener().postInsert(entity, proxy);

        // cache entity
        if (cacheable) {
            cache.put(entityClass, proxy.key(), entity);
        }
    }

    public void upsert(E entity, final EntityProxy<E> proxy) {
        if (hasGeneratedKey) {
            if (hasKey(proxy)) {
                update(entity, proxy, Cascade.UPSERT, null, null);
            } else {
                insert(entity, proxy, Cascade.UPSERT, null);
            }
        } else {
            if (context.getPlatform().supportsUpsert()) {
                for (Attribute<E, ?> attribute : associativeAttributes) {
                    cascadeKeyReference(Cascade.UPSERT, proxy, attribute);
                }
                incrementVersion(proxy);
                List<Attribute<E, ?>> attributes = Arrays.asList(bindableAttributes);
                UpdateOperation upsert = new UpdateOperation(context);
                QueryElement<Scalar<Integer>> element =
                        new QueryElement<>(QueryType.UPSERT, model, upsert);
                for (Attribute<E, ?> attribute : attributes) {
                    element.value((Expression) attribute, proxy.get(attribute, false));
                }
                int rows = upsert.evaluate(element).value();
                if (rows <= 0) {
                    throw new RowCountException(1, rows);
                }
                proxy.link(context.read(entityClass));
                updateAssociations(Cascade.UPSERT, entity, proxy, null);
                if (cacheable) {
                    cache.put(entityClass, proxy.key(), entity);
                }
            } else {
                // not a real upsert, but can be ok for embedded databases
                if (update(entity, proxy, Cascade.UPSERT, null, null) == 0) {
                    insert(entity, proxy, Cascade.UPSERT, null);
                }
            }
        }
    }

    public void update(E entity, EntityProxy<E> proxy, final Attribute<E, ?>[] attributes) {
        final List<Attribute<E, ?>> list = Arrays.asList(attributes);
        update(entity, proxy, Cascade.AUTO,
            new Predicate<Attribute<E, ?>>() {
                @Override
                public boolean test(Attribute<E, ?> value) {
                    return list.contains(value) && !value.isAssociation();
                }
            }, new Predicate<Attribute<E, ?>>() {
                @Override
                public boolean test(Attribute<E, ?> value) {
                    return list.contains(value) && value.isAssociation();
                }
            });
    }

    public void update(E entity, EntityProxy<E> proxy) {
        int count = update(entity, proxy, Cascade.AUTO, null, null);
        if (count != -1) {
            checkRowsAffected(count, entity, proxy);
        }
    }

    private int update(final E entity, final EntityProxy<E> proxy, Cascade mode,
                       Predicate<Attribute<E, ?>> filterBindable,
                       Predicate<Attribute<E, ?>> filterAssociations) {

        context.getStateListener().preUpdate(entity, proxy);
        // updates the entity using a query (not the query values are not specified but instead
        // mapped directly to avoid boxing)
        if (filterBindable == null) {
            final List<Attribute<E, ?>> list = new ArrayList<>();
            for (Attribute<E, ?> value : bindableAttributes) {
                if (stateless ||
                    ((proxy.getState(value) == PropertyState.MODIFIED) &&
                    (!value.isAssociation() || value.isForeignKey() || value.isKey()))) {
                    list.add(value);
                }
            }
            filterBindable = new Predicate<Attribute<E, ?>>() {
                @Override
                public boolean test(Attribute<E, ?> value) {
                    return list.contains(value);
                }
            };
        }
        boolean hasVersion = versionAttribute != null;
        final Object version = hasVersion ? incrementVersion(proxy, filterBindable) : null;
        final Predicate<Attribute<E, ?>> filter = filterBindable;

        EntityUpdateOperation operation = new EntityUpdateOperation(context, null) {
            @Override
            public int bindParameters(PreparedStatement statement) throws SQLException {
                // first write the changed properties
                int index = EntityWriter.this.bindParameters(statement, entity, filter);
                // write the where arguments
                for (Attribute<E, ?> attribute : whereAttributes) {
                    if (attribute == versionAttribute) {
                        mapping.write((Expression) attribute, statement, index + 1, version);
                    } else {
                        if (attribute.getPrimitiveKind() != null) {
                            mapPrimitiveType(proxy, attribute, statement, index + 1);
                        } else {
                            Object value = proxy.get(attribute, false);
                            mapping.write((Expression) attribute, statement, index + 1, value);
                        }
                    }
                    index++;
                }
                return index;
            }
        };
        QueryElement<Scalar<Integer>> query = new QueryElement<>(UPDATE, model, operation);
        query.from(entityClass);
        int count = 0;
        for (Attribute<E, ?> attribute : bindableAttributes) {
            if (!filterBindable.test(attribute)) {
                continue;
            }
            // persist the foreign key object if needed
            S referenced = foreignKeyReference(proxy, attribute);
            if (referenced != null && !stateless) {
                proxy.setState(attribute, PropertyState.LOADED);
                cascadeWrite(mode, referenced, null);
                // reset the state temporarily for the updateable filter
                proxy.setState(attribute, PropertyState.MODIFIED);
            }
            query.set((Expression)attribute, null);
            count++;
        }
        int result = -1;
        if (count > 0) {
            if (keyAttribute != null) {
                query.where(Attributes.query(keyAttribute).equal("?"));
            } else {
                for (Attribute<E, ?> attribute : type.getKeyAttributes()) {
                    query.where(Attributes.query(attribute).equal("?"));
                }
            }
            if (hasVersion) {
                addVersionCondition(query, version);
            }
            result = query.get().value();
            proxy.link(context.read(entityClass));
            if (result > 0) {
                updateAssociations(mode, entity, proxy, filterAssociations);
            }
        } else {
            updateAssociations(mode, entity, proxy, filterAssociations);
        }
        context.getStateListener().postUpdate(entity, proxy);
        return result;
    }

    private void addVersionCondition(Where<?> where, Object version) {
        QueryAttribute<E, Object> attribute = Attributes.query(versionAttribute);
        VersionColumnDefinition definition = context.getPlatform().versionColumnDefinition();
        String name = definition.columnName();
        if (!definition.createColumn() && name != null) {
            FieldExpression<Object> expression = (FieldExpression<Object>) attribute.as(name);
            where.where(expression.equal(version));
        } else {
            where.where(attribute.equal(version));
        }
    }

    private void updateAssociations(Cascade mode, E entity, EntityProxy<E> proxy,
                                    Predicate<Attribute<E, ?>> filter) {
        for (Attribute<E, ?> attribute : associativeAttributes) {
            if ((filter != null && filter.test(attribute)) ||
                    (stateless || proxy.getState(attribute) == PropertyState.MODIFIED)) {
                updateAssociation(mode, entity, proxy, attribute);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void updateAssociation(Cascade mode, E entity, EntityProxy<E> proxy,
                                   Attribute<E, ?> attribute) {
        switch (attribute.getCardinality()) {
            case ONE_TO_ONE:
                S value = (S) proxy.get(attribute, false);
                if (value != null) {
                    Attribute<S, Object> mapped = Attributes.get(attribute.getMappedAttribute());
                    EntityProxy<S> referred = context.proxyOf(value, true);
                    referred.set(mapped, entity, PropertyState.MODIFIED);
                    cascadeWrite(mode, value, referred);
                } else if (!stateless) {
                    throw new PersistenceException(
                            "1-1 relationship can only be removed from the owning side");
                }
                break;
            case ONE_TO_MANY:
                Object relation = proxy.get(attribute, false);
                if (relation instanceof ObservableCollection) {
                    ObservableCollection<S> collection = (ObservableCollection<S>) relation;
                    CollectionChanges<?, S> changes =
                            (CollectionChanges<?, S>) collection.observer();
                    List<S> added = new ArrayList<>(changes.addedElements());
                    List<S> removed = new ArrayList<>(changes.removedElements());
                    changes.clear();
                    for (S element : added) {
                        updateMappedAssociation(mode, element, attribute, entity);
                    }
                    for (S element : removed) {
                        updateMappedAssociation(Cascade.UPDATE, element, attribute, null);
                    }
                } else if (relation instanceof Iterable) {
                    Iterable<S> iterable = (Iterable<S>) relation;
                    for (S added : iterable) {
                        updateMappedAssociation(mode, added, attribute, entity);
                    }
                } else {
                    throw new IllegalStateException("unsupported relation type " + relation);
                }
                break;
            case MANY_TO_MANY:
                Class referencedClass = attribute.getReferencedClass();
                if (referencedClass == null) {
                    throw new IllegalStateException("Invalid referenced class in " + attribute);
                }
                Type<?> referencedType = model.typeOf(referencedClass);
                QueryAttribute<S, Object> tKey = null;
                QueryAttribute<S, Object> uKey = null;
                for (Attribute a : referencedType.getAttributes()) {
                    if (entityClass.isAssignableFrom(a.getReferencedClass())) {
                        tKey = Attributes.query(a);
                    } else if (attribute.getElementClass().isAssignableFrom(
                            a.getReferencedClass())) {
                        uKey = Attributes.query(a);
                    }
                }
                Objects.requireNotNull(tKey);
                Objects.requireNotNull(uKey);
                Attribute<E, Object> tRef = Attributes.get(tKey.getReferencedAttribute());
                Attribute<S, Object> uRef = Attributes.get(uKey.getReferencedAttribute());

                CollectionChanges<?, S> changes = null;
                relation = proxy.get(attribute, false);
                Iterable<S> addedElements = (Iterable<S>) relation;
                boolean isObservable = relation instanceof ObservableCollection;
                if (relation instanceof ObservableCollection) {
                    ObservableCollection<S> collection = (ObservableCollection<S>) relation;
                    changes = (CollectionChanges<?, S>) collection.observer();
                    if (changes != null) {
                        addedElements = changes.addedElements();
                    }
                }
                for (S added : addedElements) {
                    S junction = (S) referencedType.getFactory().get();
                    EntityProxy<S> junctionProxy = context.proxyOf(junction, false);
                    EntityProxy<S> uProxy = context.proxyOf(added, false);

                    if (attribute.getCascadeActions().contains(CascadeAction.SAVE)) {
                        cascadeWrite(mode, added, uProxy);
                    }
                    Object tValue = proxy.get(tRef, false);
                    Object uValue = uProxy.get(uRef, false);

                    junctionProxy.set(tKey, tValue, PropertyState.MODIFIED);
                    junctionProxy.set(uKey, uValue, PropertyState.MODIFIED);

                    Cascade cascade = isObservable && mode == Cascade.UPSERT ?
                            Cascade.UPSERT : Cascade.INSERT;
                    cascadeWrite(cascade, junction, null);
                }
                if (changes != null) {
                    Object keyValue = proxy.get(tRef, false);
                    for (S removed : changes.removedElements()) {
                        Object otherValue = context.proxyOf(removed, false).get(uRef);
                        Class<? extends S> removeType = (Class<? extends S>)
                                referencedType.getClassType();

                        Supplier<? extends Scalar<Integer>> query = queryable.delete(removeType)
                                .where(tKey.equal(keyValue))
                                .and(uKey.equal(otherValue));
                        int count = query.get().value();
                        if (count != 1) {
                            throw new RowCountException(1, count);
                        }
                    }
                    changes.clear();
                }
                break;
            case MANY_TO_ONE:
            default:
                break;
        }
        context.read(type.getClassType()).refresh(entity, proxy, attribute);
    }

    private void incrementVersion(EntityProxy<E> proxy) {
        if (versionAttribute != null && !hasSystemVersionColumn()) {
            Object version = proxy.get(versionAttribute);
            Class<?> type = versionAttribute.getClassType();
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
                throw new PersistenceException("Unsupported version type: " +
                        versionAttribute.getClassType());
            }
            proxy.setObject(versionAttribute, version, PropertyState.MODIFIED);
        }
    }

    private Object incrementVersion(EntityProxy<E> proxy, Predicate<Attribute<E, ?>> filter) {
        boolean modified = false;
        for (Attribute<E, ?> attribute : bindableAttributes) {
            if (attribute != versionAttribute && filter.test(attribute)) {
                modified = true;
                break;
            }
        }
        final Object version = proxy.get(versionAttribute, true);
        if (modified) {
            if (version == null) {
                throw new MissingVersionException(proxy);
            }
            incrementVersion(proxy);
        }
        return version;
    }

    private void updateMappedAssociation(Cascade mode, S entity, Attribute attribute,
                                         Object value) {
        EntityProxy<S> proxy = context.proxyOf(entity, false);
        Attribute<S, Object> mapped = Attributes.get(attribute.getMappedAttribute());
        proxy.set(mapped, value, PropertyState.MODIFIED);
        cascadeWrite(mode, entity, proxy);
    }

    public void delete(E entity, EntityProxy<E> proxy) {
        context.getStateListener().preDelete(entity, proxy);
        proxy.unlink();
        if (cacheable) {
            cache.invalidate(entityClass, proxy.key());
        }
        // if cascade delete and the property is not loaded (load it)
        for (Attribute<E, ?> attribute : associativeAttributes) {
            boolean delete = attribute.getCascadeActions().contains(CascadeAction.DELETE);
            if (delete && (stateless || proxy.getState(attribute) == PropertyState.FETCH)) {
                context.read(type.getClassType()).refresh(entity, proxy, attribute);
            }
        }

        Deletion<? extends Scalar<Integer>> deletion = queryable.delete(entityClass);

        for (Attribute<E, ?> attribute : whereAttributes) {
            if (attribute == versionAttribute) {
                Object version = proxy.get(versionAttribute, true);
                if (version == null) {
                    throw new MissingVersionException(proxy);
                }
                addVersionCondition(deletion, version);
            } else {
                QueryAttribute<E, Object> id = Attributes.query(attribute);
                deletion.where(id.equal(proxy.get(attribute)));
            }
        }

        int rows = deletion.get().value();
        boolean cascaded = clearAssociations(entity, proxy);
        if (!cascaded) {
            checkRowsAffected(rows, entity, proxy);
        }
        context.getStateListener().postDelete(entity, proxy);
    }

    private boolean clearAssociations(E entity, EntityProxy<E> proxy) {
        // if deleting any foreign key reference would cascade to this entity then marked true
        boolean cascade = false;
        for (Attribute<E, ?> attribute : associativeAttributes) {
            boolean delete = attribute.getCascadeActions().contains(CascadeAction.DELETE);
            Object value = proxy.get(attribute, false);
            proxy.set(attribute, null, PropertyState.LOADED);
            if (value != null) {
                if (attribute.isForeignKey() &&
                    attribute.getDeleteAction() == ReferentialAction.CASCADE) {
                    cascade = true;
                }
                switch (attribute.getCardinality()) {
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
        }
        return cascade;
    }

    private void cascadeKeyReference(Cascade mode, EntityProxy<E> proxy,
                                     Attribute<E, ?> attribute) {
        S referenced = foreignKeyReference(proxy, attribute);
        if (referenced != null && proxy.getState(attribute) == PropertyState.MODIFIED) {
            EntityProxy<S> referred = context.proxyOf(referenced, false);
            if (!referred.isLinked()) {
                proxy.setState(attribute, PropertyState.LOADED);
                cascadeWrite(mode, referenced, null);
            }
        }
    }

    private <U extends S> void cascadeWrite(Cascade mode, U entity, EntityProxy<U> proxy) {
        if (entity != null) {
            if (proxy == null) {
                proxy = context.proxyOf(entity, false);
            }
            EntityWriter<U, S> writer = context.write(proxy.type().getClassType());
            if (mode == Cascade.AUTO) {
                mode = proxy.isLinked()? Cascade.UPDATE : Cascade.UPSERT;
            }
            switch (mode) {
                case INSERT:
                    writer.insert(entity, proxy, mode, null);
                    break;
                case UPDATE:
                    writer.update(entity, proxy, mode, null, null);
                    break;
                case UPSERT:
                    writer.upsert(entity, proxy);
                    break;
            }
        }
    }

    private <U extends S> void cascadeRemove(E entity, U element, boolean delete) {
        EntityProxy<U> proxy = context.proxyOf(element, false);
        if (proxy != null) {
            EntityWriter<U, S> writer = context.write(proxy.type().getClassType());
            if (delete && proxy.isLinked()) {
                writer.delete(element, proxy);
            } else {
                writer.removeEntity(proxy, entity);
            }
        }
    }

    void delete(Iterable<E> entities) {
        if (keyCount == 0) {
            for (E entity : entities) {
                delete(entity, type.getProxyProvider().apply(entity));
            }
        } else {
            batchDelete(entities);
        }
    }

    private void batchDelete(Iterable<E> entities) {
        final int batchSize = context.getBatchUpdateSize();
        final Iterator<E> iterator = entities.iterator();

        while (iterator.hasNext()) {
            final List<Object> ids = new LinkedList<>();
            while (iterator.hasNext() && ids.size() < batchSize) {
                E entity = iterator.next();
                EntityProxy<E> proxy = proxyProvider.apply(entity);
                if (versionAttribute != null || keyCount > 1) {
                    // not optimized if version column has to be checked, or multiple primary keys
                    // TODO could use JDBC batching
                    delete(entity, proxy);
                } else {
                    context.getStateListener().preDelete(entity, proxy);
                    boolean cascaded = clearAssociations(entity, proxy);

                    Object key = proxy.key();
                    if (cacheable) {
                        cache.invalidate(entityClass, key);
                    }
                    if (!cascaded) {
                        ids.add(key);
                    }
                    proxy.unlink();
                    context.getStateListener().postDelete(entity, proxy);
                }
            }
            // optimized case: delete from T where key in (keys...)
            if (ids.size() > 0) {
                Deletion<? extends Scalar<Integer>> deletion = queryable.delete(entityClass);
                for (Attribute<E, ?> attribute : type.getKeyAttributes()) {
                    QueryAttribute<E, Object> id = Attributes.query(attribute);
                    deletion.where(id.in(ids));
                }
                int rows = deletion.get().value();
                if (rows != ids.size()) {
                    throw new RowCountException(ids.size(), rows);
                }
            }
        }
    }

    private <U extends S> boolean hasKey(EntityProxy<U> proxy) {
        Type<U> type = proxy.type();
        if (keyCount > 0) {
            for (Attribute<U, ?> attribute : type.getKeyAttributes()) {
                PropertyState state = proxy.getState(attribute);
                if (!(state == PropertyState.MODIFIED || state == PropertyState.LOADED)) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    private void removeEntity(EntityProxy<E> proxy, S entity) {
        for (Attribute<E, ?> attribute : associativeAttributes) {
            Object value = proxy.get(attribute, false);
            switch (attribute.getCardinality()) {
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
                    } else if (value instanceof MutableResult) {
                        @SuppressWarnings("unchecked")
                        MutableResult<Object> result = (MutableResult) value;
                        result.remove(entity);
                    }
                    break;
            }
        }
    }
}
