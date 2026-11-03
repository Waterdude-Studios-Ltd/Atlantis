package de.waterdu.atlantis.util.java.interfaces;

import java.util.Objects;

/**
 * Accepts three arguments and returns no result.
 *
 * @param <T> the type of the first argument
 * @param <U> the type of the second argument
 * @param <V> the type of the third argument
 */
@FunctionalInterface
public interface TriConsumer<T, U, V> {

    /**
     * Does the operation on the arguments.
     *
     * @param t the first argument
     * @param u the second argument
     * @param v the third argument
     */
    void accept(T t, U u, V v);

    /**
     * Creates a consumer that calls this consumer, then {@code after}, with the same arguments.
     *
     * @param after the consumer to call second
     * @return the combined consumer
     * @throws NullPointerException if {@code after} is {@code null}
     */
    default TriConsumer<T, U, V> andThen(TriConsumer<? super T, ? super U, ? super V> after) {
        Objects.requireNonNull(after);
        return (t, u, v) -> {
            this.accept(t, u, v);
            after.accept(t, u, v);
        };
    }
}
