package de.waterdu.atlantis.util.java;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class OptionalCacheTest {

    @Test
    void missingKeysAreEmpty() {
        OptionalCache<String> cache = OptionalCache.create();

        assertThat(cache.get("missing")).isEmpty();
        assertThat(cache).isEmpty();
    }

    @Test
    void cacheStoresKeysAsValues() {
        OptionalCache<String> cache = OptionalCache.create(4);

        assertThat(cache.cache("a")).isNull();
        assertThat(cache.cache("a")).contains("a");
        assertThat(cache.get("a")).contains("a");
    }

    @Test
    void cacheOptional() {
        OptionalCache<String> cache = OptionalCache.create();

        assertThat(cache.cache(Optional.<String>empty())).isEmpty();
        assertThat(cache).isEmpty();
        assertThat(cache.cache(Optional.of("b"))).isEmpty();
        assertThat(cache.cache(Optional.of("b"))).contains("b");
        assertThat(cache.get("b")).contains("b");
    }

    @Test
    void cacheAll() {
        OptionalCache<Integer> cache = OptionalCache.create();

        cache.cacheAll(1, 2, 3);

        assertThat(cache).hasSize(3);
        assertThat(cache.get(2)).contains(2);
    }
}
