package de.waterdu.atlantis.util.concurrency;

import de.waterdu.atlantis.test.AtlantisTestEnvironment;
import de.waterdu.atlantis.test.MinecraftBootstrapped;
import de.waterdu.atlantis.test.MockServers;
import de.waterdu.atlantis.test.TestModLoader;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@MinecraftBootstrapped
@ExtendWith(AtlantisTestEnvironment.class)
class TaskTest {

    private AtlantisThreadPool pool;

    @BeforeEach
    void createPool() {
        this.pool = AtlantisThreadPool.createNewThreadPool("atlantis-task-test", 2, true);
    }

    @AfterEach
    void stopPool() {
        this.pool.getPool().shutdownNow();
    }

    @Test
    void durationsConvertToMilliseconds() {
        assertThat(Task.builder("t").ticks(3).build().duration()).isEqualTo(Duration.ofMillis(150));
        assertThat(Task.builder("t").milliseconds(7).build().duration()).isEqualTo(Duration.ofMillis(7));
        assertThat(Task.builder("t").seconds(2).build().duration()).isEqualTo(Duration.ofSeconds(2));
        assertThat(Task.builder("t").minutes(1).build().duration()).isEqualTo(Duration.ofMinutes(1));
        assertThat(Task.builder("t").duration(4, TimeUnit.SECONDS).build().duration()).isEqualTo(Duration.ofSeconds(4));
        assertThat(Task.builder("t").seconds(9).instant().build().duration()).isEqualTo(Duration.ZERO);
    }

    @Test
    void builderDefaultsToAnAsyncOneShot() {
        var task = Task.builder("t").build();

        assertThat(task.repeats()).isFalse();
        assertThat(task.isCancelled()).isFalse();
        assertThat(task.originalDuration()).isEqualTo(Duration.ZERO);
        assertThat(task.id()).isEqualTo("t");
    }

    @Test
    void asyncTaskRunsOnThePool() throws InterruptedException {
        var latch = new CountDownLatch(1);
        var thread = new AtomicInteger();

        Task.builder("async").executor(this.pool).async().milliseconds(10).task(() -> {
            thread.set(Thread.currentThread().getName().startsWith("atlantis-task-test") ? 1 : -1);
            latch.countDown();
        }).schedule();

        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
        assertThat(thread).hasValue(1);
    }

    @Test
    void repeatingTaskRunsUntilCancelled() throws InterruptedException {
        var runs = new AtomicInteger();
        var latch = new CountDownLatch(3);

        var task = Task.builder("repeat").executor(this.pool).repeating().milliseconds(5).task(it -> {
            if (runs.incrementAndGet() == 3) {
                it.cancel();
            }
            latch.countDown();
        }).schedule();

        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
        Thread.sleep(100);
        assertThat(runs).hasValue(3);
        assertThat(task.isCancelled()).isTrue();
        assertThat(task.repeats()).isFalse();
    }

    @Test
    void cancelledTaskDoesNotRun() throws InterruptedException {
        var runs = new AtomicInteger();
        var task = Task.builder("cancelled").executor(this.pool).milliseconds(50).task(runs::incrementAndGet).schedule();

        task.cancel();
        Thread.sleep(200);

        assertThat(runs).hasValue(0);
    }

    @Test
    void andThenRunsBothTasksInOrder() {
        List<String> calls = new CopyOnWriteArrayList<>();
        var task = Task.builder("chain").executor(this.pool).task(() -> calls.add("first")).andThen(() -> calls.add("second")).build();

        task.task().run();

        assertThat(calls).containsExactly("first", "second");
    }

    @Test
    void syncTaskRunsThroughTheServer(TestModLoader loader) {
        loader.server(MockServers.executingInline());
        var runs = new AtomicInteger();
        var task = Task.builder("sync").executor(this.pool).sync().task(runs::incrementAndGet).build();

        task.run();

        assertThat(runs).hasValue(1);
    }

    @Test
    void equalityUsesTheId() {
        var task = Task.builder("same").build();

        assertThat(task).isEqualTo(Task.builder("same").seconds(5).build());
        assertThat(task).hasSameHashCodeAs(Task.builder("same").build());
        assertThat(task).isNotEqualTo(Task.builder("other").build());
        assertThat(task).hasToString("ScheduledTask(same)");
    }
}
