package io.requery.sql;

import io.requery.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class IterableInliner {

    /**
     * Contains the transformed parameters of {@link #inlineIterables(String, Object[])}.
     */
    static final class IterableInlineResult {
        private String sql;
        private Object[] parameters;

        private IterableInlineResult(Object[] parameters, String sql) {
            this.parameters = parameters;
            this.sql = sql;
        }

        /**
         * The parameters where Iterables and Arrays have been inlined.
         */
        public Object[] getParameters() {
            return parameters;
        }

        /**
         * The modified SQL statement where question marks
         * have been added for newly inlined Iterables and Arrays.
         */
        public String getSql() {
            return sql;
        }
    }

    private static final Pattern questionMarkPattern = Pattern.compile("\\?");

    private IterableInliner() {
    }

    /**
     * Since SQL has no native support for Arrays and Iterables in "IN"-clauses,
     * this method inlines them. An example: <br/>
     * <br/>
     * <code>{@link IterableInlineResult} res = inlineIterables("select * from Person where id in ?", Arrays.asList(1, 2, 3));</code><br/>
     * <br/>
     * This is transformed into "select * from Person where id in (?, ?, ?) and the resulting new
     * parameters are (int, int, int) instead of (List).<br/>
     * Supported types to be inlined are {@link Iterable}s, Arrays of primitive
     * and reference types.
     */
    static IterableInlineResult inlineIterables(String sql, Object[] parameters) {
        List<Integer> indicesOfArguments = new ArrayList<>(parameters.length);
        Matcher matcher = questionMarkPattern.matcher(sql);
        while (matcher.find()) {
            indicesOfArguments.add(matcher.start());
        }

        StringBuilder inlineBuilder = new StringBuilder(sql);
        List<Object> newParameters = new ArrayList<>(Arrays.asList(parameters)); // Modifiable copy

        // Iterate backwords to avoid modifying the indices of
        // parameters in the front
        for (int i = parameters.length - 1; i >= 0; i--) {
            Object parameter = parameters[i];
            int argumentStringIndex = indicesOfArguments.get(i);

            if (parameter instanceof Iterable) {
                //noinspection unchecked
                List<Object> objects = CollectionUtils.toList((Iterable<Object>) parameter);
                inlineBuilder.replace(argumentStringIndex, argumentStringIndex + 1, argumentTuple(objects.size()));
                newParameters.addAll(0, objects);
            } else if (parameter instanceof byte[]) {
                inlineBuilder.replace(argumentStringIndex, argumentStringIndex + 1, argumentTuple(((byte[]) parameter).length));
                newParameters.addAll(0, CollectionUtils.toList(((byte[]) parameter)));
            } else if (parameter instanceof short[]) {
                inlineBuilder.replace(argumentStringIndex, argumentStringIndex + 1, argumentTuple(((short[]) parameter).length));
                newParameters.addAll(0, CollectionUtils.toList(((short[]) parameter)));
            } else if (parameter instanceof int[]) {
                inlineBuilder.replace(argumentStringIndex, argumentStringIndex + 1, argumentTuple(((int[]) parameter).length));
                newParameters.addAll(0, CollectionUtils.toList(((int[]) parameter)));
            } else if (parameter instanceof long[]) {
                inlineBuilder.replace(argumentStringIndex, argumentStringIndex + 1, argumentTuple(((long[]) parameter).length));
                newParameters.addAll(0, CollectionUtils.toList(((long[]) parameter)));
            } else if (parameter instanceof float[]) {
                inlineBuilder.replace(argumentStringIndex, argumentStringIndex + 1, argumentTuple(((float[]) parameter).length));
                newParameters.addAll(0, CollectionUtils.toList(((float[]) parameter)));
            } else if (parameter instanceof double[]) {
                inlineBuilder.replace(argumentStringIndex, argumentStringIndex + 1, argumentTuple(((double[]) parameter).length));
                newParameters.addAll(0, CollectionUtils.toList(((double[]) parameter)));
            } else if (parameter instanceof Object[]) {
                inlineBuilder.replace(argumentStringIndex, argumentStringIndex + 1, argumentTuple(((Object[]) parameter).length));
                newParameters.addAll(0, Arrays.asList((Object[]) parameter));
            } else {
                newParameters.add(0, parameter);
            }
        }

        return new IterableInlineResult(newParameters.toArray(), inlineBuilder.toString());
    }

    /**
     * Build a String of the form "(?, ?, ..., ?)" where the number
     * of question marks is length.
     */
    private static String argumentTuple(int length) {
        StringBuilder sb = new StringBuilder("(");
        for (int i = 0; i < length; i++) {
            sb.append("?");
            if (i + 1 < length) {
                sb.append(", ");
            }
        }
        sb.append(")");
        return sb.toString();
    }

}
