package de.waterdu.atlantis.util.java.collection;

import de.waterdu.atlantis.util.java.ExceptionUtils;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.function.Function;
import java.util.function.IntFunction;

/**
 * Creates, maps and expands arrays.
 */
public final class ArrayUtils {

    private ArrayUtils() {
        ExceptionUtils.throwCannotInstantiateClassException();
    }

    /**
     * Fills {@code dest} with the last elements of {@code src}. {@code src} must not be shorter than {@code dest}.
     *
     * @return {@code dest}
     */
    public static <T> T[] coerceTail(T[] src, T[] dest) {
        System.arraycopy(src, Math.max(0, src.length - dest.length), dest, 0, dest.length);
        return dest;
    }

    /**
     * Maps each element of {@code src} into a new array of the same length.
     *
     * @param <T> the type of the source elements
     * @param <V> the type of the mapped elements
     * @param type the component type of the new array
     * @param mapper maps one element
     * @param src the elements to map
     * @return the new array
     */
    @SafeVarargs
    @SuppressWarnings("unchecked")
    public static <T, V> V[] map(Class<V> type, Function<T, V> mapper, T... src) {
        var newArray = (V[]) Array.newInstance(type, src.length);
        for (var i = 0; i < src.length; i++) {
            newArray[i] = mapper.apply(src[i]);
        }
        return newArray;
    }

    /**
     * Maps each element of {@code src} into a new array, without the elements for which the mapper returns
     * {@code null}.
     *
     * @param <T> the type of the source elements
     * @param <V> the type of the mapped elements
     * @param type the component type of the new array
     * @param mapper maps one element
     * @param src the elements to map
     * @return the new array
     */
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

    /**
     * Creates an array and fills each index with the result of the mapper for that index.
     *
     * @param <T> the type of the elements
     * @param type the component type of the array
     * @param size the length of the array
     * @param mapper creates the element for an index
     * @return the new array
     */
    @SuppressWarnings("unchecked")
    public static <T> T[] arrayOf(Class<T> type, int size, IntFunction<T> mapper) {
        var array = (T[]) Array.newInstance(type, size);
        for (var i = 0; i < size; i++) {
            array[i] = mapper.apply(i);
        }
        return array;
    }

    /**
     * Creates a new array with the elements of {@code array} followed by the elements of {@code expansion}. The new
     * array has the same component type as {@code array}.
     *
     * @param <T> the type of the elements
     * @param array the first elements
     * @param expansion the elements to add at the end
     * @return the new array
     */
    @SafeVarargs
    @SuppressWarnings("unchecked")
    public static <T> T[] expand(T[] array, T... expansion) {
        var newArray = (T[]) Array.newInstance(array.getClass().getComponentType(), array.length + expansion.length);
        System.arraycopy(array, 0, newArray, 0, array.length);
        System.arraycopy(expansion, 0, newArray, array.length, expansion.length);
        return newArray;
    }
}
