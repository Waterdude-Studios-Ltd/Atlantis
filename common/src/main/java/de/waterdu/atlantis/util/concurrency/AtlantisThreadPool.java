package de.waterdu.atlantis.util.concurrency;

import de.waterdu.atlantis.Atlantis;
import de.waterdu.atlantis.AtlantisLogger;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Wraps a scheduled thread pool that can log exceptions in tasks instead of rethrowing them.
 */
public class AtlantisThreadPool {

    private static final Set<AtlantisThreadPool> ALL_POOLS = new HashSet<>();

    private final ScheduledThreadPoolExecutor pool;
    private boolean keepAlive = true;
    private boolean catchErrors = true;

    /**
     * Creates a pool. Use {@link #createNewThreadPool(String, int, boolean)} so that {@link #shutdownAll()} knows the
     * pool.
     *
     * @param modId the prefix of the thread names
     * @param size the core number of threads
     * @param daemon if {@code true}, the threads are daemon threads
     */
    private AtlantisThreadPool(String modId, int size, boolean daemon) {
        this.pool = (ScheduledThreadPoolExecutor) Executors.newScheduledThreadPool(size, new Factory(modId, daemon));
    }

    /**
     * @param name the prefix of the thread names
     */
    public static AtlantisThreadPool createNewThreadPool(String name, int size, boolean daemon) {
        AtlantisThreadPool pool = new AtlantisThreadPool(name, size, daemon);
        AtlantisThreadPool.ALL_POOLS.add(pool);
        return pool;
    }

    /**
     * Shuts down every pool that has {@link #keepAlive(boolean)} set to {@code false}. Pools stay alive by default.
     */
    public static void shutdownAll() {
        for (AtlantisThreadPool pool : AtlantisThreadPool.ALL_POOLS) {
            if (!pool.keepAlive) {
                pool.pool.shutdown();
            }
        }
    }

    /**
     * @param keepAlive if {@code true}, {@link #shutdownAll()} does not shut down this pool. The default is
     *     {@code true}.
     */
    public AtlantisThreadPool keepAlive(boolean keepAlive) {
        this.keepAlive = keepAlive;
        return this;
    }

    /**
     * Sets the core number of threads.
     *
     * @param size the core number of threads
     * @return this pool
     */
    public AtlantisThreadPool setSize(final int size) {
        this.pool.setCorePoolSize(size);
        return this;
    }

    /**
     * @param catchErrors if {@code true}, exceptions in tasks are logged and not rethrown, and a callable gives its
     *     fallback. The default is {@code true}.
     */
    public AtlantisThreadPool shouldCatchErrors(boolean catchErrors) {
        this.catchErrors = catchErrors;
        return this;
    }

    /**
     * Wraps the runnable so that it logs exceptions, if the pool catches errors.
     *
     * @param runnable the runnable to wrap
     * @return the wrapped runnable, or the same runnable if the pool does not catch errors
     */
    private Runnable encapsulateRunnable(final Runnable runnable) {
        if (this.catchErrors) {
            return () -> {
                try {
                    runnable.run();
                } catch (Exception e) {
                    AtlantisLogger.error(Atlantis.ERROR_TEXT);
                    AtlantisLogger.error("Cause: {}", e.getMessage());
                    AtlantisLogger.debugBuilder().withThrowable(e).log();
                }
            };
        } else {
            return runnable;
        }
    }

    /**
     * Wraps the callable so that it logs exceptions and gives the fallback, if the pool catches errors.
     *
     * @param <T> the result type
     * @param callable the callable to wrap
     * @param fallback the result if the callable throws
     * @return the wrapped callable, or the same callable if the pool does not catch errors
     */
    private <T> Callable<T> encapsulateCallable(final Callable<T> callable, T fallback) {
        if (this.catchErrors) {
            return () -> {
                try {
                    return callable.call();
                } catch (Exception e) {
                    AtlantisLogger.error(Atlantis.ERROR_TEXT);
                    AtlantisLogger.error("Cause: {}", e.getMessage());
                    AtlantisLogger.debugBuilder().withThrowable(e).log();
                }
                return fallback;
            };
        } else {
            return callable;
        }
    }

    /**
     * Runs the runnable on the pool as soon as possible.
     *
     * @param runnable the runnable to run
     * @return the future of the runnable
     */
    public ScheduledFuture<?> submit(final Runnable runnable) {
        return (ScheduledFuture<?>) this.pool.submit(this.encapsulateRunnable(runnable));
    }

    /**
     * @param fallback the result if the callable throws and the pool catches errors
     */
    public <T> ScheduledFuture<T> submit(final Callable<T> callable, final T fallback) {
        return (ScheduledFuture<T>) this.pool.submit(this.encapsulateCallable(callable, fallback));
    }

    /**
     * @param delay the delay in milliseconds
     */
    public ScheduledFuture<?> schedule(final Runnable runnable, final long delay) {
        return this.schedule(runnable, delay, TimeUnit.MILLISECONDS);
    }

    /**
     * Runs the runnable on the pool after the delay.
     *
     * @param runnable the runnable to run
     * @param delay the delay
     * @param timeUnit the unit of the delay
     * @return the future of the runnable
     */
    public ScheduledFuture<?> schedule(final Runnable runnable, final long delay, final TimeUnit timeUnit) {
        return this.pool.schedule(this.encapsulateRunnable(runnable), delay, timeUnit);
    }

    /**
     * @param delay the delay in seconds
     * @param period the period in seconds
     */
    public ScheduledFuture<?> scheduleAtFixedRate(final Runnable runnable, final long delay, final long period) {
        return this.scheduleAtFixedRate(runnable, delay, period, TimeUnit.SECONDS);
    }

    /**
     * Runs the runnable on the pool after the delay, then again after each period.
     *
     * @param runnable the runnable to run
     * @param delay the delay before the first run
     * @param period the time between the starts of two runs
     * @param timeUnit the unit of the delay and the period
     * @return the future of the repetition
     */
    public ScheduledFuture<?> scheduleAtFixedRate(final Runnable runnable, final long delay, final long period, final TimeUnit timeUnit) {
        return this.pool.scheduleAtFixedRate(this.encapsulateRunnable(runnable), delay, period, timeUnit);
    }

    /**
     * @param period the period in milliseconds
     */
    public ScheduledFuture<?> repeatUntilComplete(final Callable<Boolean> task, final long period) {
        return this.repeatUntilComplete(task, period, TimeUnit.MILLISECONDS);
    }

    /**
     * @return the executor that this pool wraps
     */
    public ScheduledThreadPoolExecutor getPool() {
        return this.pool;
    }

    /**
     * Calls the task after each period until it returns {@code true}. An exception stops the repetition.
     *
     * @return the future of the first call only. If you cancel it after the first call, the repetition continues.
     */
    public ScheduledFuture<?> repeatUntilComplete(final Callable<Boolean> task, final long period, final TimeUnit timeUnit) {
        return this.schedule(() -> {
            try {
                if (!task.call()) {
                    this.repeatUntilComplete(task, period, timeUnit);
                }
            } catch (Exception e) {
                AtlantisLogger.error(Atlantis.ERROR_TEXT);
                AtlantisLogger.error("Cause: {}", e.getMessage());
                AtlantisLogger.debugBuilder().withThrowable(e).log();
            }
        }, period, timeUnit);
    }

    /**
     * Creates numbered threads that log uncaught exceptions.
     */
    private static class Factory implements ThreadFactory {

        private final ThreadGroup group;
        private final AtomicInteger threadNumber = new AtomicInteger(1);
        private final String namePrefix;
        private final boolean daemon;

        /**
         * Creates a factory with its own thread group.
         *
         * @param name the prefix of the thread names
         * @param daemon if {@code true}, the threads are daemon threads
         */
        Factory(String name, boolean daemon) {
            this.group = new ThreadGroup(Atlantis.ID);
            this.namePrefix = (name + " Scheduled Thread Pool");
            this.daemon = daemon;
        }

        /**
         * Creates a thread with the next worker number and normal priority. The thread logs uncaught exceptions.
         *
         * @param r the runnable that the thread runs
         * @return the new thread
         */
        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(this.group, r, this.namePrefix + " (worker " + this.threadNumber.getAndIncrement() + ")", 0);
            t.setDaemon(this.daemon);
            t.setUncaughtExceptionHandler((thread, e) -> {
                AtlantisLogger.error(Atlantis.ERROR_TEXT);
                AtlantisLogger.error("Cause: {}", e.getMessage());
                AtlantisLogger.debugBuilder().withThrowable(e).log();
            });
            if (t.getPriority() != Thread.NORM_PRIORITY) {
                t.setPriority(Thread.NORM_PRIORITY);
            }
            return t;
        }
    }
}
