package de.waterdu.atlantis.util.java.collection;

import de.waterdu.atlantis.util.java.ExceptionUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Function;

public final class CollectionUtils {

    private CollectionUtils() {
        ExceptionUtils.throwCannotInstantiateClassException();
    }

    public static <T, V> Collection<V> map(Collection<T> src, Function<T, V> mapper) {
        return CollectionUtils.map(src, new ArrayList<>(), mapper);
    }

    public static <T, V, D extends Collection<V>> D map(Collection<T> src, D dest, Function<T, V> mapper) {
        for (var instance : src) {
            dest.add(mapper.apply(instance));
        }
        return dest;
    }
}
