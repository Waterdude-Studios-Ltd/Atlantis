package de.waterdu.atlantis.mod.object;

import java.util.concurrent.CompletableFuture;

public record ModFuture(ModObjectKey type, CompletableFuture<?> future) implements ModType {

    @SuppressWarnings("unchecked")
    public <T> CompletableFuture<T> dereference() {
        return (CompletableFuture<T>) this.future;
    }

    @SuppressWarnings("unchecked")
    public <T> CompletableFuture<T> dereference(Class<T> clazz) {
        return (CompletableFuture<T>) this.future;
    }
}
