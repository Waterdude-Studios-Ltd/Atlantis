package de.waterdu.atlantis.mod.wrapper;

import de.waterdu.atlantis.mod.object.ModFuture;
import de.waterdu.atlantis.mod.object.ModObject;

import java.util.concurrent.CompletableFuture;

/**
 * Wraps an object of a mod, so that common code can use it without linking against the mod.
 *
 * @param <T> the type of the wrapped object
 */
public abstract class ModWrapper<T> {

    private final ModObject object;
    private final ModFuture future;

    /**
     * Creates a wrapper for an object that exists now. The future of the wrapper is already complete.
     *
     * @param object the object to wrap
     */
    protected ModWrapper(ModObject object) {
        this.object = object;
        this.future = new ModFuture(object.type(), CompletableFuture.completedFuture(object.reference()));
    }

    /**
     * Creates a wrapper for an object that a future gives. If the future is not complete yet, the wrapped object is
     * {@code null}.
     *
     * @param future the future of the object to wrap
     */
    protected ModWrapper(ModFuture future) {
        this.object = new ModObject(future.type(), future.future().getNow(null));
        this.future = future;
    }

    /**
     * @return the wrapped object. If the wrapper was made from a future that was not complete, this is {@code null}.
     */
    public T dereference() {
        return this.object.dereference();
    }

    /**
     * @return the future that completes with the wrapped object
     */
    public CompletableFuture<T> future() {
        return this.future.dereference();
    }
}
