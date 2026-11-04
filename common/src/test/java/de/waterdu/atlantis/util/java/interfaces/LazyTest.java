package de.waterdu.atlantis.util.java.interfaces;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests {@link Lazy}.
 */
class LazyTest {

    /**
     * Checks that {@link Lazy#of} computes the value only on the first get.
     */
    @Test
    void fastComputesOnce() {
        var calls = new AtomicInteger();
        var lazy = Lazy.of(() -> "v" + calls.incrementAndGet());

        assertThat(calls).hasValue(0);
        assertThat(lazy.get()).isEqualTo("v1");
        assertThat(lazy.get()).isEqualTo("v1");
        assertThat(calls).hasValue(1);
    }

    /**
     * Checks that a {@code null} result is not cached, so each get computes the value again.
     */
    @Test
    void nullResultIsRecomputed() {
        var calls = new AtomicInteger();
        var lazy = Lazy.of(() -> {
            calls.incrementAndGet();
            return null;
        });

        lazy.get();
        lazy.get();

        assertThat(calls).hasValue(2);
    }

    /**
     * Checks that {@link Lazy#throwingOf} wraps a checked exception in a {@link RuntimeException} on get, and
     * throws it as it is on {@code getThrowing}.
     */
    @Test
    void throwingWrapsCheckedExceptions() throws Throwable {
        var failing = Lazy.throwingOf(() -> {
            throw new IOException("boom");
        });
        var working = Lazy.throwingOf(() -> 5);

        assertThatThrownBy(failing::get).isInstanceOf(RuntimeException.class).hasCauseInstanceOf(IOException.class);
        assertThatThrownBy(failing::getThrowing).isInstanceOf(IOException.class);
        assertThat(working.get()).isEqualTo(5);
        assertThat(working.getThrowing()).isEqualTo(5);
    }

    /**
     * Checks that {@link Lazy#concurrentThrowingOf} caches the value and wraps a checked exception on get.
     */
    @Test
    void concurrentThrowing() throws Throwable {
        var calls = new AtomicInteger();
        var lazy = Lazy.concurrentThrowingOf(calls::incrementAndGet);
        var failing = Lazy.concurrentThrowingOf(() -> {
            throw new IOException("boom");
        });

        assertThat(lazy.get()).isEqualTo(1);
        assertThat(lazy.getThrowing()).isEqualTo(1);
        assertThatThrownBy(failing::get).hasCauseInstanceOf(IOException.class);
    }

    /**
     * Checks that {@link Lazy#concurrentOf} computes the value only once when 16 threads get it at the same time.
     */
    @Test
    void concurrentComputesOnceAcrossThreads() throws InterruptedException {
        var calls = new AtomicInteger();
        var lazy = Lazy.concurrentOf(() -> {
            calls.incrementAndGet();
            sleep();
            return new Object();
        });
        var threads = 16;
        var start = new CountDownLatch(1);
        var results = new java.util.concurrent.ConcurrentLinkedQueue<Object>();
        try (var executor = Executors.newFixedThreadPool(threads)) {
            for (var i = 0; i < threads; i++) {
                executor.submit(() -> {
                    start.await();
                    results.add(lazy.get());
                    return null;
                });
            }
            start.countDown();
            executor.shutdown();
            assertThat(executor.awaitTermination(10, TimeUnit.SECONDS)).isTrue();
        }

        assertThat(calls).hasValue(1);
        assertThat(results).hasSize(threads).containsOnly(lazy.get());
    }

    /**
     * Sleeps for 20 milliseconds. If the thread is interrupted, it sets the interrupt flag again.
     */
    private static void sleep() {
        try {
            Thread.sleep(20);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
