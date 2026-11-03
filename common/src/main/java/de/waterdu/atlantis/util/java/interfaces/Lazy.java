package de.waterdu.atlantis.util.java.interfaces;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * Gets a value from a supplier on first use and keeps it.
 *
 * @param <T> the type of the value
 */
public sealed interface Lazy<T> extends Supplier<T> {

    /**
     * Creates a lazy value that is not thread-safe.
     *
     * @param supplier gives the value
     * @param <T> the type of the value
     * @return the new lazy value
     */
    static <T> Fast<T> of(@NonNull Supplier<T> supplier) {
        return new Fast<>(supplier);
    }

    /**
     * Creates a lazy value from a supplier that can throw. It is not thread-safe.
     *
     * @param supplier gives the value
     * @param <T> the type of the value
     * @return the new lazy value
     */
    static <T> Throwing<T> throwingOf(@NonNull ThrowingSupplier<T> supplier) {
        return new Throwing<>(supplier);
    }

    /**
     * Creates a thread-safe lazy value. Only one thread at a time calls the supplier.
     *
     * @param supplier gives the value
     * @param <T> the type of the value
     * @return the new lazy value
     */
    static <T> Concurrent<T> concurrentOf(@NonNull Supplier<T> supplier) {
        return new Concurrent<>(supplier);
    }

    /**
     * Creates a thread-safe lazy value from a supplier that can throw. Only one thread at a time calls the supplier.
     *
     * @param supplier gives the value
     * @param <T> the type of the value
     * @return the new lazy value
     */
    static <T> ConcurrentThrowing<T> concurrentThrowingOf(@NonNull ThrowingSupplier<T> supplier) {
        return new ConcurrentThrowing<>(supplier);
    }

    /**
     * Calls the supplier on the first call. If the supplier returns {@code null}, the next call calls it again.
     */
    T get();

    /**
     * Keeps a lazy value without synchronization. Use it only from one thread.
     *
     * @param <T> the type of the value
     */
    final class Fast<T> implements Lazy<T> {

        private final Supplier<T> supplier;
        private T instance;

        /**
         * Creates a lazy value from the supplier.
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
    }

    /**
     * Keeps a lazy value from a supplier that can throw, without synchronization. Use it only from one thread.
     *
     * @param <T> the type of the value
     */
    final class Throwing<T> implements Lazy<T> {

        private final ThrowingSupplier<T> supplier;
        private T instance;

        /**
         * Creates a lazy value from the supplier.
         *
         * @param supplier gives the value
         */
        private Throwing(ThrowingSupplier<T> supplier) {
            this.supplier = supplier;
        }

        /**
         * Gets the value with {@link #getThrowing()}.
         *
         * @return the value, or {@code null} if the supplier returned {@code null}
         * @throws RuntimeException if the supplier throws. It wraps the exception of the supplier.
         */
        @Nullable
        @Override
        public T get() {
            try {
                return this.getThrowing();
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        }

        /**
         * Throws the exception of the supplier as it is. {@link #get()} wraps it in a {@link RuntimeException}.
         */
        public T getThrowing() throws Throwable {
            if (this.instance == null) {
                this.instance = this.supplier.get();
            }
            return this.instance;
        }
    }

    /**
     * Keeps a lazy value that many threads can get. It uses double-checked locking.
     *
     * @param <T> the type of the value
     */
    final class Concurrent<T> implements Lazy<T> {

        private final Object lock = new Object();
        private final Supplier<T> supplier;
        private final AtomicReference<T> instance;

        /**
         * Creates a lazy value from the supplier.
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
    }

    /**
     * Keeps a lazy value from a supplier that can throw, that many threads can get. It uses double-checked locking.
     *
     * @param <T> the type of the value
     */
    final class ConcurrentThrowing<T> implements Lazy<T> {

        private final Object lock = new Object();
        private final ThrowingSupplier<T> supplier;
        private final AtomicReference<T> instance;

        /**
         * Creates a lazy value from the supplier.
         *
         * @param supplier gives the value
         */
        private ConcurrentThrowing(ThrowingSupplier<T> supplier) {
            this.supplier = supplier;
            this.instance = new AtomicReference<>(null);
        }

        /**
         * Gets the value with {@link #getThrowing()}.
         *
         * @return the value, or {@code null} if the supplier returned {@code null}
         * @throws RuntimeException if the supplier throws. It wraps the exception of the supplier.
         */
        @Nullable
        @Override
        public T get() {
            try {
                return this.getThrowing();
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        }

        /**
         * Throws the exception of the supplier as it is. {@link #get()} wraps it in a {@link RuntimeException}.
         */
        public T getThrowing() throws Throwable {
            if (this.instance.get() == null) {
                synchronized (this.lock) {
                    if (this.instance.get() == null) {
                        this.instance.set(this.supplier.get());
                    }
                }
            }
            return this.instance.get();
        }
    }
}
