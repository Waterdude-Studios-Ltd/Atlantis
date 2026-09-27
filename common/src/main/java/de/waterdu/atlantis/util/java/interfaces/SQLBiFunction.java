package de.waterdu.atlantis.util.java.interfaces;

import java.sql.SQLException;
import java.util.Objects;
import java.util.function.Function;

@FunctionalInterface
public interface SQLBiFunction<A, B, R> {

    R apply(A a, B b) throws SQLException;

    default <V> SQLBiFunction<A, B, V> andThen(Function<? super R, ? extends V> after) {
        Objects.requireNonNull(after);
        return (A a, B b) -> after.apply(this.apply(a, b));
    }
}

