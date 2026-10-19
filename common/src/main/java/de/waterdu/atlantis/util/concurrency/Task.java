package de.waterdu.atlantis.util.concurrency;

import de.waterdu.atlantis.Atlantis;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class Task implements Runnable {

    private final String id;
    private final AtlantisThreadPool executor;

    private final boolean async;
    private final Duration originalDuration;
    private final Runnable task;

    private boolean repeats;
    private Duration duration;

    private boolean cancelled = false;

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

    public static Builder builder(String id) {
        return new Builder(id);
    }

    public Task schedule() {
        this.executor.schedule(this, this.duration.toMillis(), TimeUnit.MILLISECONDS);
        return this;
    }

    @Override
    public void run() {
        if (this.async) {
            this.task.run();
        } else {
            Concurrency.sync(this.task);
        }
    }

    public String id() {
        return this.id;
    }

    public Duration originalDuration() {
        return this.originalDuration;
    }

    public Duration duration() {
        return this.duration;
    }

    public void setDuration(Duration duration) {
        this.duration = duration;
    }

    public Runnable task() {
        return this.task;
    }

    public boolean repeats() {
        return this.repeats;
    }

    public void repeat(boolean repeat) {
        this.repeats = repeat;
    }

    public void cancel() {
        this.cancelled = true;
        this.repeats = false;
    }

    public boolean isCancelled() {
        return this.cancelled;
    }

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

    @Override
    public int hashCode() {
        return this.id.hashCode();
    }

    @Override
    public String toString() {
        return "ScheduledTask(" + this.id + ")";
    }

    public static class Builder {

        private final String id;
        private AtlantisThreadPool executor = Atlantis.THREAD_POOL;
        private boolean async = true;
        private boolean repeats = false;
        private Duration duration = Duration.ZERO;
        private Consumer<Task> task = ___ -> {};

        private Builder(String id) {
            this.id = id;
        }

        public Builder executor(AtlantisThreadPool executor) {
            this.executor = executor;
            return this;
        }

        public Builder async(boolean async) {
            this.async = async;
            return this;
        }

        public Builder async() {
            return this.async(true);
        }

        public Builder sync() {
            return this.async(false);
        }

        public Builder instant() {
            this.duration = Duration.ZERO;
            return this;
        }

        public Builder ticks(long ticks) {
            return this.duration(ticks * 50L, ChronoUnit.MILLIS);
        }

        public Builder milliseconds(long milliseconds) {
            return this.duration(milliseconds, ChronoUnit.MILLIS);
        }

        public Builder seconds(long seconds) {
            return this.duration(seconds, ChronoUnit.SECONDS);
        }

        public Builder minutes(long minutes) {
            return this.duration(minutes, ChronoUnit.MINUTES);
        }

        public Builder duration(long duration, TimeUnit unit) {
            return this.duration(TimeUnit.MILLISECONDS.convert(duration, unit), ChronoUnit.MILLIS);
        }

        public Builder duration(long duration, TemporalUnit unit) {
            this.duration = Duration.of(duration, unit);
            return this;
        }

        public Builder repeats(boolean repeats) {
            this.repeats = repeats;
            return this;
        }

        public Builder repeating() {
            return this.repeats(true);
        }

        public Builder once() {
            return this.repeats(false);
        }

        public Builder task(Consumer<Task> task) {
            this.task = task;
            return this;
        }

        public Builder task(Runnable task) {
            return this.task(___ -> task.run());
        }

        public Builder andThen(Consumer<Task> task) {
            this.task = this.task.andThen(task);
            return this;
        }

        public Builder andThen(Runnable task) {
            return this.andThen(___ -> task.run());
        }

        public Task build() {
            return new Task(this.id, this.executor, this.repeats, this.async, this.duration, this.task);
        }

        public Task schedule() {
            return this.build().schedule();
        }
    }
}
