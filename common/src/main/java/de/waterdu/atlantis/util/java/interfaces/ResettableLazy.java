package de.waterdu.atlantis.util.java.interfaces;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * Gets a value from a supplier on first use and keeps it until {@link #reset()}.
 *
 * @param <T> the type of the value
 */
public sealed interface ResettableLazy<T> extends Supplier<T> permits ResettableLazy.Fast, ResettableLazy.Concurrent {

    /**
     * Creates a resettable lazy value that is not thread-safe.
     *
     * @param supplier gives the value
     * @param <T> the type of the value
     * @return the new lazy value
     */
    static <T> ResettableLazy<T> of(@NonNull Supplier<T> supplier) {
        return new Fast<>(supplier);
    }

    /**
     * Creates a thread-safe resettable lazy value. Only one thread at a time calls the supplier.
     *
     * @param supplier gives the value
     * @param <T> the type of the value
     * @return the new lazy value
     */
    static <T> ResettableLazy<T> concurrentOf(@NonNull Supplier<T> supplier) {
        return new Concurrent<>(supplier);
    }

    /**
     * Calls the supplier on the first call and after each {@link #reset()}. If the supplier returns {@code null}, the
     * next call calls it again.
     */
    T get();

    /**
     * Removes the value, so that the next {@link #get()} calls the supplier again. The default does nothing.
     */
    default void reset() {

    }

    /**
     * Resets the value, then gets a new value.
     */
    default T getAndReset() {
        this.reset();
        return this.get();
    }

    /**
     * Keeps a resettable lazy value without synchronization. Use it only from one thread.
     *
     * @param <T> the type of the value
     */
    final class Fast<T> implements ResettableLazy<T> {

        private final Supplier<T> supplier;
        private T instance;

        /**
         * Creates a resettable lazy value from the supplier.
         *
         * @param supplier gives the value
         */
        private Fast(Supplier<T> supplier) {
            this.supplier = supplier;
        }

        /**
         * Gets the value. It calls the supplier if there is no value yet.
         *
         * @return the value, or {@code null} if the supplier returned {@code null}
         */
        @Nullable
        @Override
        public T get() {
            if (this.instance == null) {
                this.instance = this.supplier.get();
            }
            return this.instance;
        }

        /**
         * Removes the value, so that the next {@link #get()} calls the supplier again.
         */
        @Override
        public void reset() {
            this.instance = null;
        }
    }

    /**
     * Keeps a resettable lazy value that many threads can get. It uses double-checked locking.
     *
     * @param <T> the type of the value
     */
    final class Concurrent<T> implements ResettableLazy<T> {

        private final Object lock = new Object();
        private final Supplier<T> supplier;
        private final AtomicReference<T> instance;

        /**
         * Creates a resettable lazy value from the supplier.
         *
         * @param supplier gives the value
         */
        private Concurrent(Supplier<T> supplier) {
            this.supplier = supplier;
            this.instance = new AtomicReference<>(null);
        }

        /**
         * Gets the value. If there is no value yet, it calls the supplier while it holds the lock.
         *
         * @return the value, or {@code null} if the supplier returned {@code null}
         */
        @Nullable
        @Override
        public T get() {
            if (this.instance.get() == null) {
                synchronized (this.lock) {
                    if (this.instance.get() == null) {
                        this.instance.set(this.supplier.get());
                    }
                }
            }
            return this.instance.get();
        }

        /**
         * Removes the value while it holds the lock, so that the next {@link #get()} calls the supplier again.
         */
        @Override
        public void reset() {
            synchronized (this.lock) {
                this.instance.set(null);
            }
        }
    }
}
