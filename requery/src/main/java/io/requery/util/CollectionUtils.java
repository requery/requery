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
     * Inserts the array at index 0 in the supplied list.
     */
    public static void insertIntoListBeginning(byte[] arr, ArrayList<? super Byte> list) {
        list.ensureCapacity(list.size() + arr.length);
        for (int i = arr.length - 1; i >= 0; i--) {
            list.add(0, arr[i]);
        }
    }

    /**
     * Inserts the array at index 0 in the supplied list.
     */
    public static void insertIntoListBeginning(short[] arr, ArrayList<? super Short> list) {
        list.ensureCapacity(list.size() + arr.length);
        for (int i = arr.length - 1; i >= 0; i--) {
            list.add(0, arr[i]);
        }
    }

    /**
     * Inserts the array at index 0 in the supplied list.
     */
    public static void insertIntoListBeginning(int[] arr, ArrayList<? super Integer> list) {
        list.ensureCapacity(list.size() + arr.length);
        for (int i = arr.length - 1; i >= 0; i--) {
            list.add(0, arr[i]);
        }
    }

    /**
     * Inserts the array at index 0 in the supplied list.
     */
    public static void insertIntoListBeginning(long[] arr, ArrayList<? super Long> list) {
        list.ensureCapacity(list.size() + arr.length);
        for (int i = arr.length - 1; i >= 0; i--) {
            list.add(0, arr[i]);
        }
    }

    /**
     * Inserts the array at index 0 in the supplied list.
     */
    public static void insertIntoListBeginning(float[] arr, ArrayList<? super Float> list) {
        list.ensureCapacity(list.size() + arr.length);
        for (int i = arr.length - 1; i >= 0; i--) {
            list.add(0, arr[i]);
        }
    }

    /**
     * Inserts the array at index 0 in the supplied list.
     */
    public static void insertIntoListBeginning(double[] arr, ArrayList<? super Double> list) {
        list.ensureCapacity(list.size() + arr.length);
        for (int i = arr.length - 1; i >= 0; i--) {
            list.add(0, arr[i]);
        }
    }

    /**
     * Inserts the array at index 0 in the supplied list.
     */
    public static void insertIntoListBeginning(Object[] arr, ArrayList<Object> list) {
        list.ensureCapacity(list.size() + arr.length);
        for (int i = arr.length - 1; i >= 0; i--) {
            list.add(0, arr[i]);
        }
    }

    /**
     * Inserts the array at index 0 in the supplied list.
     */
    public static void insertIntoListBeginning(boolean[] arr, ArrayList<Object> list) {
        list.ensureCapacity(list.size() + arr.length);
        for (int i = arr.length - 1; i >= 0; i--) {
            list.add(0, arr[i]);
        }
    }

    /**
     * Inserts the array at index 0 in the supplied list.
     */
    public static void insertIntoListBeginning(char[] arr, ArrayList<Object> list) {
        list.ensureCapacity(list.size() + arr.length);
        for (int i = arr.length - 1; i >= 0; i--) {
            list.add(0, arr[i]);
        }
    }

    /**
     * Inserts the {@link Iterable} at index 0 in the supplied list.
     */
    public static <T> void insertIntoListBeginning(Iterable<T> iterable, ArrayList<? super T> list) {
        if (iterable instanceof List) {
            list.addAll(0, (List<T>) iterable);
            return;
        }

        int i = 0;
        for (T t : iterable) {
            list.add(i++, t);
        }
    }
}
