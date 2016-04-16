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

import io.requery.util.ArrayFunctions;
import io.requery.util.function.Consumer;
import io.requery.util.function.Predicate;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Since SQL has no native support for Arrays and Iterables in "IN"-clauses,
 * this method inlines them. Example: <br/>
 * <br/>
 * <code>raw("select * from Person where id in ?",
 * Arrays.asList(1, 2, 3));</code><br/>
 * <br/>
 * This is transformed into "select * from Person where id in (?, ?, ?) and the resulting new
 * parameters are (int, int, int) instead of (List).<br/>
 * Supported types to be inlined are {@link Iterable}s, Arrays of primitive
 * and reference types.
 */
final class ParameterInliner implements Predicate<Object[]> {

    private static final Pattern questionMarkPattern = Pattern.compile("\\?");

    private String sql;
    private Object[] parameters;

    ParameterInliner(String sql, Object[] parameters) {
        this.sql = sql;
        this.parameters = parameters;
    }

    /**
     * The parameters with the additional inlined elements.
     */
    public Object[] parameters() {
        return parameters;
    }

    /**
     * The modified SQL statement where ? placeholders have been added for inlined elements.
     */
    public String sql() {
        return sql;
    }

    ParameterInliner apply() {
        if (!test(parameters)) {
            return this;
        }
        List<Integer> indicesOfArguments = new ArrayList<>(parameters.length);
        Matcher matcher = questionMarkPattern.matcher(sql);
        while (matcher.find()) {
            indicesOfArguments.add(matcher.start());
        }

        StringBuilder sb = new StringBuilder(sql);
        ArrayList<Object> list = new ArrayList<>();

        // Iterate backwards to avoid modifying the indices of parameters in the front
        for (int i = parameters.length - 1; i >= 0; i--) {
            Object parameter = parameters[i];
            int index = indicesOfArguments.get(i);

            if (parameter instanceof Iterable) {
                int sizeBefore = list.size();
                Iterable iterable = (Iterable) parameter;
                Consumer<Object> collector = collect(list);
                for (Object e : iterable) {
                    collector.accept(e);
                }
                expand(sb, index, list.size() - sizeBefore);
            } else if (parameter instanceof short[]) {
                short[] array = (short[]) parameter;
                ArrayFunctions.forEach(array, collect(list));
                expand(sb, index, array.length);
            } else if (parameter instanceof int[]) {
                int[] array = (int[]) parameter;
                ArrayFunctions.forEach(array, collect(list));
                expand(sb, index, array.length);
            } else if (parameter instanceof long[]) {
                long[] array = (long[]) parameter;
                ArrayFunctions.forEach(array, collect(list));
                expand(sb, index, array.length);
            } else if (parameter instanceof float[]) {
                float[] array = (float[]) parameter;
                ArrayFunctions.forEach(array, collect(list));
                expand(sb, index, array.length);
            } else if (parameter instanceof double[]) {
                double[] array = (double[]) parameter;
                ArrayFunctions.forEach(array, collect(list));
                expand(sb, index, array.length);
            } else if (parameter instanceof boolean[]) {
                boolean[] array = (boolean[]) parameter;
                ArrayFunctions.forEach(array, collect(list));
                expand(sb, index, array.length);
            } else if (parameter instanceof Object[]) {
                Object[] array = (Object[]) parameter;
                ArrayFunctions.forEach(array, collect(list));
                expand(sb, index, array.length);
            } else {
                list.add(0, parameter);
            }
        }
        sql = sb.toString();
        parameters = list.toArray();
        return this;
    }

    private static  <T> Consumer<T> collect(final ArrayList<T> list) {
        return new Consumer<T>() {
            int index = 0;
            @Override
            public void accept(T t) {
                list.add(index++, t);
            }
        };
    }

    @Override
    public boolean test(Object[] value) {
        for (Object parameter : parameters) {
            if (parameter instanceof Iterable ||
                (parameter != null && parameter.getClass().isArray())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Build a String of the form "(?, ?, ..., ?)" where the number of question marks is length.
     */
    private void expand(StringBuilder sb, int index, int length) {
        StringBuilder replacement = new StringBuilder("(");
        for (int i = 0; i < length; i++) {
            replacement.append("?");
            if (i + 1 < length) {
                replacement.append(", ");
            }
        }
        replacement.append(")");
        sb.replace(index, index + 1, replacement.toString());
    }
}
