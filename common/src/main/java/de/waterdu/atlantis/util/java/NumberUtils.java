package de.waterdu.atlantis.util.java;

public final class NumberUtils {

    private NumberUtils() {
        ExceptionUtils.throwCannotInstantiateClassException();
    }

    public static double boundDouble(double d) {
        return isFinite(d) ? d : (d > 0 ? 1.7976931348623157E308D : -1.7976931348623157E308D);
    }

    public static float boundFloat(double d) {
        return boundFloat((float) d);
    }

    public static float boundFloat(float f) {
        return isFinite(f) ? f : (f > 0 ? 3.4028235E38F : -3.4028235E38F);
    }

    public static boolean isFinite(double d) {
        return Math.abs(d) <= 1.7976931348623157E308D;
    }

    public static boolean isFinite(float f) {
        return Math.abs(f) <= 3.4028235E38F;
    }

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

    public static byte setBit(byte flags, int mask, boolean value) {
        if (value) {
            flags = (byte) (flags | mask);
        } else {
            flags = (byte) (flags & ~mask);
        }
        return flags;
    }

    public static boolean getBit(byte flags, int mask) {
        return (flags & mask) != 0;
    }

    public static byte flipBit(byte flags, int mask) {
        return (byte) (flags ^ mask);
    }

    public static int setBit(int flags, int mask, boolean value) {
        if (value) {
            flags = flags | mask;
        } else {
            flags = flags & ~mask;
        }
        return flags;
    }

    public static boolean getBit(int flags, int mask) {
        return (flags & mask) != 0;
    }

    public static int flipBit(int flags, int mask) {
        return flags ^ mask;
    }
}
