package de.waterdu.atlantis.util.java.interfaces;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Has a unique name per class, given by {@link #toString()}. Instances are cached by their class and name.
 */
public interface UniquelyNamed {

    Map<Class<?>, Map<String, Object>> CACHE = new HashMap<>();

    /**
     * Gets the cached instance with the name. If there is none, creates one with {@code ifNotPresent} and caches it.
     * Throws {@link IllegalArgumentException} if the name of the new instance does not match.
     */
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

    /**
     * Caches this instance by its runtime class and its name. It replaces a cached instance with the same name.
     */
    default void cache() {
        CACHE.computeIfAbsent(this.getClass(), ___ -> new HashMap<>()).put(this.toString(), this);
    }

    /**
     * @return {@code true} if no other instance with the same class and name is cached
     */
    default boolean isUnique() {
        var value = CACHE.computeIfAbsent(this.getClass(), ___ -> new HashMap<>()).get(this.toString());
        if (value == null) {
            return true;
        }
        return this == value;
    }

    /**
     * Checks if the name of this instance is the name. The check is case-sensitive.
     *
     * @param name the name to compare with
     * @return {@code true} if {@link #toString()} is equal to the name
     */
    default boolean nameMatches(String name) {
        return this.toString().equals(name);
    }

    /**
     * @return the unique name of this instance
     */
    String toString();
}
