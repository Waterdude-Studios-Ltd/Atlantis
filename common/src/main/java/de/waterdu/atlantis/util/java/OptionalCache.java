package de.waterdu.atlantis.util.java;

import java.util.HashMap;
import java.util.Optional;

public class OptionalCache<K> extends HashMap<K, Optional<K>> {

    public static <K> OptionalCache<K> create() {
        return new OptionalCache<>();
    }

    public static <K> OptionalCache<K> create(int defaultCapacity) {
        return new OptionalCache<>(defaultCapacity);
    }

    private OptionalCache() {
        super();
    }

    private OptionalCache(int defaultCapacity) {
        super(defaultCapacity);
    }

    @Override
    public Optional<K> get(Object key) {
        return super.getOrDefault(key, Optional.empty());
    }

    public Optional<K> cache(K key) {
        return this.put(key, Optional.of(key));
    }

    public Optional<K> cache(Optional<K> value) {
        if (value.isPresent()) {
            var old = this.put(value.get(), value);
            return old == null ? Optional.empty() : old;
        }
        return Optional.empty();
    }

    @SafeVarargs
    public final void cacheAll(K... keys) {
        for (var key : keys) {
            this.cache(key);
        }
    }
}
