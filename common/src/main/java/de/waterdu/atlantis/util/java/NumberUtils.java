package de.waterdu.atlantis.util.java;

/**
 * Bounds numbers, checks if they are finite and changes bit flags.
 */
public final class NumberUtils {

    private NumberUtils() {
        ExceptionUtils.throwCannotInstantiateClassException();
    }

    /**
     * Replaces an infinite value with the largest finite value of the same sign. NaN becomes {@code -Double.MAX_VALUE}.
     */
    public static double boundDouble(double d) {
        return isFinite(d) ? d : (d > 0 ? 1.7976931348623157E308D : -1.7976931348623157E308D);
    }

    /**
     * Casts the value to a float and bounds it with {@link #boundFloat(float)}.
     *
     * @param d the value to bound
     * @return the bounded value
     */
    public static float boundFloat(double d) {
        return boundFloat((float) d);
    }

    /**
     * Replaces an infinite value with the largest finite value of the same sign. NaN becomes {@code -Float.MAX_VALUE}.
     */
    public static float boundFloat(float f) {
        return isFinite(f) ? f : (f > 0 ? 3.4028235E38F : -3.4028235E38F);
    }

    /**
     * Checks if the value is finite.
     *
     * @param d the value to check
     * @return {@code false} if the value is infinite or NaN
     */
    public static boolean isFinite(double d) {
        return Math.abs(d) <= 1.7976931348623157E308D;
    }

    /**
     * Checks if the value is finite.
     *
     * @param f the value to check
     * @return {@code false} if the value is infinite or NaN
     */
    public static boolean isFinite(float f) {
        return Math.abs(f) <= 3.4028235E38F;
    }

    /**
     * @return the values from {@code start} to {@code end}, both inclusive. The values decrease if {@code start} is
     *     greater than {@code end}.
     */
    public static int[] arrayOfRange(int start, int end) {
        if (start == end) {
            return new int[]{start};
        }
        var array = new int[Math.abs(start - end) + 1];
        var change = start < end ? 1 : -1;
        var index = 0;
        for (var value = start; value != (end + change); value += change) {
            array[index++] = value;
        }
        return array;
    }

    /**
     * Sets or clears the bits of the mask in the flags.
     *
     * @param flags the flags to change
     * @param mask the bits to set or clear
     * @param value {@code true} to set the bits, {@code false} to clear them
     * @return the changed flags
     */
    public static byte setBit(byte flags, int mask, boolean value) {
        if (value) {
            flags = (byte) (flags | mask);
        } else {
            flags = (byte) (flags & ~mask);
        }
        return flags;
    }

    /**
     * Checks if any bit of the mask is set in the flags.
     *
     * @param flags the flags to check
     * @param mask the bits to check
     * @return {@code true} if at least one bit of the mask is set
     */
    public static boolean getBit(byte flags, int mask) {
        return (flags & mask) != 0;
    }

    /**
     * Inverts the bits of the mask in the flags.
     *
     * @param flags the flags to change
     * @param mask the bits to invert
     * @return the changed flags
     */
    public static byte flipBit(byte flags, int mask) {
        return (byte) (flags ^ mask);
    }

    /**
     * Sets or clears the bits of the mask in the flags.
     *
     * @param flags the flags to change
     * @param mask the bits to set or clear
     * @param value {@code true} to set the bits, {@code false} to clear them
     * @return the changed flags
     */
    public static int setBit(int flags, int mask, boolean value) {
        if (value) {
            flags = flags | mask;
        } else {
            flags = flags & ~mask;
        }
        return flags;
    }

    /**
     * Checks if any bit of the mask is set in the flags.
     *
     * @param flags the flags to check
     * @param mask the bits to check
     * @return {@code true} if at least one bit of the mask is set
     */
    public static boolean getBit(int flags, int mask) {
        return (flags & mask) != 0;
    }

    /**
     * Inverts the bits of the mask in the flags.
     *
     * @param flags the flags to change
     * @param mask the bits to invert
     * @return the changed flags
     */
    public static int flipBit(int flags, int mask) {
        return flags ^ mask;
    }
}
