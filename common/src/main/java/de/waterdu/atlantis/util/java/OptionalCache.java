package de.waterdu.atlantis.util.java;

import java.util.HashMap;
import java.util.Optional;

/**
 * Maps each cached key to an {@link Optional} of itself. {@link #get(Object)} returns an empty {@link Optional}
 * for a key that is not cached.
 *
 * @param <K> the type of the keys
 */
public class OptionalCache<K> extends HashMap<K, Optional<K>> {

    /**
     * Creates an empty cache.
     *
     * @param <K> the type of the keys
     * @return the new cache
     */
    public static <K> OptionalCache<K> create() {
        return new OptionalCache<>();
    }

    /**
     * Creates an empty cache with an initial capacity.
     *
     * @param <K> the type of the keys
     * @param defaultCapacity the initial capacity of the map
     * @return the new cache
     */
    public static <K> OptionalCache<K> create(int defaultCapacity) {
        return new OptionalCache<>(defaultCapacity);
    }

    /**
     * Creates an empty cache.
     */
    private OptionalCache() {
        super();
    }

    /**
     * Creates an empty cache with an initial capacity.
     *
     * @param defaultCapacity the initial capacity of the map
     */
    private OptionalCache(int defaultCapacity) {
        super(defaultCapacity);
    }

    /**
     * Gets the cached value for the key.
     *
     * @param key the key to look up
     * @return the cached value, or an empty {@link Optional} if the key is not cached
     */
    @Override
    public Optional<K> get(Object key) {
        return super.getOrDefault(key, Optional.empty());
    }

    /**
     * @return the previous value, or {@code null} if there was none
     */
    public Optional<K> cache(K key) {
        return this.put(key, Optional.of(key));
    }

    /**
     * Does nothing if the value is empty.
     *
     * @return the previous value, or an empty {@link Optional} if there was none
     */
    public Optional<K> cache(Optional<K> value) {
        if (value.isPresent()) {
            var old = this.put(value.get(), value);
            return old == null ? Optional.empty() : old;
        }
        return Optional.empty();
    }

    /**
     * Caches each key with {@link #cache(Object)}.
     *
     * @param keys the keys to cache
     */
    @SafeVarargs
    public final void cacheAll(K... keys) {
        for (var key : keys) {
            this.cache(key);
        }
    }
}
