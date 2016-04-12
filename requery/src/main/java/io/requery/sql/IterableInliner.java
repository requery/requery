package io.requery.sql;

import io.requery.util.CollectionUtils;

import java.util.ArrayList;
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
        if (!containsIterableOrArray(parameters)) {
            return new IterableInlineResult(parameters, sql);
        }

        List<Integer> indicesOfArguments = new ArrayList<>(parameters.length);
        Matcher matcher = questionMarkPattern.matcher(sql);
        while (matcher.find()) {
            indicesOfArguments.add(matcher.start());
        }

        StringBuilder inlineBuilder = new StringBuilder(sql);
        ArrayList<Object> newParameters = new ArrayList<>(); // Modifiable copy

        // Iterate backwords to avoid modifying the indices of
        // parameters in the front
        for (int i = parameters.length - 1; i >= 0; i--) {
            Object parameter = parameters[i];
            int argumentStringIndex = indicesOfArguments.get(i);

            if (parameter instanceof Iterable) {
                int sizeBefore = newParameters.size();
                //noinspection unchecked
                CollectionUtils.insertIntoListBeginning((Iterable<Object>) parameter, newParameters);
                inlineBuilder.replace(argumentStringIndex, argumentStringIndex + 1, argumentTuple(newParameters.size() - sizeBefore));
            } else if (parameter instanceof byte[]) {
                inlineBuilder.replace(argumentStringIndex, argumentStringIndex + 1, argumentTuple(((byte[]) parameter).length));
                CollectionUtils.insertIntoListBeginning((byte[]) parameter, newParameters);
            } else if (parameter instanceof short[]) {
                inlineBuilder.replace(argumentStringIndex, argumentStringIndex + 1, argumentTuple(((short[]) parameter).length));
                CollectionUtils.insertIntoListBeginning((short[]) parameter, newParameters);
            } else if (parameter instanceof int[]) {
                inlineBuilder.replace(argumentStringIndex, argumentStringIndex + 1, argumentTuple(((int[]) parameter).length));
                CollectionUtils.insertIntoListBeginning((int[]) parameter, newParameters);
            } else if (parameter instanceof long[]) {
                inlineBuilder.replace(argumentStringIndex, argumentStringIndex + 1, argumentTuple(((long[]) parameter).length));
                CollectionUtils.insertIntoListBeginning((long[]) parameter, newParameters);
            } else if (parameter instanceof float[]) {
                inlineBuilder.replace(argumentStringIndex, argumentStringIndex + 1, argumentTuple(((float[]) parameter).length));
                CollectionUtils.insertIntoListBeginning((float[]) parameter, newParameters);
            } else if (parameter instanceof double[]) {
                inlineBuilder.replace(argumentStringIndex, argumentStringIndex + 1, argumentTuple(((double[]) parameter).length));
                CollectionUtils.insertIntoListBeginning((double[]) parameter, newParameters);
            } else if (parameter instanceof boolean[]) {
                inlineBuilder.replace(argumentStringIndex, argumentStringIndex + 1, argumentTuple(((boolean[]) parameter).length));
                CollectionUtils.insertIntoListBeginning((boolean[]) parameter, newParameters);
            } else if (parameter instanceof char[]) {
                inlineBuilder.replace(argumentStringIndex, argumentStringIndex + 1, argumentTuple(((char[]) parameter).length));
                CollectionUtils.insertIntoListBeginning((char[]) parameter, newParameters);
            } else if (parameter instanceof Object[]) {
                inlineBuilder.replace(argumentStringIndex, argumentStringIndex + 1, argumentTuple(((Object[]) parameter).length));
                CollectionUtils.insertIntoListBeginning((Object[]) parameter, newParameters);
            } else {
                newParameters.add(0, parameter);
            }
        }

        return new IterableInlineResult(newParameters.toArray(), inlineBuilder.toString());
    }

    private static boolean containsIterableOrArray(Object[] parameters) {
        for (Object parameter : parameters) {
            if (parameter instanceof Iterable || parameter.getClass().isArray()) {
                return true;
            }
        }
        return false;
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
