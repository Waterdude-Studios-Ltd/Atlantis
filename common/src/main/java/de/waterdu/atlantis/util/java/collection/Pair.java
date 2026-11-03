package de.waterdu.atlantis.util.java.collection;

import java.util.Objects;

/**
 * Holds two values of any type. The values can change, unless the pair is {@link Immutable}.
 *
 * @param <A> the type of the first value
 * @param <B> the type of the second value
 */
public class Pair<A, B> {

    protected A a;
    protected B b;

    /**
     * Creates a pair with the two values.
     *
     * @param a the first value
     * @param b the second value
     */
    protected Pair(A a, B b) {
        this.a = a;
        this.b = b;
    }

    /**
     * Creates a mutable pair.
     *
     * @param a the first value
     * @param b the second value
     * @param <A> the type of the first value
     * @param <B> the type of the second value
     * @return the new pair
     */
    public static <A, B> Pair<A, B> of(A a, B b) {
        return new Pair<>(a, b);
    }

    /**
     * Creates a mutable pair with two values of the same type.
     *
     * @param a the first value
     * @param b the second value
     * @param <T> the type of the values
     * @return the new pair
     */
    public static <T> UnaryPair<T> unary(T a, T b) {
        return new UnaryPair<>(a, b);
    }

    /**
     * @return a new mutable copy, also if this pair is mutable
     */
    public Pair<A, B> mutable() {
        return Pair.of(this.a, this.b);
    }

    /**
     * @return a new immutable copy, also if this pair is immutable
     */
    public Pair<A, B> immutable() {
        return Immutable.of(this.a, this.b);
    }

    /**
     * @return the first value
     */
    public A getA() {
        return this.a;
    }

    /**
     * @return the second value
     */
    public B getB() {
        return this.b;
    }

    /**
     * Sets the first value.
     *
     * @param a the new first value
     */
    public void setA(A a) {
        this.a = a;
    }

    /**
     * Sets the second value.
     *
     * @param b the new second value
     */
    public void setB(B b) {
        this.b = b;
    }

    /**
     * Gets the hash code. It is the XOR of the hash codes of the two values, with 0 for a {@code null} value.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return ((this.a == null) ? 0 : this.a.hashCode()) ^ ((this.b == null) ? 0 : this.b.hashCode());
    }

    /**
     * Checks if the object is a {@link Pair} with equal values. The mutability of the pairs is not compared.
     *
     * @param obj the object to compare with
     * @return {@code true} if both values are equal
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Pair<?, ?> other)) {
            return false;
        }
        return Objects.equals(this.a, other.a) && Objects.equals(this.b, other.b);
    }

    /**
     * @return the two values in the format {@code Pair{[a],[b]}}
     */
    @Override
    public String toString() {
        return "Pair{[" + (this.a == null ? "null" : this.a.toString()) + "],[" + (this.b == null ? "null" : this.b.toString()) + "]}";
    }

    /**
     * Holds two values that cannot change.
     *
     * @param <A> the type of the first value
     * @param <B> the type of the second value
     */
    public static class Immutable<A, B> extends Pair<A, B> {

        /**
         * Creates an immutable pair with the two values.
         *
         * @param a the first value
         * @param b the second value
         */
        protected Immutable(A a, B b) {
            super(a, b);
        }

        /**
         * Creates an immutable pair.
         *
         * @param a the first value
         * @param b the second value
         * @param <A> the type of the first value
         * @param <B> the type of the second value
         * @return the new pair
         */
        public static <A, B> Immutable<A, B> of(A a, B b) {
            return new Immutable<>(a, b);
        }

        /**
         * Creates an immutable pair with two values of the same type.
         *
         * @param a the first value
         * @param b the second value
         * @param <T> the type of the values
         * @return the new pair
         */
        public static <T> UnaryPair.Immutable<T> unary(T a, T b) {
            return new UnaryPair.Immutable<>(a, b);
        }

        /**
         * Always throws, because this pair is immutable.
         *
         * @param a ignored
         * @throws UnsupportedOperationException always
         */
        @Override
        public void setA(A a) {
            throw new UnsupportedOperationException("Cannot modify an immutable Pair");
        }

        /**
         * Always throws, because this pair is immutable.
         *
         * @param b ignored
         * @throws UnsupportedOperationException always
         */
        @Override
        public void setB(B b) {
            throw new UnsupportedOperationException("Cannot modify an immutable Pair");
        }

        /**
         * @return the two values in the format {@code Pair.Immutable{[a],[b]}}
         */
        @Override
        public String toString() {
            return "Pair.Immutable{[" + (this.getA() == null ? "null" : this.getA().toString()) + "],[" + (this.getB() == null ? "null" : this.getB().toString()) + "]}";
        }
    }
}
