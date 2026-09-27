package de.waterdu.atlantis.util.java.interfaces;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

public sealed interface Lazy<T> extends Supplier<T> {
    
    static <T> Fast<T> of(@Nonnull Supplier<T> supplier) {
        return new Fast<>(supplier);
    }

    static <T> Throwing<T> throwingOf(@Nonnull ThrowingSupplier<T> supplier) {
        return new Throwing<>(supplier);
    }
    
    static <T> Concurrent<T> concurrentOf(@Nonnull Supplier<T> supplier) {
        return new Concurrent<>(supplier);
    }

    static <T> ConcurrentThrowing<T> concurrentThrowingOf(@Nonnull ThrowingSupplier<T> supplier) {
        return new ConcurrentThrowing<>(supplier);
    }

    T get();

    final class Fast<T> implements Lazy<T> {

        private final Supplier<T> supplier;
        private T instance;

        private Fast(Supplier<T> supplier) {
            this.supplier = supplier;
        }

        @Nullable
        @Override
        public T get() {
            if (this.instance == null) {
                this.instance = this.supplier.get();
            }
            return this.instance;
        }
    }

    final class Throwing<T> implements Lazy<T> {

        private final ThrowingSupplier<T> supplier;
        private T instance;

        private Throwing(ThrowingSupplier<T> supplier) {
            this.supplier = supplier;
        }

        @Nullable
        @Override
        public T get() {
            try {
                return this.getThrowing();
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        }

        public T getThrowing() throws Throwable {
            if (this.instance == null) {
                this.instance = this.supplier.get();
            }
            return this.instance;
        }
    }

    final class Concurrent<T> implements Lazy<T> {

        private final Object lock = new Object();
        private final Supplier<T> supplier;
        private final AtomicReference<T> instance;

        private Concurrent(Supplier<T> supplier) {
            this.supplier = supplier;
            this.instance = new AtomicReference<>(null);
        }

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

    final class ConcurrentThrowing<T> implements Lazy<T> {

        private final Object lock = new Object();
        private final ThrowingSupplier<T> supplier;
        private final AtomicReference<T> instance;

        private ConcurrentThrowing(ThrowingSupplier<T> supplier) {
            this.supplier = supplier;
            this.instance = new AtomicReference<>(null);
        }

        @Nullable
        @Override
        public T get() {
            try {
                return this.getThrowing();
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        }

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
