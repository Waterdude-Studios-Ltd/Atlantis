package de.waterdu.atlantis.mod.object;

import java.util.concurrent.CompletableFuture;

/**
 * Holds a future of an object from another mod, together with the key of its type.
 *
 * @param type the key of the type of the object
 * @param future the future of the object
 */
public record ModFuture(ModObjectKey type, CompletableFuture<?> future) implements ModType {

    /**
     * Casts the future to the type that the caller expects. This method does not check the type.
     *
     * @param <T> the type of the object
     * @return the future
     */
    @SuppressWarnings("unchecked")
    public <T> CompletableFuture<T> dereference() {
        return (CompletableFuture<T>) this.future;
    }

    /**
     * @param clazz only sets {@code T}. This method does not check the type, so a wrong class fails later, at the first
     *     use of the result.
     */
    @SuppressWarnings("unchecked")
    public <T> CompletableFuture<T> dereference(Class<T> clazz) {
        return (CompletableFuture<T>) this.future;
    }
}
