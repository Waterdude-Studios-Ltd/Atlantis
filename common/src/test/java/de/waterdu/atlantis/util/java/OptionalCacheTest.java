package de.waterdu.atlantis.util.java;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests {@link OptionalCache}.
 */
class OptionalCacheTest {

    /**
     * Checks that a key that is not cached gives an empty optional.
     */
    @Test
    void missingKeysAreEmpty() {
        OptionalCache<String> cache = OptionalCache.create();

        assertThat(cache.get("missing")).isEmpty();
        assertThat(cache).isEmpty();
    }

    /**
     * Checks that {@code cache} stores the key as its own value, and returns {@code null} when the key was not
     * cached before.
     */
    @Test
    void cacheStoresKeysAsValues() {
        OptionalCache<String> cache = OptionalCache.create(4);

        assertThat(cache.cache("a")).isNull();
        assertThat(cache.cache("a")).contains("a");
        assertThat(cache.get("a")).contains("a");
    }

    /**
     * Checks that an empty optional caches nothing, and that a present optional caches its value.
     */
    @Test
    void cacheOptional() {
        OptionalCache<String> cache = OptionalCache.create();

        assertThat(cache.cache(Optional.<String>empty())).isEmpty();
        assertThat(cache).isEmpty();
        assertThat(cache.cache(Optional.of("b"))).isEmpty();
        assertThat(cache.cache(Optional.of("b"))).contains("b");
        assertThat(cache.get("b")).contains("b");
    }

    /**
     * Checks that {@code cacheAll} caches every given key.
     */
    @Test
    void cacheAll() {
        OptionalCache<Integer> cache = OptionalCache.create();

        cache.cacheAll(1, 2, 3);

        assertThat(cache).hasSize(3);
        assertThat(cache.get(2)).contains(2);
    }
}
