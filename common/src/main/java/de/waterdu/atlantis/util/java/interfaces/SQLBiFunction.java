package de.waterdu.atlantis.util.java.interfaces;

import java.sql.SQLException;
import java.util.Objects;
import java.util.function.Function;

/**
 * Accepts two arguments and returns a result. It can throw an {@link SQLException}.
 *
 * @param <A> the type of the first argument
 * @param <B> the type of the second argument
 * @param <R> the type of the result
 */
@FunctionalInterface
public interface SQLBiFunction<A, B, R> {

    /**
     * Applies the function to the arguments.
     *
     * @param a the first argument
     * @param b the second argument
     * @return the result
     * @throws SQLException if a database operation fails
     */
    R apply(A a, B b) throws SQLException;

    /**
     * Creates a function that applies this function, then {@code after} to its result.
     *
     * @param after the function to apply to the result
     * @param <V> the type of the result of {@code after}
     * @return the combined function
     * @throws NullPointerException if {@code after} is {@code null}
     */
    default <V> SQLBiFunction<A, B, V> andThen(Function<? super R, ? extends V> after) {
        Objects.requireNonNull(after);
        return (A a, B b) -> after.apply(this.apply(a, b));
    }
}

