package de.waterdu.atlantis.comm;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

public interface Query<T> extends Runnable {

    CompletableFuture<T> future();

    default CompletableFuture<T> query() {
        return MojangAPI.query(this);
    }

    default Optional<T> result() {
        try {
            return this.future().isDone() ? Optional.ofNullable(this.future().get()) : Optional.empty();
        } catch (ExecutionException | InterruptedException e) {
            return Optional.empty();
        }
    }

    default void whenComplete(Consumer<T> action) {
        this.future().whenComplete((result, err) -> action.accept(result));
    }

    default boolean isDone() {
        return this.future().isDone();
    }

    default boolean isCancelled() {
        return this.future().isCancelled();
    }
}
