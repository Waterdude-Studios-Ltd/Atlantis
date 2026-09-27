package de.waterdu.atlantis.util.java;

import java.util.Random;

public final class RandomUtils {

    private static final Random RANDOM = new Random();

    private RandomUtils() {
        ExceptionUtils.throwCannotInstantiateClassException();
    }

    public static Random getRandom() {
        return RandomUtils.RANDOM;
    }

    public static byte getBetween(byte min, byte max) {
        if (min > max) {
            var temp = min;
            min = max;
            max = temp;
        }
        return (byte) (RandomUtils.getRandom().nextInt(max - min + 1) + min);
    }

    public static short getBetween(short min, short max) {
        if (min > max) {
            var temp = min;
            min = max;
            max = temp;
        }
        return (short) (RandomUtils.getRandom().nextInt(max - min + 1) + min);
    }

    public static int getBetween(int min, int max) {
        if (min > max) {
            var temp = min;
            min = max;
            max = temp;
        }
        return RandomUtils.getRandom().nextInt(max - min + 1) + min;
    }

    public static long getBetween(long min, long max) {
        if (min > max) {
            var temp = min;
            min = max;
            max = temp;
        }
        return RandomUtils.getRandom().nextInt((int) (max - min + 1L)) + min;
    }

    public static float getBetween(float min, float max) {
        if (min > max) {
            var temp = min;
            min = max;
            max = temp;
        }
        return RandomUtils.getRandom().nextFloat() * (max - min + 1F) + min;
    }

    public static double getBetween(double min, double max) {
        if (min > max) {
            var temp = min;
            min = max;
            max = temp;
        }
        return RandomUtils.getRandom().nextDouble() * (max - min + 1D) + min;
    }
}
