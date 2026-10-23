package de.waterdu.atlantis.util.java.collection;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class UnaryMapTest {

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

    @Test
    void getMissingKeyIsNull() {
        assertThat(UnaryMap.<String>create().get("missing")).isNull();
    }

    @Test
    void containsKeyAndValue() {
        UnaryMap<String> map = UnaryMap.create(2);
        map.put("k", "v");

        assertThat(map.containsKey("k")).isTrue();
        assertThat(map.containsKey("v")).isFalse();
        assertThat(map.containsValue("v")).isTrue();
        assertThat(map.containsValue("k")).isFalse();
    }

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

    @Test
    void createFromString() {
        var map = UnaryMap.createFromString("shiny:true  level:50 untradeable");

        assertThat(map).containsOnly(Map.entry("shiny", "true"), Map.entry("level", "50"), Map.entry("untradeable", ""));
        assertThat(map.keySet()).containsExactly("shiny", "level", "untradeable");
    }

    @Test
    void createFromStringWithCustomDelimiters() {
        var map = UnaryMap.createFromString("a=1;b=2", ";", "=");

        assertThat(map.get("a")).isEqualTo("1");
        assertThat(map.get("b")).isEqualTo("2");
    }

    @Test
    void createFromStringJoinsExtraValueSegmentsWithSpaces() {
        assertThat(UnaryMap.createFromString("time:12:30").get("time")).isEqualTo("12 30");
    }
}
