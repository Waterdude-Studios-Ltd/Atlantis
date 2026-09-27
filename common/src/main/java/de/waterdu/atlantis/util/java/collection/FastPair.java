package de.waterdu.atlantis.util.java.collection;

import org.jspecify.annotations.NonNull;

import java.util.Objects;

public record FastPair<A, B>(A a, B b) {

    public static <A, B> FastPair<A, B> of(A a, B b) {
        return new FastPair<>(a, b);
    }

    public static <T> UnaryPair<T> unary(T a, T b) {
        return new UnaryPair<>(a, b);
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
        if (!(obj instanceof FastPair<?, ?>(Object a1, Object b1))) {
            return false;
        }
        return Objects.equals(this.a, a1) && Objects.equals(this.b, b1);
    }

    @Override
    public @NonNull String toString() {
        return "FastPair{[" + (this.a == null ? "null" : this.a.toString()) + "],[" + (this.b == null ? "null" : this.b.toString()) + "]}";
    }
}
