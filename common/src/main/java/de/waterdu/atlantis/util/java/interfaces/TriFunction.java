package de.waterdu.atlantis.util.java.interfaces;

import java.util.Objects;
import java.util.function.Function;

/**
 * Accepts three arguments and returns a result.
 *
 * @param <A> the type of the first argument
 * @param <B> the type of the second argument
 * @param <C> the type of the third argument
 * @param <R> the type of the result
 */
@FunctionalInterface
public interface TriFunction<A, B, C, R> {

    /**
     * Applies the function to the arguments.
     *
     * @param a the first argument
     * @param b the second argument
     * @param c the third argument
     * @return the result
     */
    R apply(A a, B b, C c);

    /**
     * Creates a function that applies this function, then {@code after} to its result.
     *
     * @param after the function to apply to the result
     * @param <V> the type of the result of {@code after}
     * @return the combined function
     * @throws NullPointerException if {@code after} is {@code null}
     */
    default <V> TriFunction<A, B, C, V> andThen(Function<? super R, ? extends V> after) {
        Objects.requireNonNull(after);
        return (A a, B b, C c) -> after.apply(this.apply(a, b, c));
    }
}

