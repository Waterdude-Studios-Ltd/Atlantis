package de.waterdu.atlantis.util.java.interfaces;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

public sealed interface ResettableLazy<T> extends Supplier<T> permits ResettableLazy.Fast, ResettableLazy.Concurrent {
    
    static <T> ResettableLazy<T> of(@Nonnull Supplier<T> supplier) {
        return new Fast<>(supplier);
    }
    
    static <T> ResettableLazy<T> concurrentOf(@Nonnull Supplier<T> supplier) {
        return new Concurrent<>(supplier);
    }

    T get();

    default void reset() {

    }

    default T getAndReset() {
        this.reset();
        return this.get();
    }

    final class Fast<T> implements ResettableLazy<T> {

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

        @Override
        public void reset() {
            this.instance = null;
        }
    }

    final class Concurrent<T> implements ResettableLazy<T> {

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

        @Override
        public void reset() {
            synchronized (this.lock) {
                this.instance.set(null);
            }
        }
    }
}
