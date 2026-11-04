package de.waterdu.atlantis.comm;

import de.waterdu.atlantis.AtlantisLogger;

import org.jspecify.annotations.Nullable;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

/**
 * A request to the Mojang API. {@link #run()} sends the request and completes the {@link #future()}.
 *
 * @param <T> the type of the result
 */
public interface Query<T> extends Runnable {

    /**
     * @return the future that completes with the result of this query
     */
    CompletableFuture<T> future();

    /**
     * Submits the query to run asynchronously. Each call sends a new request.
     */
    default CompletableFuture<T> query() {
        return MojangAPI.query(this);
    }

    /**
     * @return the result. Null if the query is not done, failed or has no result.
     */
    default @Nullable T result() {
        try {
            return this.future().isDone() ? this.future().get() : null;
        } catch (ExecutionException | InterruptedException e) {
            AtlantisLogger.error("Error while executing query", e);
            return null;
        }
    }

    /**
     * Runs the action when the future completes. The action also runs when the future fails.
     *
     * @param action receives the result, or {@code null} when the future fails
     */
    default void whenComplete(Consumer<T> action) {
        this.future().whenComplete((result, err) -> action.accept(result));
    }

    /**
     * Checks if the future of this query is complete.
     *
     * @return {@code true} if the future is complete
     */
    default boolean isDone() {
        return this.future().isDone();
    }

    /**
     * Checks if the future of this query is cancelled.
     *
     * @return {@code true} if the future is cancelled
     */
    default boolean isCancelled() {
        return this.future().isCancelled();
    }
}
