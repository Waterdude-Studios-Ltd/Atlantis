package de.waterdu.atlantis.util.config;

import com.google.gson.GsonBuilder;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class KeyTest {

    @Test
    void parsesAndCompares() {
        Key<String> parsed = Key.of("atlantis:thing");
        Key<String> split = Key.of("atlantis", "thing");

        assertThat(parsed).isEqualTo(split).hasSameHashCodeAs(split);
        assertThat(parsed.key()).isEqualTo(ResourceLocation.fromNamespaceAndPath("atlantis", "thing"));
        assertThat(parsed.toString()).isEqualTo("atlantis:thing");
        assertThat(Key.<String>of("thing")).isEqualTo(Key.of("minecraft", "thing"));
        assertThat(parsed).isNotEqualTo(Key.of("atlantis", "other")).isNotEqualTo("atlantis:thing");
    }

    @Test
    void getUsesTheRegisteredGetter() {
        Key.registerGetter(GetterValue.class, key -> new GetterValue(key.key().getPath()));

        assertThat(Key.<GetterValue>of("atlantis", "value").get(GetterValue.class)).isEqualTo(new GetterValue("value"));
    }

    @Test
    void getWithoutGetterThrows() {
        assertThatThrownBy(() -> Key.<Unregistered>of("atlantis", "value").get(Unregistered.class))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining(Unregistered.class.getName());
    }

    @Test
    void cacheCallsTheGetterOnceUntilUncached() {
        var calls = new AtomicInteger();
        Key.registerGetter(CachedValue.class, key -> new CachedValue(calls.incrementAndGet()));
        Key<CachedValue> key = Key.of("atlantis", "cached");

        var first = key.cache(CachedValue.class);
        var second = key.cache(CachedValue.class);

        assertThat(second).isSameAs(first);
        assertThat(calls).hasValue(1);
        assertThat(key.uncache(CachedValue.class)).isSameAs(first);
        assertThat(key.cache(CachedValue.class)).isEqualTo(new CachedValue(2));
    }

    @Test
    void ofWithValuePrimesTheCache() {
        var value = new PrimedValue("primed");
        var key = Key.of("atlantis", "primed", value);

        assertThat(key.cache(PrimedValue.class)).isSameAs(value);
        assertThat(Key.of("atlantis:primed_string", new PrimedValue("other")).cache(PrimedValue.class)).isEqualTo(new PrimedValue("other"));
    }

    @Test
    void extensions() {
        Key.registerGetter(ExtendedValue.class, key -> new ExtendedValue(key.toString()));
        Key.registerExtension(ExtendedValue.class, Key.Extension::new);
        Key<ExtendedValue> key = Key.of("atlantis", "extended");

        Key.Extension<ExtendedValue> extension = key.ext(ExtendedValue.class);

        assertThat(extension.key()).isSameAs(key);
        assertThat(extension.get(ExtendedValue.class)).isEqualTo(new ExtendedValue("atlantis:extended"));
        var cached = extension.cache(ExtendedValue.class);
        assertThat(extension.cache(ExtendedValue.class)).isSameAs(cached);
        assertThat(extension.uncache(ExtendedValue.class)).isSameAs(cached);
        assertThatThrownBy(() -> Key.<Unregistered>of("atlantis", "value").ext(Unregistered.class))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void typeAdapterRoundTrips() {
        var gson = new GsonBuilder().registerTypeHierarchyAdapter(Key.class, new Key.TypeAdapter()).create();

        assertThat(gson.toJson(Key.of("atlantis", "thing"))).isEqualTo("\"atlantis:thing\"");
        assertThat(gson.fromJson("\"atlantis:thing\"", Key.class)).isEqualTo(Key.of("atlantis", "thing"));
    }

    private record GetterValue(String value) {}

    private record CachedValue(int id) {}

    private record PrimedValue(String value) {}

    private record ExtendedValue(String value) {}

    private record Unregistered() {}
}
