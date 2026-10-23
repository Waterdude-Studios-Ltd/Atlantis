package de.waterdu.atlantis.util.concurrency;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AtlantisThreadPoolTest {

    private final List<AtlantisThreadPool> pools = new ArrayList<>();

    private AtlantisThreadPool pool() {
        var pool = AtlantisThreadPool.createNewThreadPool("Atlantis Test", 2, true);
        this.pools.add(pool);
        return pool;
    }

    @AfterEach
    void shutdown() {
        this.pools.forEach(pool -> pool.getPool().shutdownNow());
    }

    @Test
    void submitRunsOnANamedDaemonThread() throws Exception {
        var thread = new java.util.concurrent.atomic.AtomicReference<Thread>();

        this.pool().submit(() -> thread.set(Thread.currentThread())).get(5, TimeUnit.SECONDS);

        assertThat(thread.get().getName()).isEqualTo("Atlantis Test Scheduled Thread Pool (worker 1)");
        assertThat(thread.get().isDaemon()).isTrue();
    }

    @Test
    void submitCallableReturnsItsValue() throws Exception {
        assertThat(this.pool().submit(() -> 42, -1).get(5, TimeUnit.SECONDS)).isEqualTo(42);
    }

    @Test
    void caughtErrorsReturnTheFallback() throws Exception {
        var pool = this.pool();

        var callable = pool.submit(() -> {
            throw new IllegalStateException("expected");
        }, -1);
        var runnable = pool.submit(() -> {
            throw new IllegalStateException("expected");
        });

        assertThat(callable.get(5, TimeUnit.SECONDS)).isEqualTo(-1);
        assertThat(runnable.get(5, TimeUnit.SECONDS)).isNull();
    }

    @Test
    void uncaughtErrorsPropagateToTheFuture() {
        var pool = this.pool().shouldCatchErrors(false);

        var future = pool.submit(() -> {
            throw new IllegalStateException("expected");
        }, -1);

        assertThatThrownBy(() -> future.get(5, TimeUnit.SECONDS)).isInstanceOf(ExecutionException.class).hasCauseInstanceOf(IllegalStateException.class);
    }

    @Test
    void scheduleWaitsForTheDelay() throws Exception {
        var start = System.nanoTime();

        this.pool().schedule(() -> {}, 100).get(5, TimeUnit.SECONDS);

        assertThat(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start)).isGreaterThanOrEqualTo(90);
    }

    @Test
    void scheduleAtFixedRateRepeats() throws Exception {
        var latch = new CountDownLatch(3);

        var future = this.pool().scheduleAtFixedRate(latch::countDown, 0, 10, TimeUnit.MILLISECONDS);

        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
        future.cancel(false);
    }

    @Test
    void repeatUntilCompleteStopsOnTrue() throws Exception {
        var calls = new AtomicInteger();
        var done = new CountDownLatch(1);

        this.pool().repeatUntilComplete(() -> {
            if (calls.incrementAndGet() == 3) {
                done.countDown();
                return true;
            }
            return false;
        }, 5);

        assertThat(done.await(5, TimeUnit.SECONDS)).isTrue();
        Thread.sleep(50);
        assertThat(calls).hasValue(3);
    }

    @Test
    void repeatUntilCompleteStopsOnError() throws Exception {
        var calls = new AtomicInteger();

        this.pool().repeatUntilComplete(() -> {
            calls.incrementAndGet();
            throw new IllegalStateException("expected");
        }, 5);

        Thread.sleep(100);
        assertThat(calls).hasValue(1);
    }

    @Test
    void shutdownAllOnlyStopsPoolsThatAreNotKeptAlive() {
        var kept = this.pool();
        var stopped = this.pool().keepAlive(false);

        AtlantisThreadPool.shutdownAll();

        assertThat(kept.getPool().isShutdown()).isFalse();
        assertThat(stopped.getPool().isShutdown()).isTrue();
    }

    @Test
    void setSize() {
        assertThat(this.pool().setSize(5).getPool().getCorePoolSize()).isEqualTo(5);
    }
}
