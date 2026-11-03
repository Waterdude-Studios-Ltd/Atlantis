package de.waterdu.atlantis.ui.api;

import de.waterdu.atlantis.util.java.interfaces.TriConsumer;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

/**
 * Runs a task on a button at a fixed rate of page updates. It also keeps values between runs, stored by integer key.
 */
public class ButtonUpdateTask {

    private int tickrate;
    private int delay;
    private int tick = 0;
    private TriConsumer<Button, Long, ButtonUpdateTask> task;
    private final Map<Integer, Object> valueStore = new HashMap<>();

    /**
     * Creates an update task.
     *
     * @param tickrate the number of page updates between runs. 0 or less stops the task.
     * @param delay the number of page updates to skip before the task starts to count
     * @param task receives the button, the tick and this update task
     */
    protected ButtonUpdateTask(int tickrate, int delay, TriConsumer<Button, Long, ButtonUpdateTask> task) {
        this.tickrate = tickrate;
        this.delay = delay;
        this.task = task;
    }

    /**
     * Counts one page update. If a delay is left, it decreases the delay instead. The task runs when the count
     * reaches the tick rate, and then the count resets.
     *
     * @param button the button of this task
     * @param tick the tick that is given to the task
     */
    public void invoke(Button button, long tick) {
        if (this.delay > 0) {
            this.delay--;
        } else if (this.tickrate > 0 && ++this.tick >= this.tickrate) {
            this.task.accept(button, tick, this);
            this.tick = 0;
        }
    }

    /**
     * @param tickrate the number of page updates between runs. A page updates every {@link PageOptions#refreshRate()}
     *     ticks. The task only counts updates while its button is visible. 0 or less stops the task.
     */
    public void setTickrate(int tickrate) {
        this.tickrate = tickrate;
    }

    /**
     * @param delay the number of page updates to skip before the task starts to count
     */
    public void setDelay(int delay) {
        this.delay = delay;
    }

    /**
     * Adds to the number of page updates to skip.
     *
     * @param delay the number of page updates to add to the delay
     */
    public void addDelay(int delay) {
        this.delay += delay;
    }

    /**
     * Sets the delay to 0, so the task starts to count at the next page update.
     */
    public void clearDelay() {
        this.delay = 0;
    }

    /**
     * Replaces the task that runs.
     *
     * @param task receives the button, the tick and this update task
     */
    public void setTask(TriConsumer<Button, Long, ButtonUpdateTask> task) {
        this.task = task;
    }

    /**
     * Gets a stored value.
     *
     * @param key the key of the value
     * @param defaultValue the value to return if the key has no value
     * @param <T> the type of the value
     * @return the stored value, or {@code defaultValue}
     */
    @SuppressWarnings("unchecked")
    public <T> T getOrDefault(int key, T defaultValue) {
        return (T) this.valueStore.getOrDefault(key, defaultValue);
    }

    /**
     * @param defaultValue is called even if the key has a value
     */
    @SuppressWarnings("unchecked")
    public <T> T getOrDefault(int key, Supplier<T> defaultValue) {
        return (T) this.valueStore.getOrDefault(key, defaultValue.get());
    }

    /**
     * Stores a value. It replaces the old value of the key.
     *
     * @param key the key of the value
     * @param value the value to store
     */
    public void put(int key, Object value) {
        this.valueStore.put(key, value);
    }

    /**
     * Removes a stored value.
     *
     * @param key the key of the value
     * @param <T> the type of the value
     * @return the removed value, or {@code null} if the key had no value
     */
    @SuppressWarnings("unchecked")
    public <T> T remove(int key) {
        return (T) this.valueStore.remove(key);
    }

    /**
     * Gets a stored value. If the key has no value, it stores and returns the value of the supplier.
     *
     * @param key the key of the value
     * @param ifAbsent gives the value to store if the key has no value
     * @param <T> the type of the value
     * @return the stored value
     */
    @SuppressWarnings("unchecked")
    public <T> T computeIfAbsent(int key, Supplier<T> ifAbsent) {
        return (T) this.valueStore.computeIfAbsent(key, k -> ifAbsent.get());
    }

    /**
     * Stores the modified value and returns the value from before the change.
     *
     * @param key the key of the value
     * @param defaultValue the value to modify if the key has no value
     * @param modification changes the value
     * @param <T> the type of the value
     * @return the value before the change
     */
    public <T> T getAndModify(int key, T defaultValue, UnaryOperator<T> modification) {
        var value = this.getOrDefault(key, defaultValue);
        this.put(key, modification.apply(value));
        return value;
    }

    /**
     * Stores the modified value and returns it.
     *
     * @param key the key of the value
     * @param defaultValue the value to modify if the key has no value
     * @param modification changes the value
     * @param <T> the type of the value
     * @return the value after the change
     */
    public <T> T modifyAndGet(int key, T defaultValue, UnaryOperator<T> modification) {
        this.put(key, modification.apply(this.getOrDefault(key, defaultValue)));
        return this.getOrDefault(key, defaultValue);
    }

    /**
     * Stores the modified value and returns the value from before the change.
     *
     * @param key the key of the value
     * @param defaultValue gives the value to modify if the key has no value. It is called even if the key has a
     *     value.
     * @param modification changes the value
     * @param <T> the type of the value
     * @return the value before the change
     */
    public <T> T getAndModify(int key, Supplier<T> defaultValue, UnaryOperator<T> modification) {
        var value = this.getOrDefault(key, defaultValue);
        this.put(key, modification.apply(value));
        return value;
    }

    /**
     * Stores the modified value and returns it.
     *
     * @param key the key of the value
     * @param defaultValue gives the value to modify if the key has no value. It is called even if the key has a
     *     value.
     * @param modification changes the value
     * @param <T> the type of the value
     * @return the value after the change
     */
    public <T> T modifyAndGet(int key, Supplier<T> defaultValue, UnaryOperator<T> modification) {
        this.put(key, modification.apply(this.getOrDefault(key, defaultValue)));
        return this.getOrDefault(key, defaultValue);
    }

    /**
     * Stops the task and removes it from the button.
     *
     * @param button the button that has this task
     */
    public void end(Button button) {
        this.setTickrate(-1);
        button.removeUpdateTask(this);
    }
}
