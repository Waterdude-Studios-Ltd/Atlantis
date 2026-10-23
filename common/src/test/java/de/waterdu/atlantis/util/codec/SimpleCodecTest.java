package de.waterdu.atlantis.util.codec;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializer;
import de.waterdu.atlantis.util.config.Key;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class SimpleCodecTest {

    private static final Key<Sample> KEY = Key.of("atlantis", "sample");

    private static Gson gson(SimpleCodec<Sample> codec) {
        return new GsonBuilder()
                .registerTypeAdapter(Sample.class, (JsonSerializer<Sample>) (src, type, context) -> {
                    var object = new JsonObject();
                    codec.serialize(src, object, context);
                    return object;
                })
                .registerTypeAdapter(Sample.class, (JsonDeserializer<Sample>) (json, type, context) -> codec.deserialize(json.getAsJsonObject(), context))
                .create();
    }

    private static Sample sample() {
        var sample = new Sample();
        sample.number = 4;
        sample.name = "name";
        sample.numbers = new int[]{1, 2};
        sample.list = List.of("a", "b");
        sample.set = Set.of(3);
        sample.map = Map.of("k", 5L);
        sample.ignored = "ignored";
        return sample;
    }

    @Test
    void fromTypeFieldsRoundTripsEveryNonTransientField() {
        var gson = gson(SimpleCodec.of(KEY, Sample.class).fromTypeFields());

        var json = gson.toJsonTree(sample()).getAsJsonObject();
        var copy = gson.fromJson(json, Sample.class);

        assertThat(json.keySet()).containsExactlyInAnyOrder("number", "name", "numbers", "list", "set", "map");
        assertThat(copy.number).isEqualTo(4);
        assertThat(copy.name).isEqualTo("name");
        assertThat(copy.numbers).containsExactly(1, 2);
        assertThat(copy.list).containsExactly("a", "b");
        assertThat(copy.set).containsExactly(3);
        assertThat(copy.map).containsEntry("k", 5L);
        assertThat(copy.ignored).isNull();
    }

    @Test
    void explicitFieldsRoundTrip() {
        var codec = SimpleCodec.of(KEY, Sample.class)
                .withField("number", int.class)
                .withField("name", String.class)
                .withArray("numbers", int.class)
                .withList("list", String.class)
                .withSet("set", Integer.class)
                .withMap("map", String.class, Long.class);
        var gson = gson(codec);

        var copy = gson.fromJson(gson.toJson(sample()), Sample.class);

        assertThat(copy.number).isEqualTo(4);
        assertThat(copy.name).isEqualTo("name");
        assertThat(copy.numbers).containsExactly(1, 2);
        assertThat(copy.list).containsExactly("a", "b");
        assertThat(copy.set).containsExactly(3);
        assertThat(copy.map).containsEntry("k", 5L);
    }

    @Test
    void mismatchedOrMissingFieldsAreSkipped() {
        var codec = SimpleCodec.of(KEY, Sample.class)
                .withField("number", String.class)
                .withField("missing", String.class)
                .withField("name", String.class);

        var json = gson(codec).toJsonTree(sample()).getAsJsonObject();

        assertThat(json.keySet()).containsExactly("name");
    }

    @Test
    void absentJsonFieldsKeepTheirDefaults() {
        var copy = gson(SimpleCodec.of(KEY, Sample.class).fromTypeFields()).fromJson("{\"name\":\"only\"}", Sample.class);

        assertThat(copy.name).isEqualTo("only");
        assertThat(copy.number).isEqualTo(Sample.DEFAULT_NUMBER);
    }

    @Test
    void supplierAndInstanceFactories() {
        var supplied = SimpleCodec.of(KEY, Sample.class, () -> {
            var sample = new Sample();
            sample.name = "supplied";
            return sample;
        }).withField("number", int.class);

        assertThat(gson(supplied).fromJson("{\"number\":9}", Sample.class).name).isEqualTo("supplied");
        assertThat(SimpleCodec.of(KEY, new Sample()).id()).isEqualTo(KEY);
    }

    @Test
    void typeWithoutNoArgConstructorDeserializesToNull() {
        var codec = SimpleCodec.of(Key.<NoDefaultConstructor>of("atlantis", "no_default"), NoDefaultConstructor.class);

        assertThat(codec.deserialize(new JsonObject(), null)).isNull();
    }

    @Test
    void poolLooksUpById() {
        var pool = new SimpleCodecPool<Sample>();
        var codec = pool.add(SimpleCodec.of(KEY, Sample.class));

        assertThat(pool.get(KEY)).containsSame(codec);
        assertThat(pool.get(Key.of("atlantis", "other"))).isEmpty();
    }

    public static final class Sample {

        static final int DEFAULT_NUMBER = 7;

        private int number = DEFAULT_NUMBER;
        private String name;
        private int[] numbers;
        private List<String> list;
        private Set<Integer> set;
        private Map<String, Long> map;
        private transient String ignored;

        public Sample() {}
    }

    public record NoDefaultConstructor(String value) {}
}
