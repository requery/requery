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

package io.requery.query.function;

import io.requery.query.Condition;
import io.requery.query.Expression;

import java.util.ArrayList;

public class Case<E> extends Function<E> {

    private final ArrayList<CaseCondition<?, ?>> conditions;
    private Object elseValue;

    private Case(String name, Class<E> type) {
        super(name, type);
        conditions = new ArrayList<>();
    }

    public static <S> Case<S> type(String name, Class<S> type) {
        return new Case<>(name, type);
    }

    public static <S> Case<S> type(Class<S> type) {
        return new Case<>(null, type);
    }

    public <U, V> Case<E> when(Condition<U, ?> condition, V then) {
        conditions.add(new CaseCondition<>(condition, then));
        return this;
    }

    public <V> Expression<E> elseThen(V result) {
        elseValue = result;
        return this;
    }

    @Override
    public Object[] arguments() {
        return new Object[]{};
    }

    public Object elseValue() {
        return elseValue;
    }

    public ArrayList<CaseCondition<?, ?>> conditions() {
        return conditions;
    }

    public static class CaseCondition<V, W> {
        private final Condition<V, ?> condition;
        private final W then;

        CaseCondition(Condition<V, ?> condition, W then) {
            this.condition = condition;
            this.then = then;
        }

        public Condition<V, ?> condition() {
            return condition;
        }

        public W thenValue() {
            return then;
        }
    }
}
