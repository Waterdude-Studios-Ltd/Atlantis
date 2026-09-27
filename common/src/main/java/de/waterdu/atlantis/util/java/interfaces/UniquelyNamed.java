package de.waterdu.atlantis.util.java.interfaces;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public interface UniquelyNamed {

    Map<Class<?>, Map<String, Object>> CACHE = new HashMap<>();

    @SuppressWarnings("unchecked")
    static <T extends UniquelyNamed> T get(Class<T> type, String name, Supplier<T> ifNotPresent) {
        return (T) CACHE.computeIfAbsent(type, ___ -> new HashMap<>()).computeIfAbsent(name, n -> {
            var value = ifNotPresent.get();
            if (!value.nameMatches(n)) {
                throw new IllegalArgumentException("Supplied object has invalid name " + value + ", expected " + n);
            }
            return value;
        });
    }

    default void cache() {
        CACHE.computeIfAbsent(this.getClass(), ___ -> new HashMap<>()).put(this.toString(), this);
    }

    default boolean isUnique() {
        var value = CACHE.computeIfAbsent(this.getClass(), ___ -> new HashMap<>()).get(this.toString());
        if (value == null) {
            return true;
        }
        return this == value;
    }

    default boolean nameMatches(String name) {
        return this.toString().equals(name);
    }

    String toString();
}
