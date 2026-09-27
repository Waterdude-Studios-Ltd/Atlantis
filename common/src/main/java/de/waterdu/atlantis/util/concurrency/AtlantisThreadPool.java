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

public class AtlantisThreadPool {

    private static final Set<AtlantisThreadPool> ALL_POOLS = new HashSet<>();

    private final ScheduledThreadPoolExecutor pool;
    private boolean keepAlive = true;
    private boolean catchErrors = true;

    private AtlantisThreadPool(String modid, int size, boolean daemon) {
        this.pool = (ScheduledThreadPoolExecutor) Executors.newScheduledThreadPool(size, new Factory(modid, daemon));
    }

    public static AtlantisThreadPool createNewThreadPool(String name, int size, boolean daemon) {
        AtlantisThreadPool pool = new AtlantisThreadPool(name, size, daemon);
        AtlantisThreadPool.ALL_POOLS.add(pool);
        return pool;
    }

    public static void shutdownAll() {
        for (AtlantisThreadPool pool : AtlantisThreadPool.ALL_POOLS) {
            if (!pool.keepAlive) {
                pool.pool.shutdown();
            }
        }
    }

    public AtlantisThreadPool keepAlive(boolean keepAlive) {
        this.keepAlive = keepAlive;
        return this;
    }

    public AtlantisThreadPool setSize(final int size) {
        this.pool.setCorePoolSize(size);
        return this;
    }

    public AtlantisThreadPool shouldCatchErrors(boolean catchErrors) {
        this.catchErrors = catchErrors;
        return this;
    }

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

    public ScheduledFuture<?> submit(final Runnable runnable) {
        return (ScheduledFuture<?>) this.pool.submit(this.encapsulateRunnable(runnable));
    }

    public <T> ScheduledFuture<T> submit(final Callable<T> callable, final T fallback) {
        return (ScheduledFuture<T>) this.pool.submit(this.encapsulateCallable(callable, fallback));
    }

    public ScheduledFuture<?> schedule(final Runnable runnable, final long delay) {
        return this.schedule(runnable, delay, TimeUnit.MILLISECONDS);
    }

    public ScheduledFuture<?> schedule(final Runnable runnable, final long delay, final TimeUnit timeUnit) {
        return this.pool.schedule(this.encapsulateRunnable(runnable), delay, timeUnit);
    }

    public ScheduledFuture<?> scheduleAtFixedRate(final Runnable runnable, final long delay, final long period) {
        return this.scheduleAtFixedRate(runnable, delay, period, TimeUnit.SECONDS);
    }

    public ScheduledFuture<?> scheduleAtFixedRate(final Runnable runnable, final long delay, final long period, final TimeUnit timeUnit) {
        return this.pool.scheduleAtFixedRate(this.encapsulateRunnable(runnable), delay, period, timeUnit);
    }

    public ScheduledFuture<?> repeatUntilComplete(final Callable<Boolean> task, final long period) {
        return this.repeatUntilComplete(task, period, TimeUnit.MILLISECONDS);
    }

    public ScheduledThreadPoolExecutor getPool() {
        return this.pool;
    }

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

    private static class Factory implements ThreadFactory {

        private final ThreadGroup group;
        private final AtomicInteger threadNumber = new AtomicInteger(1);
        private final String namePrefix;
        private final boolean daemon;

        Factory(String name, boolean daemon) {
            this.group = new ThreadGroup(Atlantis.ID);
            this.namePrefix = (name + " Scheduled Thread Pool");
            this.daemon = daemon;
        }

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
