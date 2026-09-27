package de.waterdu.atlantis.util.java.interfaces;

import java.util.Objects;

@FunctionalInterface
public interface EqualityTest<T> {

    static <T> EqualityTest<T> objectsEqual() {
        return Objects::equals;
    }

    boolean equals(T a, T b);
}