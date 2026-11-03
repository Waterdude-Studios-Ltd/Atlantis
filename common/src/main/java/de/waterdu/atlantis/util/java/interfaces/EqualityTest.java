package de.waterdu.atlantis.util.java.interfaces;

import java.util.Objects;

/**
 * Checks if two values are equal by a custom rule.
 *
 * @param <T> the type of the values
 */
@FunctionalInterface
public interface EqualityTest<T> {

    /**
     * Gets a test that uses {@link Objects#equals(Object, Object)}.
     *
     * @param <T> the type of the values
     * @return the test
     */
    static <T> EqualityTest<T> objectsEqual() {
        return Objects::equals;
    }

    /**
     * Checks if the two values are equal.
     *
     * @param a the first value
     * @param b the second value
     * @return {@code true} if the values are equal
     */
    boolean equals(T a, T b);
}