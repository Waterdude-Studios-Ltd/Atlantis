package de.waterdu.atlantis.ui.api;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;

class ButtonUpdateTaskTest {

    private final List<Long> fired = new ArrayList<>();

    private ButtonUpdateTask task(int tickrate, int delay) {
        return new ButtonUpdateTask(tickrate, delay, (button, tick, task) -> this.fired.add(tick));
    }

    private void run(ButtonUpdateTask task, int invocations) {
        for (var i = 1; i <= invocations; i++) {
            task.invoke(null, i);
        }
    }

    @Test
    void firesEveryTickrateInvocations() {
        this.run(this.task(3, 0), 9);

        assertThat(this.fired).containsExactly(3L, 6L, 9L);
    }

    @Test
    void delayIsConsumedBeforeCounting() {
        this.run(this.task(3, 2), 8);

        assertThat(this.fired).containsExactly(5L, 8L);
    }

    @Test
    void nonPositiveTickrateNeverFires() {
        this.run(this.task(0, 0), 5);
        this.run(this.task(-1, 0), 5);

        assertThat(this.fired).isEmpty();
    }

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

    @Test
    void taskReceivesItself() {
        var self = new ArrayList<ButtonUpdateTask>();
        var task = new ButtonUpdateTask(1, 0, (button, tick, it) -> self.add(it));
        task.invoke(null, 1);

        assertThat(self).containsExactly(task);
    }

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

    @Test
    void computeIfAbsentStoresOnce() {
        var task = this.task(1, 0);
        var calls = new AtomicInteger();

        assertThat(task.computeIfAbsent(1, () -> calls.incrementAndGet())).isEqualTo(1);
        assertThat(task.computeIfAbsent(1, () -> calls.incrementAndGet())).isEqualTo(1);
        assertThat(calls).hasValue(1);
    }

    @Test
    void getAndModifyReturnsThePreviousValue() {
        var task = this.task(1, 0);

        assertThat(task.getAndModify(1, 0, value -> value + 1)).isEqualTo(0);
        assertThat(task.getAndModify(1, 0, value -> value + 1)).isEqualTo(1);
        Supplier<Integer> ten = () -> 10;
        assertThat(task.<Integer>getAndModify(2, ten, value -> value * 2)).isEqualTo(10);
        assertThat(task.<Integer>getOrDefault(2, 0)).isEqualTo(20);
    }

    @Test
    void modifyAndGetReturnsTheNewValue() {
        var task = this.task(1, 0);

        assertThat(task.modifyAndGet(1, 0, value -> value + 1)).isEqualTo(1);
        assertThat(task.modifyAndGet(1, 0, value -> value + 1)).isEqualTo(2);
        Supplier<Integer> ten = () -> 10;
        assertThat(task.<Integer>modifyAndGet(2, ten, value -> value * 2)).isEqualTo(20);
    }
}
