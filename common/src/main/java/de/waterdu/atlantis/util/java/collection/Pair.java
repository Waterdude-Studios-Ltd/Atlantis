package de.waterdu.atlantis.util.java.collection;

import java.util.Objects;

public class Pair<A, B> {

    protected A a;
    protected B b;

    protected Pair(A a, B b) {
        this.a = a;
        this.b = b;
    }

    public static <A, B> Pair<A, B> of(A a, B b) {
        return new Pair<>(a, b);
    }

    public static <T> UnaryPair<T> unary(T a, T b) {
        return new UnaryPair<>(a, b);
    }

    public Pair<A, B> mutable() {
        return Pair.of(this.a, this.b);
    }

    public Pair<A, B> immutable() {
        return Immutable.of(this.a, this.b);
    }

    public A getA() {
        return this.a;
    }

    public B getB() {
        return this.b;
    }

    public void setA(A a) {
        this.a = a;
    }

    public void setB(B b) {
        this.b = b;
    }

    @Override
    public int hashCode() {
        return ((this.a == null) ? 0 : this.a.hashCode()) ^ ((this.b == null) ? 0 : this.b.hashCode());
    }

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

    @Override
    public String toString() {
        return "Pair{[" + (this.a == null ? "null" : this.a.toString()) + "],[" + (this.b == null ? "null" : this.b.toString()) + "]}";
    }

    public static class Immutable<A, B> extends Pair<A, B> {

        protected Immutable(A a, B b) {
            super(a, b);
        }

        public static <A, B> Immutable<A, B> of(A a, B b) {
            return new Immutable<>(a, b);
        }

        public static <T> UnaryPair.Immutable<T> unary(T a, T b) {
            return new UnaryPair.Immutable<>(a, b);
        }

        @Override
        public void setA(A a) {
            throw new UnsupportedOperationException("Cannot modify an immutable Pair");
        }

        @Override
        public void setB(B b) {
            throw new UnsupportedOperationException("Cannot modify an immutable Pair");
        }

        @Override
        public String toString() {
            return "Pair.Immutable{[" + (this.getA() == null ? "null" : this.getA().toString()) + "],[" + (this.getB() == null ? "null" : this.getB().toString()) + "]}";
        }
    }
}
