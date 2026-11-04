package de.waterdu.atlantis.util.java.collection;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests {@link ArrayUtils}.
 */
class ArrayUtilsTest {

    /**
     * Checks that {@link ArrayUtils#coerceTail} copies the last elements of a longer source into the destination.
     */
    @Test
    void coerceTailCopiesTheEnd() {
        var dest = ArrayUtils.coerceTail(new String[]{"a", "b", "c", "d"}, new String[2]);

        assertThat(dest).containsExactly("c", "d");
    }

    /**
     * Checks that {@link ArrayUtils#coerceTail} copies all elements when the source and destination have the same
     * length.
     */
    @Test
    void coerceTailWithLargerSourceThanDestination() {
        assertThat(ArrayUtils.coerceTail(new Integer[]{1, 2}, new Integer[2])).containsExactly(1, 2);
    }

    /**
     * Checks that {@link ArrayUtils#map} maps every element into an array of the given component type.
     */
    @Test
    void mapProducesATypedArray() {
        Integer[] lengths = ArrayUtils.map(Integer.class, String::length, "a", "bb", "ccc");

        assertThat(lengths).containsExactly(1, 2, 3);
        assertThat(lengths.getClass()).isEqualTo(Integer[].class);
    }

    /**
     * Checks that {@link ArrayUtils#mapNotNull} leaves out {@code null} results and keeps the component type.
     */
    @Test
    void mapNotNullDropsNulls() {
        String[] result = ArrayUtils.mapNotNull(String.class, s -> s.isBlank() ? null : s.trim(), " a ", "  ", "b");

        assertThat(result).containsExactly("a", "b");
        assertThat(result.getClass()).isEqualTo(String[].class);
    }

    /**
     * Checks that {@link ArrayUtils#arrayOf} fills an array of the given size from the index, and gives an empty
     * array for size zero.
     */
    @Test
    void arrayOf() {
        assertThat(ArrayUtils.arrayOf(String.class, 3, i -> "#" + i)).containsExactly("#0", "#1", "#2");
        assertThat(ArrayUtils.arrayOf(String.class, 0, i -> "x")).isEmpty();
    }

    /**
     * Checks that {@link ArrayUtils#expand} adds the elements to the end of the array and keeps the component type.
     */
    @Test
    void expandAppends() {
        String[] expanded = ArrayUtils.expand(new String[]{"a", "b"}, "c", "d");

        assertThat(expanded).containsExactly("a", "b", "c", "d");
        assertThat(expanded.getClass()).isEqualTo(String[].class);
        assertThat(ArrayUtils.expand(new String[]{"a"})).containsExactly("a");
    }
}
