package de.waterdu.atlantis.util.java.interfaces;

import java.util.Objects;

/**
 * Accepts four arguments and returns no result.
 *
 * @param <T> the type of the first argument
 * @param <U> the type of the second argument
 * @param <V> the type of the third argument
 * @param <W> the type of the fourth argument
 */
@FunctionalInterface
public interface QuadConsumer<T, U, V, W> {

    /**
     * Does the operation on the arguments.
     *
     * @param t the first argument
     * @param u the second argument
     * @param v the third argument
     * @param w the fourth argument
     */
    void accept(T t, U u, V v, W w);

    /**
     * Creates a consumer that calls this consumer, then {@code after}, with the same arguments.
     *
     * @param after the consumer to call second
     * @return the combined consumer
     * @throws NullPointerException if {@code after} is {@code null}
     */
    default QuadConsumer<T, U, V, W> andThen(QuadConsumer<? super T, ? super U, ? super V, ? super W> after) {
        Objects.requireNonNull(after);
        return (t, u, v, w) -> {
            this.accept(t, u, v, w);
            after.accept(t, u, v, w);
        };
    }
}
