package io.requery.util;

import java.util.ArrayList;
import java.util.List;

/**
 * Helper class for {@link java.util.Collection} objects
 * and (primitive) arrays.
 */
public final class CollectionUtils {

    private CollectionUtils() {
    }

    /**
     * Copies the array into a modifiable list.
     */
    public static List<Byte> toList(byte[] arr) {
        List<Byte> list = new ArrayList<>(arr.length);
        for (byte value : arr) {
            list.add(value);
        }
        return list;
    }

    /**
     * Copies the array into a modifiable list.
     */
    public static List<Short> toList(short[] arr) {
        List<Short> list = new ArrayList<>(arr.length);
        for (short value : arr) {
            list.add(value);
        }
        return list;
    }

    /**
     * Copies the array into a modifiable list.
     */
    public static List<Integer> toList(int[] arr) {
        List<Integer> list = new ArrayList<>(arr.length);
        for (int value : arr) {
            list.add(value);
        }
        return list;
    }

    /**
     * Copies the array into a modifiable list.
     */
    public static List<Long> toList(long[] arr) {
        List<Long> list = new ArrayList<>(arr.length);
        for (long value : arr) {
            list.add(value);
        }
        return list;
    }

    /**
     * Copies the array into a modifiable list.
     */
    public static List<Float> toList(float[] arr) {
        List<Float> list = new ArrayList<>(arr.length);
        for (float value : arr) {
            list.add(value);
        }
        return list;
    }

    /**
     * Copies the array into a modifiable list.
     */
    public static List<Double> toList(double[] arr) {
        List<Double> list = new ArrayList<>(arr.length);
        for (double value : arr) {
            list.add(value);
        }
        return list;
    }

    /**
     * Copies the {@link Iterable} into a modifiable list.
     */
    public static <T> List<T> toList(Iterable<T> iterable) {
        List<T> list = new ArrayList<>();
        for (T t : iterable) {
            list.add(t);
        }
        return list;
    }
}
