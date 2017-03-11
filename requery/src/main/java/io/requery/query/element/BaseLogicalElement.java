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

package io.requery.query.element;

import io.requery.query.AndOr;
import io.requery.query.Condition;
import io.requery.util.Objects;

import java.util.Set;

abstract class BaseLogicalElement<E extends S, S> implements AndOr<S>, LogicalElement {

    private final Set<E> elements;
    private final LogicalOperator operator;
    private final Condition<?,?> condition;

    BaseLogicalElement(Set<E> elements,
                       Condition<?,?> condition,
                       LogicalOperator operator) {
        this.elements = elements;
        this.condition = condition;
        this.operator = operator;
    }

    abstract E newElement(Set<E> elements, Condition<?,?> condition, LogicalOperator operator);

    @Override
    public <V> S and(Condition<V, ?> condition) {
        E element = newElement(elements, condition, LogicalOperator.AND);
        elements.add(element);
        return element;
    }

    @Override
    public <V> S or(Condition<V, ?> condition) {
        E element = newElement(elements, condition, LogicalOperator.OR);
        elements.add(element);
        return element;
    }

    @Override
    public Condition<?,?> getCondition() {
        return condition;
    }

    @Override
    public LogicalOperator getOperator() {
        return operator;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof BaseLogicalElement) {
            BaseLogicalElement other = (BaseLogicalElement) obj;
            return Objects.equals(operator, other.operator) &&
                Objects.equals(condition, other.condition);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(operator, condition);
    }
}
