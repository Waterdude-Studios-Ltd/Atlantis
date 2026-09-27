package de.waterdu.atlantis.mod.wrapper;

import de.waterdu.atlantis.mod.object.ModFuture;
import de.waterdu.atlantis.mod.object.ModObject;

import java.util.concurrent.CompletableFuture;

public abstract class ModWrapper<T> {

    private final ModObject object;
    private final ModFuture future;

    protected ModWrapper(ModObject object) {
        this.object = object;
        this.future = new ModFuture(object.type(), CompletableFuture.completedFuture(object.reference()));
    }

    protected ModWrapper(ModFuture future) {
        this.object = new ModObject(future.type(), future.future().getNow(null));
        this.future = future;
    }

    public T dereference() {
        return this.object.dereference();
    }

    public CompletableFuture<T> future() {
        return this.future.dereference();
    }
}
