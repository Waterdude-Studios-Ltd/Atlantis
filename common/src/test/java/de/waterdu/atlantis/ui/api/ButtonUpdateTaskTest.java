package de.waterdu.atlantis.ui.api;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests the tick counting, the delay and the value store of {@link ButtonUpdateTask}.
 */
class ButtonUpdateTaskTest {

    private final List<Long> fired = new ArrayList<>();

    /**
     * Creates a task that adds each tick it fires on to {@link #fired}.
     *
     * @param tickrate the number of invocations between two runs
     * @param delay the number of invocations to skip before counting starts
     * @return the task
     */
    private ButtonUpdateTask task(int tickrate, int delay) {
        return new ButtonUpdateTask(tickrate, delay, (button, tick, task) -> this.fired.add(tick));
    }

    /**
     * Invokes the task with no button, once for each tick from {@code 1} to {@code invocations}.
     *
     * @param task the task to invoke
     * @param invocations the number of invocations
     */
    private void run(ButtonUpdateTask task, int invocations) {
        for (var i = 1; i <= invocations; i++) {
            task.invoke(null, i);
        }
    }

    /**
     * Checks that the task fires once every {@code tickrate} invocations.
     */
    @Test
    void firesEveryTickrateInvocations() {
        this.run(this.task(3, 0), 9);

        assertThat(this.fired).containsExactly(3L, 6L, 9L);
    }

    /**
     * Checks that the delay passes before the task starts to count invocations.
     */
    @Test
    void delayIsConsumedBeforeCounting() {
        this.run(this.task(3, 2), 8);

        assertThat(this.fired).containsExactly(5L, 8L);
    }

    /**
     * Checks that a task with a tickrate of zero or less never fires.
     */
    @Test
    void nonPositiveTickrateNeverFires() {
        this.run(this.task(0, 0), 5);
        this.run(this.task(-1, 0), 5);

        assertThat(this.fired).isEmpty();
    }

    /**
     * Checks that {@link ButtonUpdateTask#setDelay}, {@link ButtonUpdateTask#addDelay} and
     * {@link ButtonUpdateTask#clearDelay} change the remaining delay.
     */
    @Test
    void delayCanBeChanged() {
        var task = this.task(1, 0);
        task.setDelay(2);
        task.addDelay(1);
        this.run(task, 4);

        assertThat(this.fired).containsExactly(4L);

        task.addDelay(10);
        task.clearDelay();
        task.invoke(null, 5);

        assertThat(this.fired).containsExactly(4L, 5L);
    }

    /**
     * Checks that a new tickrate and a new task replace the old ones.
     */
    @Test
    void tickrateAndTaskCanBeChanged() {
        var task = this.task(5, 0);
        var replaced = new AtomicInteger();
        task.setTickrate(1);
        task.setTask((button, tick, self) -> replaced.incrementAndGet());
        this.run(task, 2);

        assertThat(this.fired).isEmpty();
        assertThat(replaced).hasValue(2);
    }

    /**
     * Checks that the task function receives the {@link ButtonUpdateTask} that runs it.
     */
    @Test
    void taskReceivesItself() {
        var self = new ArrayList<ButtonUpdateTask>();
        var task = new ButtonUpdateTask(1, 0, (button, tick, it) -> self.add(it));
        task.invoke(null, 1);

        assertThat(self).containsExactly(task);
    }

    /**
     * Checks that values can be put, read with a default or a supplied default, and removed.
     */
    @Test
    void valueStore() {
        var task = this.task(1, 0);

        assertThat(task.getOrDefault(1, "default")).isEqualTo("default");
        task.put(1, "value");
        assertThat(task.<String>getOrDefault(1, "default")).isEqualTo("value");
        assertThat(task.getOrDefault(1, () -> "supplied")).isEqualTo("value");
        assertThat(task.getOrDefault(2, () -> "supplied")).isEqualTo("supplied");
        assertThat(task.<String>remove(1)).isEqualTo("value");
        assertThat(task.<String>remove(1)).isNull();
    }

    /**
     * Checks that {@link ButtonUpdateTask#computeIfAbsent} calls the supplier only when no value is stored.
     */
    @Test
    void computeIfAbsentStoresOnce() {
        var task = this.task(1, 0);
        var calls = new AtomicInteger();

        assertThat(task.computeIfAbsent(1, () -> calls.incrementAndGet())).isEqualTo(1);
        assertThat(task.computeIfAbsent(1, () -> calls.incrementAndGet())).isEqualTo(1);
        assertThat(calls).hasValue(1);
    }

    /**
     * Checks that {@link ButtonUpdateTask#getAndModify} stores the modified value and returns the previous one.
     */
    @Test
    void getAndModifyReturnsThePreviousValue() {
        var task = this.task(1, 0);

        assertThat(task.getAndModify(1, 0, value -> value + 1)).isEqualTo(0);
        assertThat(task.getAndModify(1, 0, value -> value + 1)).isEqualTo(1);
        Supplier<Integer> ten = () -> 10;
        assertThat(task.<Integer>getAndModify(2, ten, value -> value * 2)).isEqualTo(10);
        assertThat(task.<Integer>getOrDefault(2, 0)).isEqualTo(20);
    }

    /**
     * Checks that {@link ButtonUpdateTask#modifyAndGet} stores the modified value and returns it.
     */
    @Test
    void modifyAndGetReturnsTheNewValue() {
        var task = this.task(1, 0);

        assertThat(task.modifyAndGet(1, 0, value -> value + 1)).isEqualTo(1);
        assertThat(task.modifyAndGet(1, 0, value -> value + 1)).isEqualTo(2);
        Supplier<Integer> ten = () -> 10;
        assertThat(task.<Integer>modifyAndGet(2, ten, value -> value * 2)).isEqualTo(20);
    }
}
