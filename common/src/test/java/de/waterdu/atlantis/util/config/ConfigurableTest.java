package de.waterdu.atlantis.util.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonPrimitive;
import com.google.gson.reflect.TypeToken;
import de.waterdu.atlantis.test.AtlantisTestEnvironment;
import de.waterdu.atlantis.util.java.interfaces.ResettableLazy;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.awt.Color;
import java.lang.reflect.Type;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

class ConfigurableTest {

    private static Type configurableOf(Class<?> type) {
        return TypeToken.getParameterized(Configurable.class, type).getType();
    }

    @Test
    void valueIsReturnedAsIs() {
        assertThat(Configurable.of("value").get()).isEqualTo("value");
    }

    @Test
    void supplierIsLazyAndCachedUntilReset() {
        var calls = new AtomicInteger();
        var configurable = Configurable.of(calls::incrementAndGet);

        assertThat(calls).hasValue(0);
        assertThat(configurable.get()).isEqualTo(1);
        assertThat(configurable.get()).isEqualTo(1);
        configurable.reset();
        assertThat(configurable.get()).isEqualTo(2);
    }

    @Test
    void wrapsAnExistingLazy() {
        var lazy = ResettableLazy.of(() -> "lazy");

        assertThat(Configurable.of(lazy).value()).isSameAs(lazy);
        assertThat(Configurable.of(lazy).get()).isEqualTo("lazy");
    }

    @Test
    void lists() {
        Supplier<String> supplier = () -> "b";

        assertThat(Configurable.ofList("a", "b")).extracting(Configurable::get).containsExactly("a", "b");
        assertThat(Configurable.ofList(supplier, supplier)).extracting(Configurable::get).containsExactly("b", "b");
        assertThat(Configurable.ofList(ResettableLazy.of(() -> "c"))).extracting(Configurable::get).containsExactly("c");
    }

    @Test
    void contains() {
        var list = Configurable.ofList("a", "b");

        assertThat(Configurable.contains("a", list)).isTrue();
        assertThat(Configurable.contains("c", list)).isFalse();
        assertThat(Configurable.contains("A", String::equalsIgnoreCase, list)).isTrue();
        assertThat(Configurable.contains("x", "b", String::equals, list)).isTrue();
        assertThat(Configurable.contains("x", "y", String::equals, list)).isFalse();

        var array = list.toArray(Configurable[]::new);
        @SuppressWarnings("unchecked")
        Configurable<String>[] typed = array;
        assertThat(Configurable.contains("b", typed)).isTrue();
        assertThat(Configurable.contains("B", String::equalsIgnoreCase, typed)).isTrue();
        assertThat(Configurable.contains("x", "a", String::equals, typed)).isTrue();
        assertThat(Configurable.contains("x", "y", String::equals, typed)).isFalse();
    }

    @Test
    void defaultContextDelegatesToGson() {
        var builder = new GsonBuilder();
        Configurable.registerContext(builder, Integer.class);
        var gson = builder.create();

        Configurable<Integer> parsed = gson.fromJson("5", configurableOf(Integer.class));

        assertThat(parsed.get()).isEqualTo(5);
        assertThat(gson.toJson(Configurable.of(7), configurableOf(Integer.class))).isEqualTo("7");
    }

    @Test
    void deserializationIsDeferredUntilFirstGet() {
        var calls = new AtomicInteger();
        var builder = new GsonBuilder();
        Configurable.registerContext(builder, String.class, (json, context) -> {
            calls.incrementAndGet();
            return json.getAsString().toUpperCase();
        }, (src, context) -> new JsonPrimitive(src.toLowerCase()));
        var gson = builder.create();

        Configurable<String> parsed = gson.fromJson("\"value\"", configurableOf(String.class));

        assertThat(calls).hasValue(0);
        assertThat(parsed.get()).isEqualTo("VALUE");
        assertThat(parsed.get()).isEqualTo("VALUE");
        assertThat(calls).hasValue(1);
        assertThat(gson.toJson(parsed, configurableOf(String.class))).isEqualTo("\"value\"");
    }

    @ExtendWith(AtlantisTestEnvironment.class)
    @Nested
    class DefaultContexts {

        private Gson gson() {
            var builder = new GsonBuilder();
            Configurable.registerDefaultContexts(builder);
            return builder.create();
        }

        @Test
        void colour() {
            var gson = this.gson();

            Configurable<Color> opaque = gson.fromJson("{\"red\":1,\"green\":2,\"blue\":3}", configurableOf(Color.class));
            Configurable<Color> translucent = gson.fromJson("{\"red\":1,\"green\":2,\"blue\":3,\"alpha\":4}", configurableOf(Color.class));

            assertThat(opaque.get()).isEqualTo(new Color(1, 2, 3, 255));
            assertThat(translucent.get()).isEqualTo(new Color(1, 2, 3, 4));
            assertThat(gson.toJson(translucent, configurableOf(Color.class))).isEqualTo("{\"red\":1,\"green\":2,\"blue\":3,\"alpha\":4}");
        }

        @Test
        void pattern() {
            var gson = this.gson();

            Configurable<Pattern> plain = gson.fromJson("\"a+b\"", configurableOf(Pattern.class));
            Configurable<Pattern> flagged = gson.fromJson("{\"pattern\":\"a+b\",\"flags\":2}", configurableOf(Pattern.class));

            assertThat(plain.get().matcher("aab").matches()).isTrue();
            assertThat(flagged.get().flags()).isEqualTo(Pattern.CASE_INSENSITIVE);
            assertThat(flagged.get().matcher("AAB").matches()).isTrue();
            assertThat(gson.toJson(plain, configurableOf(Pattern.class))).isEqualTo("\"a+b\"");
            assertThat(gson.toJson(flagged, configurableOf(Pattern.class))).isEqualTo("{\"pattern\":\"a+b\",\"flags\":2}");
        }
    }
}
