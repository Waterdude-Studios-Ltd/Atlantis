package de.waterdu.atlantis.util.java.collection;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ArrayUtilsTest {

    @Test
    void coerceTailCopiesTheEnd() {
        var dest = ArrayUtils.coerceTail(new String[]{"a", "b", "c", "d"}, new String[2]);

        assertThat(dest).containsExactly("c", "d");
    }

    @Test
    void coerceTailWithLargerSourceThanDestination() {
        assertThat(ArrayUtils.coerceTail(new Integer[]{1, 2}, new Integer[2])).containsExactly(1, 2);
    }

    @Test
    void mapProducesATypedArray() {
        Integer[] lengths = ArrayUtils.map(Integer.class, String::length, "a", "bb", "ccc");

        assertThat(lengths).containsExactly(1, 2, 3);
        assertThat(lengths.getClass()).isEqualTo(Integer[].class);
    }

    @Test
    void mapNotNullDropsNulls() {
        String[] result = ArrayUtils.mapNotNull(String.class, s -> s.isBlank() ? null : s.trim(), " a ", "  ", "b");

        assertThat(result).containsExactly("a", "b");
        assertThat(result.getClass()).isEqualTo(String[].class);
    }

    @Test
    void arrayOf() {
        assertThat(ArrayUtils.arrayOf(String.class, 3, i -> "#" + i)).containsExactly("#0", "#1", "#2");
        assertThat(ArrayUtils.arrayOf(String.class, 0, i -> "x")).isEmpty();
    }

    @Test
    void expandAppends() {
        String[] expanded = ArrayUtils.expand(new String[]{"a", "b"}, "c", "d");

        assertThat(expanded).containsExactly("a", "b", "c", "d");
        assertThat(expanded.getClass()).isEqualTo(String[].class);
        assertThat(ArrayUtils.expand(new String[]{"a"})).containsExactly("a");
    }
}
