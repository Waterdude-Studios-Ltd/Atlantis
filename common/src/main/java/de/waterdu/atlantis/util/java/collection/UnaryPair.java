package de.waterdu.atlantis.util.java.collection;

public class UnaryPair<T> extends Pair<T, T> {

    protected UnaryPair(T a, T b) {
        super(a, b);
    }

    public UnaryPair<T> mutable() {
        return Pair.unary(this.a, this.b);
    }

    public UnaryPair<T> immutable() {
        return Pair.Immutable.unary(this.a, this.b);
    }

    @Override
    public String toString() {
        return "UnaryPair{[" + (this.a == null ? "null" : this.a.toString()) + "],[" + (this.b == null ? "null" : this.b.toString()) + "]}";
    }

    public static class Immutable<T> extends UnaryPair<T> {

        protected Immutable(T a, T b) {
            super(a, b);
        }

        @Override
        public String toString() {
            return "UnaryPair.Immutable{[" + (this.getA() == null ? "null" : this.getA().toString()) + "],[" + (this.getB() == null ? "null" : this.getB().toString()) + "]}";
        }
    }
}
