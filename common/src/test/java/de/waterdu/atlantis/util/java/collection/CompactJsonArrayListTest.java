package de.waterdu.atlantis.util.java.collection;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CompactJsonArrayListTest {

    @Test
    void behavesLikeAnArrayList() {
        var empty = new CompactJsonArrayList<String>();
        var sized = new CompactJsonArrayList<String>(4);
        var copy = new CompactJsonArrayList<>(List.of("a", "b"));
        empty.add("x");
        sized.add("y");

        assertThat(empty).containsExactly("x");
        assertThat(sized).containsExactly("y");
        assertThat(copy).containsExactly("a", "b");
    }
}
