package de.waterdu.atlantis.util.java.collection;

/**
 * Holds two values of the same type.
 *
 * @param <T> the type of the values
 */
public class UnaryPair<T> extends Pair<T, T> {

    /**
     * Creates a pair with the two values.
     *
     * @param a the first value
     * @param b the second value
     */
    protected UnaryPair(T a, T b) {
        super(a, b);
    }

    /**
     * @return a new mutable copy, also if this pair is mutable
     */
    public UnaryPair<T> mutable() {
        return Pair.unary(this.a, this.b);
    }

    /**
     * @return a new immutable copy, also if this pair is immutable
     */
    public UnaryPair<T> immutable() {
        return Pair.Immutable.unary(this.a, this.b);
    }

    /**
     * @return the two values in the format {@code UnaryPair{[a],[b]}}
     */
    @Override
    public String toString() {
        return "UnaryPair{[" + (this.a == null ? "null" : this.a.toString()) + "],[" + (this.b == null ? "null" : this.b.toString()) + "]}";
    }

    /**
     * Holds two values of the same type that cannot change.
     *
     * @param <T> the type of the values
     */
    public static class Immutable<T> extends UnaryPair<T> {

        /**
         * Creates an immutable pair with the two values.
         *
         * @param a the first value
         * @param b the second value
         */
        protected Immutable(T a, T b) {
            super(a, b);
        }

        /**
         * Always throws, because this pair is immutable.
         *
         * @param a ignored
         * @throws UnsupportedOperationException always
         */
        @Override
        public void setA(T a) {
            throw new UnsupportedOperationException("Cannot modify an immutable Pair");
        }

        /**
         * Always throws, because this pair is immutable.
         *
         * @param b ignored
         * @throws UnsupportedOperationException always
         */
        @Override
        public void setB(T b) {
            throw new UnsupportedOperationException("Cannot modify an immutable Pair");
        }

        /**
         * @return the two values in the format {@code UnaryPair.Immutable{[a],[b]}}
         */
        @Override
        public String toString() {
            return "UnaryPair.Immutable{[" + (this.getA() == null ? "null" : this.getA().toString()) + "],[" + (this.getB() == null ? "null" : this.getB().toString()) + "]}";
        }
    }
}
