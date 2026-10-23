package de.waterdu.atlantis.util.java.collection;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.TreeSet;

import static org.assertj.core.api.Assertions.assertThat;

class CollectionUtilsTest {

    @Test
    void mapIntoAList() {
        assertThat(CollectionUtils.map(List.of("a", "bb"), String::length)).containsExactly(1, 2);
    }

    @Test
    void mapIntoAGivenCollection() {
        var dest = new TreeSet<Integer>();

        var result = CollectionUtils.map(List.of("ccc", "a", "a"), dest, String::length);

        assertThat(result).isSameAs(dest).containsExactly(1, 3);
    }
}
