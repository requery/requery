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

package io.requery.processor;

import io.requery.PropertyNameStyle;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

/**
 * Memoizes all the {@link EntityDescriptor} methods of the delegate type.
 * Delegates the other methods.
 */
class EntityElementDelegate implements EntityElement {

    private final EntityElement delegate;
    private final Supplier<Boolean> generatesAdditionalTypes;
    private final Supplier<QualifiedName> typeName;
    private final Supplier<String> modelName;
    private final Supplier<String> tableName;
    private final Supplier<String> classFactoryName;
    private final Supplier<String[]> tableAttributes;
    private final Supplier<String[]> tableUniqueIndexes;
    private final Supplier<PropertyNameStyle> propertyNameStyle;
    private final Supplier<Boolean> isCacheable;
    private final Supplier<Boolean> isCopyable;
    private final Supplier<Boolean> isEmbedded;
    private final Supplier<Boolean> isImmutable;
    private final Supplier<Boolean> isReadOnly;
    private final Supplier<Boolean> isStateless;
    private final Supplier<Boolean> isUnimplementable;
    private final Supplier<Boolean> isView;
    private final Supplier<Optional<TypeMirror>> builderType;
    private final Supplier<Optional<ExecutableElement>> builderFactoryMethod;
    private final Supplier<Optional<ExecutableElement>> factoryMethod;
    private final Supplier<List<String>> factoryArguments;

    EntityElementDelegate(EntityElement delegate) {
        this.delegate = delegate;
        generatesAdditionalTypes = memoized(delegate::generatesAdditionalTypes);
        typeName = memoized(delegate::typeName);
        modelName = memoized(delegate::modelName);
        tableName = memoized(delegate::tableName);
        classFactoryName = memoized(delegate::classFactoryName);
        tableAttributes = memoized(delegate::tableAttributes);
        tableUniqueIndexes = memoized(delegate::tableUniqueIndexes);
        propertyNameStyle = memoized(delegate::propertyNameStyle);
        isCacheable = memoized(delegate::isCacheable);
        isCopyable = memoized(delegate::isCopyable);
        isEmbedded = memoized(delegate::isEmbedded);
        isImmutable = memoized(delegate::isImmutable);
        isReadOnly = memoized(delegate::isReadOnly);
        isStateless = memoized(delegate::isStateless);
        isUnimplementable = memoized(delegate::isUnimplementable);
        isView = memoized(delegate::isView);
        builderType = memoized(delegate::builderType);
        builderFactoryMethod = memoized(delegate::builderFactoryMethod);
        factoryMethod = memoized(delegate::factoryMethod);
        factoryArguments = memoized(delegate::factoryArguments);
    }

    private static <T> MemoizedSupplier<T> memoized(Supplier<T> supplier) {
        return new MemoizedSupplier<>(supplier);
    }

    private static class MemoizedSupplier<T> implements Supplier<T> {
        private AtomicBoolean computed = new AtomicBoolean();
        private Supplier<T> supplier;
        private T value;

        MemoizedSupplier(Supplier<T> supplier) {
            this.supplier = supplier;
        }

        @Override
        public T get() {
            if (computed.compareAndSet(false, true)) {
                value = supplier.get();
            }
            return value;
        }
    }

    @Override
    public void addAnnotationElement(TypeElement annotationElement, Element annotatedElement) {
        delegate.addAnnotationElement(annotationElement, annotatedElement);
    }

    @Override
    public void merge(EntityDescriptor from) {
        delegate.merge(from);
    }

    @Override
    public TypeElement element() {
        return delegate.element();
    }

    @Override
    public Set<ElementValidator> process(ProcessingEnvironment processingEnvironment) {
        return delegate.process(processingEnvironment);
    }

    @Override
    public Map<Class<? extends Annotation>, Annotation> annotations() {
        return delegate.annotations();
    }

    @Override
    public Map<Element, ? extends AttributeDescriptor> attributes() {
        return delegate.attributes();
    }

    @Override
    public boolean generatesAdditionalTypes() {
        return generatesAdditionalTypes.get();
    }

    @Override
    public Map<Element, ? extends ListenerDescriptor> listeners() {
        return delegate.listeners();
    }

    @Override
    public QualifiedName typeName() {
        return typeName.get();
    }

    @Override
    public String modelName() {
        return modelName.get();
    }

    @Override
    public String tableName() {
        return tableName.get();
    }

    @Override
    public String classFactoryName() {
        return classFactoryName.get();
    }

    @Override
    public String[] tableAttributes() {
        return tableAttributes.get();
    }

    @Override
    public String[] tableUniqueIndexes() {
        return tableUniqueIndexes.get();
    }

    @Override
    public PropertyNameStyle propertyNameStyle() {
        return propertyNameStyle.get();
    }

    @Override
    public boolean isCacheable() {
        return isCacheable.get();
    }

    @Override
    public boolean isCopyable() {
        return isCopyable.get();
    }

    @Override
    public boolean isEmbedded() {
        return isEmbedded.get();
    }

    @Override
    public boolean isImmutable() {
        return isImmutable.get();
    }

    @Override
    public boolean isReadOnly() {
        return isReadOnly.get();
    }

    @Override
    public boolean isStateless() {
        return isStateless.get();
    }

    @Override
    public boolean isUnimplementable() {
        return isUnimplementable.get();
    }

    @Override
    public boolean isView() {
        return isView.get();
    }

    @Override
    public Optional<TypeMirror> builderType() {
        return builderType.get();
    }

    @Override
    public Optional<ExecutableElement> builderFactoryMethod() {
        return builderFactoryMethod.get();
    }

    @Override
    public Optional<ExecutableElement> factoryMethod() {
        return factoryMethod.get();
    }

    @Override
    public List<String> factoryArguments() {
        return factoryArguments.get();
    }
}
