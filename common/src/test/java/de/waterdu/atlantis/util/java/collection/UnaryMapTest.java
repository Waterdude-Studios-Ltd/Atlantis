package de.waterdu.atlantis.util.java.collection;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests {@link UnaryMap}.
 */
class UnaryMapTest {

    /**
     * Checks that put, get, pair and remove return the previous or current values.
     */
    @Test
    void putGetRemove() {
        UnaryMap<String> map = UnaryMap.create();

        assertThat(map.put("a", "1")).isNull();
        assertThat(map.put("a", "2")).isEqualTo("1");
        assertThat(map.get("a")).isEqualTo("2");
        assertThat(map.pair("a")).isEqualTo(Pair.unary("a", "2"));
        assertThat(map.remove("a")).isEqualTo("2");
        assertThat(map.remove("a")).isNull();
        assertThat(map).isEmpty();
    }

    /**
     * Checks that {@link UnaryMap#get} returns {@code null} for a missing key.
     */
    @Test
    void getMissingKeyIsNull() {
        assertThat(UnaryMap.<String>create().get("missing")).isNull();
    }

    /**
     * Checks that {@link UnaryMap#containsKey} looks only at keys and {@link UnaryMap#containsValue} only at values.
     */
    @Test
    void containsKeyAndValue() {
        UnaryMap<String> map = UnaryMap.create(2);
        map.put("k", "v");

        assertThat(map.containsKey("k")).isTrue();
        assertThat(map.containsKey("v")).isFalse();
        assertThat(map.containsValue("v")).isTrue();
        assertThat(map.containsValue("k")).isFalse();
    }

    /**
     * Checks that a pair and a map can be put, that the views keep insertion order, and that
     * {@link UnaryMap#clear()} empties the map.
     */
    @Test
    void putPairAndPutAll() {
        UnaryMap<String> map = UnaryMap.create();
        map.put(Pair.unary("a", "1"));
        map.putAll(Map.of("b", "2"));

        assertThat(map.keySet()).containsExactly("a", "b");
        assertThat(map.values()).containsExactly("1", "2");
        assertThat(map.pairs()).containsExactly(Pair.unary("a", "1"), Pair.unary("b", "2"));
        assertThat(map.entrySet()).containsExactlyInAnyOrder(Map.entry("a", "1"), Map.entry("b", "2"));
        assertThat(map.size()).isEqualTo(2);
        map.clear();
        assertThat(map.isEmpty()).isTrue();
    }

    /**
     * Checks that a unary map is equal to, and has the same hash code as, other maps with the same entries.
     */
    @Test
    void equalsAndHashCodeFollowTheMapContract() {
        UnaryMap<String> a = UnaryMap.create();
        a.put("k", "v");
        UnaryMap<String> b = UnaryMap.create();
        b.put("k", "v");
        var plain = new HashMap<String, String>();
        plain.put("k", "v");

        assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);
        assertThat(a.equals(plain)).isTrue();
        assertThat(a.hashCode()).isEqualTo(plain.hashCode());
        b.put("k", "other");
        assertThat(a).isNotEqualTo(b);
        assertThat(a.equals("k")).isFalse();
    }

    /**
     * Checks that {@link UnaryMap#createFromString} reads space-separated {@code key:value} entries in order, and
     * gives an empty value to a key without one.
     */
    @Test
    void createFromString() {
        var map = UnaryMap.createFromString("shiny:true  level:50 untradeable");

        assertThat(map).containsOnly(Map.entry("shiny", "true"), Map.entry("level", "50"), Map.entry("untradeable", ""));
        assertThat(map.keySet()).containsExactly("shiny", "level", "untradeable");
    }

    /**
     * Checks that {@link UnaryMap#createFromString} accepts custom entry and key delimiters.
     */
    @Test
    void createFromStringWithCustomDelimiters() {
        var map = UnaryMap.createFromString("a=1;b=2", ";", "=");

        assertThat(map.get("a")).isEqualTo("1");
        assertThat(map.get("b")).isEqualTo("2");
    }

    /**
     * Checks that {@link UnaryMap#createFromString} joins the extra parts of a value with spaces.
     */
    @Test
    void createFromStringJoinsExtraValueSegmentsWithSpaces() {
        assertThat(UnaryMap.createFromString("time:12:30").get("time")).isEqualTo("12 30");
    }
}
