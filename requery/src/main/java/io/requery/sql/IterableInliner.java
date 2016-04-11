package io.requery.sql;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class IterableInliner {

    static final class IterableInlineResult {
        private String sql;
        private Object[] parameters;

        private IterableInlineResult(Object[] parameters, String sql) {
            this.parameters = parameters;
            this.sql = sql;
        }

        public Object[] getParameters() {
            return parameters;
        }

        public String getSql() {
            return sql;
        }
    }

    private static final Pattern questionMarkPattern = Pattern.compile("\\?");

    private IterableInliner() {
    }

    static IterableInlineResult inlineIterables(String sql, Object[] parameters) {
        List<Integer> indicesOfArguments = new ArrayList<>(parameters.length);
        Matcher matcher = questionMarkPattern.matcher(sql);
        while (matcher.find()) {
            indicesOfArguments.add(matcher.start());
        }

        StringBuilder inlineBuilder = new StringBuilder(sql);
        List<Object> newParameters = new ArrayList<>(Arrays.asList(parameters));

        for (int i = parameters.length - 1; i >= 0; i--) {
            Object parameter = parameters[i];
            int argumentStringIndex = indicesOfArguments.get(i);

            if (parameter instanceof Iterable) {
                //noinspection unchecked
                List<Object> objects = toList((Iterable<Object>) parameter);
                inlineBuilder.replace(argumentStringIndex, argumentStringIndex + 1, argumentTuple(objects.size()));
                newParameters.addAll(0, objects);
            } else if (parameter instanceof byte[]) {
                inlineBuilder.replace(argumentStringIndex, argumentStringIndex + 1, argumentTuple(((byte[]) parameter).length));
                newParameters.addAll(0, toList(((byte[]) parameter)));
            } else if (parameter instanceof short[]) {
                inlineBuilder.replace(argumentStringIndex, argumentStringIndex + 1, argumentTuple(((short[]) parameter).length));
                newParameters.addAll(0, toList(((short[]) parameter)));
            } else if (parameter instanceof int[]) {
                inlineBuilder.replace(argumentStringIndex, argumentStringIndex + 1, argumentTuple(((int[]) parameter).length));
                newParameters.addAll(0, toList(((int[]) parameter)));
            } else if (parameter instanceof long[]) {
                inlineBuilder.replace(argumentStringIndex, argumentStringIndex + 1, argumentTuple(((long[]) parameter).length));
                newParameters.addAll(0, toList(((long[]) parameter)));
            } else if (parameter instanceof float[]) {
                inlineBuilder.replace(argumentStringIndex, argumentStringIndex + 1, argumentTuple(((float[]) parameter).length));
                newParameters.addAll(0, toList(((float[]) parameter)));
            } else if (parameter instanceof double[]) {
                inlineBuilder.replace(argumentStringIndex, argumentStringIndex + 1, argumentTuple(((double[]) parameter).length));
                newParameters.addAll(0, toList(((double[]) parameter)));
            } else if (parameter instanceof Object[]) {
                inlineBuilder.replace(argumentStringIndex, argumentStringIndex + 1, argumentTuple(((Object[]) parameter).length));
                newParameters.addAll(0, Arrays.asList((Object[]) parameter));
            } else {
                newParameters.add(0, parameter);
            }
        }

        return new IterableInlineResult(newParameters.toArray(), inlineBuilder.toString());
    }

    private static List<Byte> toList(byte[] arr) {
        List<Byte> list = new ArrayList<>(arr.length);
        for (byte value : arr) {
            list.add(value);
        }
        return list;
    }

    private static List<Short> toList(short[] arr) {
        List<Short> list = new ArrayList<>(arr.length);
        for (short value : arr) {
            list.add(value);
        }
        return list;
    }

    private static List<Integer> toList(int[] arr) {
        List<Integer> list = new ArrayList<>(arr.length);
        for (int value : arr) {
            list.add(value);
        }
        return list;
    }

    private static List<Long> toList(long[] arr) {
        List<Long> list = new ArrayList<>(arr.length);
        for (long value : arr) {
            list.add(value);
        }
        return list;
    }

    private static List<Float> toList(float[] arr) {
        List<Float> list = new ArrayList<>(arr.length);
        for (float value : arr) {
            list.add(value);
        }
        return list;
    }

    private static List<Double> toList(double[] arr) {
        List<Double> list = new ArrayList<>(arr.length);
        for (double value : arr) {
            list.add(value);
        }
        return list;
    }

    private static <T> List<T> toList(Iterable<T> iterable) {
        if (iterable instanceof List) {
            return (List<T>) iterable;
        }

        List<T> list = new ArrayList<>();
        for (T t : iterable) {
            list.add(t);
        }
        return list;
    }

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
