package de.waterdu.atlantis.util.java.collection;

import de.waterdu.atlantis.util.java.ExceptionUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Function;

/**
 * Maps collections.
 */
public final class CollectionUtils {

    private CollectionUtils() {
        ExceptionUtils.throwCannotInstantiateClassException();
    }

    /**
     * Maps each element of {@code src} into a new {@link ArrayList}.
     *
     * @param <T> the type of the source elements
     * @param <V> the type of the mapped elements
     * @param src the elements to map
     * @param mapper maps one element
     * @return the new list
     */
    public static <T, V> Collection<V> map(Collection<T> src, Function<T, V> mapper) {
        return CollectionUtils.map(src, new ArrayList<>(), mapper);
    }

    /**
     * Maps each element of {@code src} and adds the result to {@code dest}.
     *
     * @param <T> the type of the source elements
     * @param <V> the type of the mapped elements
     * @param <D> the type of the destination collection
     * @param src the elements to map
     * @param dest the collection to add the mapped elements to
     * @param mapper maps one element
     * @return {@code dest}
     */
    public static <T, V, D extends Collection<V>> D map(Collection<T> src, D dest, Function<T, V> mapper) {
        for (var instance : src) {
            dest.add(mapper.apply(instance));
        }
        return dest;
    }
}
