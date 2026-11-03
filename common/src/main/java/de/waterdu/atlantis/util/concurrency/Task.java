package de.waterdu.atlantis.util.concurrency;

import de.waterdu.atlantis.Atlantis;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Runs a delayed task on an {@link AtlantisThreadPool} or on the main thread, once or repeatedly.
 */
public class Task implements Runnable {

    private final String id;
    private final AtlantisThreadPool executor;

    private final boolean async;
    private final Duration originalDuration;
    private final Runnable task;

    private boolean repeats;
    private Duration duration;

    private boolean cancelled = false;

    /**
     * Creates a task. The task does nothing after it is cancelled.
     *
     * @param id the ID of the task
     * @param executor the pool that waits for the duration
     * @param repeats if {@code true}, the task schedules itself again after each run
     * @param async if {@code true}, the task runs on the pool. If not, it runs on the main thread.
     * @param duration the delay before each run
     * @param task the action to run, which receives this task
     */
    private Task(String id, AtlantisThreadPool executor, boolean repeats, boolean async, Duration duration, Consumer<Task> task) {
        this.id = id;
        this.executor = executor;
        this.repeats = repeats;
        this.async = async;
        this.originalDuration = duration;
        this.duration = duration;
        this.task = () -> {
            if (this.cancelled) {
                return;
            }
            task.accept(this);
            if (this.repeats && !this.cancelled) {
                this.schedule();
            }
        };
    }

    /**
     * Creates a builder for a task.
     *
     * @param id the ID of the task
     * @return the builder
     */
    public static Builder builder(String id) {
        return new Builder(id);
    }

    /**
     * Runs the task once the duration elapses. A repeating task schedules itself again after each run. Each call adds a
     * separate run.
     */
    public Task schedule() {
        this.executor.schedule(this, this.duration.toMillis(), TimeUnit.MILLISECONDS);
        return this;
    }

    /**
     * Runs the task now, on the current thread if it is async or on the main thread if not.
     */
    @Override
    public void run() {
        if (this.async) {
            this.task.run();
        } else {
            Concurrency.sync(this.task);
        }
    }

    /**
     * @return the ID of the task
     */
    public String id() {
        return this.id;
    }

    /**
     * @return the delay that the task was built with
     */
    public Duration originalDuration() {
        return this.originalDuration;
    }

    /**
     * @return the current delay before each run
     */
    public Duration duration() {
        return this.duration;
    }

    /**
     * Sets the delay for the next calls of {@link #schedule()}.
     *
     * @param duration the new delay
     */
    public void setDuration(Duration duration) {
        this.duration = duration;
    }

    /**
     * @return the wrapped action, which skips cancelled tasks and schedules repeating tasks again
     */
    public Runnable task() {
        return this.task;
    }

    /**
     * @return {@code true} if the task schedules itself again after each run
     */
    public boolean repeats() {
        return this.repeats;
    }

    /**
     * Sets if the task schedules itself again after each run.
     *
     * @param repeat {@code true} to repeat the task
     */
    public void repeat(boolean repeat) {
        this.repeats = repeat;
    }

    /**
     * Cancels the task. Runs that are already scheduled do nothing, and the task does not repeat.
     */
    public void cancel() {
        this.cancelled = true;
        this.repeats = false;
    }

    /**
     * @return {@code true} if {@link #cancel()} was called
     */
    public boolean isCancelled() {
        return this.cancelled;
    }

    /**
     * Checks if the object is a task with the same ID.
     *
     * @param obj the object to compare
     * @return {@code true} if the object is a task with the same ID
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof Task)) {
            return false;
        }
        return this.id.equals(((Task) obj).id);
    }

    /**
     * @return the hash code of the ID
     */
    @Override
    public int hashCode() {
        return this.id.hashCode();
    }

    /**
     * @return {@code ScheduledTask(<id>)}
     */
    @Override
    public String toString() {
        return "ScheduledTask(" + this.id + ")";
    }

    /**
     * Builds a {@link Task}. By default, the task runs once, async, on {@link Atlantis#THREAD_POOL} and without delay.
     */
    public static class Builder {

        private final String id;
        private AtlantisThreadPool executor = Atlantis.THREAD_POOL;
        private boolean async = true;
        private boolean repeats = false;
        private Duration duration = Duration.ZERO;
        private Consumer<Task> task = ___ -> {};

        /**
         * Creates a builder.
         *
         * @param id the ID of the task
         */
        private Builder(String id) {
            this.id = id;
        }

        /**
         * Sets the pool that waits for the delay and runs async tasks.
         *
         * @param executor the pool
         * @return this builder
         */
        public Builder executor(AtlantisThreadPool executor) {
            this.executor = executor;
            return this;
        }

        /**
         * Sets if the task runs on the pool or on the main thread.
         *
         * @param async {@code true} to run on the pool, {@code false} to run on the main thread
         * @return this builder
         */
        public Builder async(boolean async) {
            this.async = async;
            return this;
        }

        /**
         * Makes the task run on the pool.
         *
         * @return this builder
         */
        public Builder async() {
            return this.async(true);
        }

        /**
         * Makes the task run on the main thread.
         *
         * @return this builder
         */
        public Builder sync() {
            return this.async(false);
        }

        /**
         * Sets the delay to zero.
         *
         * @return this builder
         */
        public Builder instant() {
            this.duration = Duration.ZERO;
            return this;
        }

        /**
         * Converts the ticks to milliseconds at 50 ms for each tick. The delay does not follow the server tick rate.
         */
        public Builder ticks(long ticks) {
            return this.duration(ticks * 50L, ChronoUnit.MILLIS);
        }

        /**
         * Sets the delay in milliseconds.
         *
         * @param milliseconds the delay
         * @return this builder
         */
        public Builder milliseconds(long milliseconds) {
            return this.duration(milliseconds, ChronoUnit.MILLIS);
        }

        /**
         * Sets the delay in seconds.
         *
         * @param seconds the delay
         * @return this builder
         */
        public Builder seconds(long seconds) {
            return this.duration(seconds, ChronoUnit.SECONDS);
        }

        /**
         * Sets the delay in minutes.
         *
         * @param minutes the delay
         * @return this builder
         */
        public Builder minutes(long minutes) {
            return this.duration(minutes, ChronoUnit.MINUTES);
        }

        /**
         * Sets the delay. The delay is converted to whole milliseconds.
         *
         * @param duration the delay
         * @param unit the unit of the delay
         * @return this builder
         */
        public Builder duration(long duration, TimeUnit unit) {
            return this.duration(TimeUnit.MILLISECONDS.convert(duration, unit), ChronoUnit.MILLIS);
        }

        /**
         * Sets the delay.
         *
         * @param duration the delay
         * @param unit the unit of the delay
         * @return this builder
         */
        public Builder duration(long duration, TemporalUnit unit) {
            this.duration = Duration.of(duration, unit);
            return this;
        }

        /**
         * Sets if the task schedules itself again after each run.
         *
         * @param repeats {@code true} to repeat the task
         * @return this builder
         */
        public Builder repeats(boolean repeats) {
            this.repeats = repeats;
            return this;
        }

        /**
         * Makes the task repeat after each run.
         *
         * @return this builder
         */
        public Builder repeating() {
            return this.repeats(true);
        }

        /**
         * Makes the task run only once.
         *
         * @return this builder
         */
        public Builder once() {
            return this.repeats(false);
        }

        /**
         * Replaces the task. Use {@link #andThen(Consumer)} to add to it.
         */
        public Builder task(Consumer<Task> task) {
            this.task = task;
            return this;
        }

        /**
         * Replaces the task with a runnable. Use {@link #andThen(Runnable)} to add to it.
         *
         * @param task the action to run
         * @return this builder
         */
        public Builder task(Runnable task) {
            return this.task(___ -> task.run());
        }

        /**
         * Adds an action that runs after the current task.
         *
         * @param task the action to add, which receives the task
         * @return this builder
         */
        public Builder andThen(Consumer<Task> task) {
            this.task = this.task.andThen(task);
            return this;
        }

        /**
         * Adds a runnable that runs after the current task.
         *
         * @param task the action to add
         * @return this builder
         */
        public Builder andThen(Runnable task) {
            return this.andThen(___ -> task.run());
        }

        /**
         * Builds the task without scheduling it.
         *
         * @return the task
         */
        public Task build() {
            return new Task(this.id, this.executor, this.repeats, this.async, this.duration, this.task);
        }

        /**
         * Builds the task and schedules it.
         *
         * @return the task
         */
        public Task schedule() {
            return this.build().schedule();
        }
    }
}
