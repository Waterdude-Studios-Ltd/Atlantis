package de.waterdu.atlantis.util.java;

import java.util.Random;

/**
 * Gets random values from one shared {@link Random}.
 */
public final class RandomUtils {

    private static final Random RANDOM = new Random();

    private RandomUtils() {
        ExceptionUtils.throwCannotInstantiateClassException();
    }

    /**
     * @return the shared random number generator
     */
    public static Random getRandom() {
        return RandomUtils.RANDOM;
    }

    /**
     * @return a value from {@code min} to {@code max}, both inclusive. The bounds can be in either order.
     */
    public static byte getBetween(byte min, byte max) {
        if (min > max) {
            var temp = min;
            min = max;
            max = temp;
        }
        return (byte) (RandomUtils.getRandom().nextInt(max - min + 1) + min);
    }

    /**
     * @return a value from {@code min} to {@code max}, both inclusive. The bounds can be in either order.
     */
    public static short getBetween(short min, short max) {
        if (min > max) {
            var temp = min;
            min = max;
            max = temp;
        }
        return (short) (RandomUtils.getRandom().nextInt(max - min + 1) + min);
    }

    /**
     * @return a value from {@code min} to {@code max}, both inclusive. The bounds can be in either order.
     */
    public static int getBetween(int min, int max) {
        if (min > max) {
            var temp = min;
            min = max;
            max = temp;
        }
        return RandomUtils.getRandom().nextInt(max - min + 1) + min;
    }

    /**
     * @return a value from {@code min} to {@code max}, both inclusive. The bounds can be in either order.
     */
    public static long getBetween(long min, long max) {
        if (min > max) {
            var temp = min;
            min = max;
            max = temp;
        }
        return RandomUtils.getRandom().nextLong(max - min + 1L) + min;
    }

    /**
     * @return a value from the lower bound, inclusive, to the upper bound, exclusive. The bounds can be in either
     *     order.
     */
    public static float getBetween(float min, float max) {
        if (min > max) {
            var temp = min;
            min = max;
            max = temp;
        }
        return RandomUtils.getRandom().nextFloat() * (max - min) + min;
    }

    /**
     * @return a value from the lower bound, inclusive, to the upper bound, exclusive. The bounds can be in either
     *     order.
     */
    public static double getBetween(double min, double max) {
        if (min > max) {
            var temp = min;
            min = max;
            max = temp;
        }
        return RandomUtils.getRandom().nextDouble() * (max - min) + min;
    }
}
