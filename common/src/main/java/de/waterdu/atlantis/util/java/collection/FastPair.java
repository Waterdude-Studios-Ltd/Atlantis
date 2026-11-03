package de.waterdu.atlantis.util.java.collection;

import org.jspecify.annotations.NonNull;

import java.util.Objects;

/**
 * Holds two values that can be {@code null}.
 *
 * @param a the first value
 * @param b the second value
 * @param <A> the type of the first value
 * @param <B> the type of the second value
 */
public record FastPair<A, B>(A a, B b) {

    /**
     * Creates a pair.
     *
     * @param <A> the type of the first value
     * @param <B> the type of the second value
     * @param a the first value
     * @param b the second value
     * @return the new pair
     */
    public static <A, B> FastPair<A, B> of(A a, B b) {
        return new FastPair<>(a, b);
    }

    /**
     * Creates a {@link UnaryPair}, a pair with two values of the same type.
     *
     * @param <T> the type of the values
     * @param a the first value
     * @param b the second value
     * @return the new pair
     */
    public static <T> UnaryPair<T> unary(T a, T b) {
        return new UnaryPair<>(a, b);
    }

    /**
     * Gets the hash codes of both values combined with XOR. A {@code null} value counts as 0.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return ((this.a == null) ? 0 : this.a.hashCode()) ^ ((this.b == null) ? 0 : this.b.hashCode());
    }

    /**
     * Checks if the object is a {@link FastPair} with equal values in the same order.
     *
     * @param obj the object to compare
     * @return {@code true} if both values are equal
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof FastPair<?, ?>(Object a1, Object b1))) {
            return false;
        }
        return Objects.equals(this.a, a1) && Objects.equals(this.b, b1);
    }

    /**
     * @return both values in the form {@code FastPair{[a],[b]}}
     */
    @Override
    public @NonNull String toString() {
        return "FastPair{[" + (this.a == null ? "null" : this.a.toString()) + "],[" + (this.b == null ? "null" : this.b.toString()) + "]}";
    }
}
