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

package io.requery.processor;

import io.requery.PropertyNameStyle;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Definition of an entity to be processed.
 *
 * @author Nikhil Purushe
 */
interface EntityDescriptor {

    /**
     * @return {@link TypeElement} element being represented as an entity for processing
     */
    TypeElement element();

    /**
     * @return map of elements to attributes
     */
    Map<Element, ? extends AttributeDescriptor> attributes();

    /**
     * @return true if this entity type requires additional types to be generated to compile.
     */
    boolean generatesAdditionalTypes();

    /**
     * @return map of elements to listener methods
     */
    Map<Element, ? extends ListenerDescriptor> listeners();

    /**
     * @return {@link QualifiedName} qualified name of the class to be generated
     */
    QualifiedName typeName();

    /**
     * @return Name of the model this entity belongs to
     */
    String modelName();

    /**
     * @return Name of the table this entity is being mapped to
     */
    String tableName();

    /**
     * @return Optional name of the class factory which will instantiates instances of the entity
     * type.
     */
    String classFactoryName();

    /**
     * @return table attributes used during table generation
     */
    String[] tableAttributes();

    /**
     * @return table unique indexes used during table generation
     */
    String[] tableUniqueIndexes();

    /**
     * @return {@link PropertyNameStyle} style of the accessors in the entity
     */
    PropertyNameStyle propertyNameStyle();

    /**
     * @return true if the entity is cacheable, false otherwise
     */
    boolean isCacheable();

    /**
     * @return true if the entity is copyable, false otherwise
     */
    boolean isCopyable();

    /**
     * @return true if this an embedded entity type.
     */
    boolean isEmbedded();

    /**
     * @return true if the underlying type being represented is immutable, false otherwise
     */
    boolean isImmutable();

    /**
     * @return true if the entity is read only, differs from immutable in that the properties can
     * still be set in the generated entity by the framework but no setters are generated
     */
    boolean isReadOnly();

    /**
     * @return true if the entity has no modification state in the generated entity
     */
    boolean isStateless();

    /**
     * @return true if the annotated type should not be extended/implemented by the generation step.
     * Either the source class that is final (cannot be extended) or another limitation prevents it
     * from being extended/implemented.
     */
    boolean isUnimplementable();

    /**
     * @return {@link TypeElement} of the builder class that can build instances of the entity if
     * the type is {@link #isImmutable()}
     */
    Optional<TypeElement> builderType();

    /**
     * @return {@link ExecutableElement} of the builder type that can create builder instances for
     * the entity if the type is {@link #isImmutable()} such as:
     * <pre><code>
     *     &#064;Entity
     *     &#064;AutoValue
     *     public abstract class Phone {
     *         static Phone.Builder builder() {
     *             return new AutoValue_Phone.Builder();
     *         }
     *     }
     * </code></pre>
     */
    Optional<ExecutableElement> builderFactoryMethod();

    /**
     * @return {@link ExecutableElement} of the type can create instances of the entity if the type
     * is {@link #isImmutable()}, such as:
     * <pre><code>
     *     &#064;Entity
     *     &#064;AutoValue
     *     public abstract class Phone {
     *         static Phone create(int id, String phone) {
     *             return new AutoValue_Phone(id, phone);
     *         }
     *     }
     * </code></pre>
     */
    Optional<ExecutableElement> factoryMethod();

    /**
     * @return the list of argument names for the {@link #factoryMethod()}
     */
    List<String> factoryArguments();
}
