package de.waterdu.atlantis.util.java.collection;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.waterdu.atlantis.util.java.interfaces.Weighted;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class WeightedSetTest {

    private Locale previous;

    @BeforeEach
    void useRootLocale() {
        this.previous = Locale.getDefault();
        Locale.setDefault(Locale.ROOT);
    }

    @AfterEach
    void restoreLocale() {
        Locale.setDefault(this.previous);
    }

    record Item(String name, double weight) implements Weighted {}

    @Test
    void emptySetReturnsNull() {
        var set = WeightedSet.<String>newWeightedSet();

        assertThat(set.isEmpty()).isTrue();
        assertThat(set.isEffectivelyEmpty()).isTrue();
        assertThat(set.get(new Random(1))).isNull();
    }

    @Test
    void addRejectsDuplicatesAndNonPositiveWeights() {
        var set = new WeightedSet<String>();

        assertThat(set.add(2, "a")).isTrue();
        assertThat(set.add(3, "a")).isFalse();
        assertThat(set.add(0, "b")).isFalse();
        assertThat(set.add(-1, "c")).isFalse();
        assertThat(set).hasSize(1);
        assertThat(set.weight()).isEqualTo(2);
    }

    @Test
    void containsAndContainsAll() {
        var set = new WeightedSet<String>();
        set.add("a");
        set.add("b");

        assertThat(set.contains("a")).isTrue();
        assertThat(set.contains("z")).isFalse();
        assertThat(set.containsAll(List.of("a", "b"))).isTrue();
        assertThat(set.containsAll(List.of("a", "z"))).isFalse();
    }

    @Test
    void defaultAndWeightedElementWeights() {
        var set = new WeightedSet<Object>();
        set.setDefaultElementWeight(4);
        set.add("plain");
        set.add(new Item("heavy", 6));

        assertThat(set.getWeight("plain")).isEqualTo(4);
        assertThat(set.getWeight(new Item("heavy", 6))).isEqualTo(6);
        assertThat(set.getWeight("missing")).isZero();
        assertThat(set.getChance("plain")).isEqualTo(0.4);
    }

    @Test
    void selectionFollowsTheWeights() {
        var set = new WeightedSet<String>();
        set.add(1, "rare");
        set.add(3, "common");
        var random = new Random(1234);
        var counts = new HashMap<String, Integer>();
        var samples = 40_000;

        set.getAndDo(random, samples, value -> counts.merge(value, 1, Integer::sum));

        assertThat(counts.get("rare") / (double) samples).isCloseTo(0.25, within(0.02));
        assertThat(counts.get("common") / (double) samples).isCloseTo(0.75, within(0.02));
    }

    @Test
    void removeRebuildsWeights() {
        var set = new WeightedSet<String>();
        set.add(1, "a");
        set.add(2, "b");
        set.add(3, "c");

        assertThat(set.remove("b")).isTrue();
        assertThat(set.remove("b")).isFalse();

        assertThat(set.weight()).isEqualTo(4);
        assertThat(set).containsExactlyInAnyOrder("a", "c");
        var random = new Random(5);
        for (var i = 0; i < 1000; i++) {
            assertThat(set.get(random)).isIn("a", "c");
        }
    }

    @Test
    void bulkOperations() {
        var set = new WeightedSet<String>();

        assertThat(set.addAll(List.of("a", "b", "c"))).isTrue();
        assertThat(set.addAll(List.of("a"))).isFalse();
        assertThat(set.retainAll(List.of("a", "b"))).isTrue();
        assertThat(set).containsExactlyInAnyOrder("a", "b");
        assertThat(set.removeAll(List.of("a", "z"))).isTrue();
        assertThat(set).containsExactly("b");
        assertThat(set.toArray()).containsExactly("b");
        assertThat(set.toArray(new String[0])).containsExactly("b");
        assertThat(set.stream()).containsExactly("b");
        set.clear();
        assertThat(set.isEmpty()).isTrue();
        assertThat(set.weight()).isZero();
    }

    @Test
    void chanceString() {
        var set = new WeightedSet<String>();
        set.add(1, "a");
        set.add(1, "b");
        set.add(1, "c");
        set.add(1, "d");

        assertThat(set.getChanceString("a", 2)).isEqualTo("25");
        assertThat(set.getChanceString("a", 0)).isEqualTo("25");
        set.add(4, "e");
        assertThat(set.getChanceString("e", 2)).isEqualTo("50");
        set.add(1, "f");
        assertThat(set.getChanceString("a", 2)).isEqualTo("11.11");
        assertThat(set.getChanceString("a", 1)).isEqualTo("11.1");
    }

    @Test
    void typeAdapterRoundTripsWithWeights() {
        var adapter = new WeightedSet.TypeAdapter<>(Item.class);
        Gson gson = new GsonBuilder().registerTypeAdapter(WeightedSet.TypeAdapter.typeOf(Item.class), adapter).create();

        WeightedSet<Item> set = gson.fromJson("[{\"name\":\"a\",\"weight\":1},{\"name\":\"b\",\"weight\":3}]", WeightedSet.TypeAdapter.typeOf(Item.class));

        assertThat(set.weight()).isEqualTo(4);
        assertThat(set.getChance(new Item("b", 3))).isEqualTo(0.75);
        assertThat(gson.toJson(set, WeightedSet.TypeAdapter.typeOf(Item.class))).contains("\"name\":\"a\"").contains("\"name\":\"b\"");
    }

    @Test
    void typeAdapterUsesUnitWeightForPlainTypes() {
        Gson gson = new GsonBuilder().registerTypeAdapter(WeightedSet.TypeAdapter.typeOf(String.class), new WeightedSet.TypeAdapter<>(String.class)).create();

        WeightedSet<String> set = gson.fromJson("[\"a\",\"b\"]", WeightedSet.TypeAdapter.typeOf(String.class));

        assertThat(set.getWeight("a")).isEqualTo(1);
        assertThat(set.weight()).isEqualTo(2);
    }
}
