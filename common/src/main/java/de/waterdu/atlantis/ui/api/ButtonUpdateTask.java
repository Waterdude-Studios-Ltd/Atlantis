package de.waterdu.atlantis.ui.api;

import de.waterdu.atlantis.util.java.interfaces.TriConsumer;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

public class ButtonUpdateTask {

    private int tickrate;
    private int delay;
    private int tick = 0;
    private TriConsumer<Button, Long, ButtonUpdateTask> task;
    private final Map<Integer, Object> valueStore = new HashMap<>();

    protected ButtonUpdateTask(int tickrate, int delay, TriConsumer<Button, Long, ButtonUpdateTask> task) {
        this.tickrate = tickrate;
        this.delay = delay;
        this.task = task;
    }

    public void invoke(Button button, long tick) {
        if (this.delay > 0) {
            this.delay--;
        } else if (this.tickrate > 0 && ++this.tick >= this.tickrate) {
            this.task.accept(button, tick, this);
            this.tick = 0;
        }
    }

    public void setTickrate(int tickrate) {
        this.tickrate = tickrate;
    }

    public void setDelay(int delay) {
        this.delay = delay;
    }

    public void addDelay(int delay) {
        this.delay += delay;
    }

    public void clearDelay() {
        this.delay = 0;
    }

    public void setTask(TriConsumer<Button, Long, ButtonUpdateTask> task) {
        this.task = task;
    }

    @SuppressWarnings("unchecked")
    public <T> T getOrDefault(int key, T defaultValue) {
        return (T) this.valueStore.getOrDefault(key, defaultValue);
    }

    @SuppressWarnings("unchecked")
    public <T> T getOrDefault(int key, Supplier<T> defaultValue) {
        return (T) this.valueStore.getOrDefault(key, defaultValue.get());
    }

    public void put(int key, Object value) {
        this.valueStore.put(key, value);
    }

    @SuppressWarnings("unchecked")
    public <T> T remove(int key) {
        return (T) this.valueStore.remove(key);
    }

    @SuppressWarnings("unchecked")
    public <T> T computeIfAbsent(int key, Supplier<T> ifAbsent) {
        return (T) this.valueStore.computeIfAbsent(key, k -> ifAbsent.get());
    }

    public <T> T getAndModify(int key, T defaultValue, UnaryOperator<T> modification) {
        var value = this.getOrDefault(key, defaultValue);
        this.put(key, modification.apply(value));
        return value;
    }

    public <T> T modifyAndGet(int key, T defaultValue, UnaryOperator<T> modification) {
        this.put(key, modification.apply(this.getOrDefault(key, defaultValue)));
        return this.getOrDefault(key, defaultValue);
    }

    public <T> T getAndModify(int key, Supplier<T> defaultValue, UnaryOperator<T> modification) {
        var value = this.getOrDefault(key, defaultValue);
        this.put(key, modification.apply(value));
        return value;
    }

    public <T> T modifyAndGet(int key, Supplier<T> defaultValue, UnaryOperator<T> modification) {
        this.put(key, modification.apply(this.getOrDefault(key, defaultValue)));
        return this.getOrDefault(key, defaultValue);
    }

    public void end(Button button) {
        this.setTickrate(-1);
        button.removeUpdateTask(this);
    }
}
