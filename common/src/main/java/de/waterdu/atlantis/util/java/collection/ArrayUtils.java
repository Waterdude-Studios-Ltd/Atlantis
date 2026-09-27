package de.waterdu.atlantis.util.java.collection;

import de.waterdu.atlantis.util.java.ExceptionUtils;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.function.Function;
import java.util.function.IntFunction;

public final class ArrayUtils {

    private ArrayUtils() {
        ExceptionUtils.throwCannotInstantiateClassException();
    }

    public static <T> T[] coerceTail(T[] src, T[] dest) {
        System.arraycopy(src, Math.max(0, src.length - dest.length), dest, 0, dest.length);
        return dest;
    }

    @SafeVarargs
    @SuppressWarnings("unchecked")
    public static <T, V> V[] map(Class<V> type, Function<T, V> mapper, T... src) {
        var newArray = (V[]) Array.newInstance(type, src.length);
        for (var i = 0; i < src.length; i++) {
            newArray[i] = mapper.apply(src[i]);
        }
        return newArray;
    }

    @SafeVarargs
    @SuppressWarnings("unchecked")
    public static <T, V> V[] mapNotNull(Class<V> type, Function<T, V> mapper, T... src) {
        var newList = new ArrayList<V>();
        for (var t : src) {
            var newValue = mapper.apply(t);
            if (newValue != null) {
                newList.add(newValue);
            }
        }
        return newList.toArray((V[]) Array.newInstance(type, newList.size()));
    }

    @SuppressWarnings("unchecked")
    public static <T> T[] arrayOf(Class<T> type, int size, IntFunction<T> mapper) {
        var array = (T[]) Array.newInstance(type, size);
        for (var i = 0; i < size; i++) {
            array[i] = mapper.apply(i);
        }
        return array;
    }

    @SafeVarargs
    @SuppressWarnings("unchecked")
    public static <T> T[] expand(T[] array, T... expansion) {
        var newArray = (T[]) Array.newInstance(array.getClass(), array.length + expansion.length);
        System.arraycopy(array, 0, newArray, 0, array.length);
        System.arraycopy(expansion, 0, newArray, array.length, expansion.length);
        return newArray;
    }
}
