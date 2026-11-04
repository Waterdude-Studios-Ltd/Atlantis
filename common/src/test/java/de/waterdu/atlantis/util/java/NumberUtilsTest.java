package de.waterdu.atlantis.util.java;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests the bound, range and bit methods of {@link NumberUtils}.
 */
class NumberUtilsTest {

    /**
     * Checks that infinite and too large values are bound to the largest finite value, and other values do not
     * change.
     */
    @Test
    void boundsInfiniteValues() {
        assertThat(NumberUtils.boundDouble(Double.POSITIVE_INFINITY)).isEqualTo(Double.MAX_VALUE);
        assertThat(NumberUtils.boundDouble(Double.NEGATIVE_INFINITY)).isEqualTo(-Double.MAX_VALUE);
        assertThat(NumberUtils.boundDouble(12.5)).isEqualTo(12.5);
        assertThat(NumberUtils.boundFloat(Float.POSITIVE_INFINITY)).isEqualTo(Float.MAX_VALUE);
        assertThat(NumberUtils.boundFloat(Float.NEGATIVE_INFINITY)).isEqualTo(-Float.MAX_VALUE);
        assertThat(NumberUtils.boundFloat(1e300)).isEqualTo(Float.MAX_VALUE);
        assertThat(NumberUtils.boundFloat(2.5)).isEqualTo(2.5F);
    }

    /**
     * Checks that NaN is not finite and that it is bound to the negative largest double.
     */
    @Test
    void nanIsNotFiniteAndBoundsToNegativeMax() {
        assertThat(NumberUtils.isFinite(Double.NaN)).isFalse();
        assertThat(NumberUtils.isFinite(Float.NaN)).isFalse();
        assertThat(NumberUtils.boundDouble(Double.NaN)).isEqualTo(-Double.MAX_VALUE);
    }

    /**
     * Checks that the largest values are finite and that infinite values are not.
     */
    @Test
    void isFinite() {
        assertThat(NumberUtils.isFinite(Double.MAX_VALUE)).isTrue();
        assertThat(NumberUtils.isFinite(Double.POSITIVE_INFINITY)).isFalse();
        assertThat(NumberUtils.isFinite(Float.MAX_VALUE)).isTrue();
        assertThat(NumberUtils.isFinite(Float.NEGATIVE_INFINITY)).isFalse();
    }

    /**
     * Checks that {@code arrayOfRange} includes both ends and counts down when the start is larger.
     */
    @Test
    void arrayOfRangeIsInclusiveInBothDirections() {
        assertThat(NumberUtils.arrayOfRange(1, 4)).containsExactly(1, 2, 3, 4);
        assertThat(NumberUtils.arrayOfRange(4, 1)).containsExactly(4, 3, 2, 1);
        assertThat(NumberUtils.arrayOfRange(-1, 1)).containsExactly(-1, 0, 1);
        assertThat(NumberUtils.arrayOfRange(7, 7)).containsExactly(7);
    }

    /**
     * Checks that the set, get and flip bit methods work on bytes, including the highest bit.
     */
    @Test
    void byteBits() {
        byte flags = 0;
        flags = NumberUtils.setBit(flags, 0b100, true);
        assertThat(NumberUtils.getBit(flags, 0b100)).isTrue();
        assertThat(NumberUtils.getBit(flags, 0b1)).isFalse();
        flags = NumberUtils.flipBit(flags, 0b1);
        assertThat(flags).isEqualTo((byte) 0b101);
        flags = NumberUtils.setBit(flags, 0b100, false);
        assertThat(flags).isEqualTo((byte) 0b1);
        assertThat(NumberUtils.setBit((byte) 0, 0x80, true)).isEqualTo((byte) 0x80);
    }

    /**
     * Checks that the set, get and flip bit methods work on ints, including the sign bit.
     */
    @Test
    void intBits() {
        var flags = NumberUtils.setBit(0, 1 << 20, true);
        assertThat(NumberUtils.getBit(flags, 1 << 20)).isTrue();
        assertThat(NumberUtils.flipBit(flags, 1 << 20)).isZero();
        assertThat(NumberUtils.setBit(0b11, 0b10, false)).isEqualTo(0b1);
        assertThat(NumberUtils.setBit(0, Integer.MIN_VALUE, true)).isEqualTo(Integer.MIN_VALUE);
    }
}
