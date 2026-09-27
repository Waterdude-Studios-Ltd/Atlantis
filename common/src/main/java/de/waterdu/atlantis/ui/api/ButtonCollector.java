package de.waterdu.atlantis.ui.api;

import com.google.common.collect.Sets;
import de.waterdu.atlantis.Atlantis;
import de.waterdu.atlantis.AtlantisLogger;
import de.waterdu.atlantis.util.concurrency.AtlantisThreadPool;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.Semaphore;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class ButtonCollector {

    private static final AtlantisThreadPool THREAD_POOL = AtlantisThreadPool.createNewThreadPool(Atlantis.NAME + "-ButtonCollector", 2, true);

    private final Set<Button> buttons;
    private final Consumer<Button> callback;

    private final Set<ScheduledFuture<?>> futures;

    private final Semaphore semaphore = new Semaphore(0);

    private boolean unlocked = false;

    private ButtonCollector(Consumer<Button> callback) {
        this.buttons = Sets.newConcurrentHashSet();
        this.callback = callback;
        this.futures = new HashSet<>();
    }

    public static ButtonCollector of(Consumer<Button> callback) {
        return new ButtonCollector(callback);
    }

    public void add(Button button) {
        this.collect(button);
    }

    public void addAll(Collection<Button> buttons) {
        this.collect(buttons);
    }

    public void addAll(Button... buttons) {
        this.collect(buttons);
    }

    @SafeVarargs
    public final void build(Consumer<Button.Builder>... builders) {
        for (var consumer : builders) {
            var builder = Button.builder();
            consumer.accept(builder);
            this.collect(builder);
        }
    }

    public void collect(Button button) {
        this.buttons.add(button);
        this.invokeCallback(button);
    }

    public void collect(Collection<Button> buttons) {
        for (var button : buttons) {
            this.buttons.add(button);
            this.invokeCallback(button);
        }
    }

    public void collect(Button... buttons) {
        for (var button : buttons) {
            this.buttons.add(button);
            this.invokeCallback(button);
        }
    }

    public void collect(Button.Builder builder) {
        this.collect(builder.build());
    }

    public void collect(Button.Builder... builders) {
        for (var builder : builders) {
            this.collect(builder);
        }
    }

    public void collectLater(Supplier<Button> supplier) {
        this.futures.add(THREAD_POOL.submit(() -> {
            this.semaphore.acquireUninterruptibly();
            this.collect(supplier.get());
            this.semaphore.release();
        }));
    }

    private void invokeCallback(Button button) {
        this.callback.accept(button);
    }

    public void unlock() {
        this.semaphore.release();
        this.unlocked = true;
    }

    public boolean isUnlocked() {
        return this.unlocked;
    }

    public Set<Button> gatherNow() {
        return Sets.newHashSet(this.buttons);
    }

    public Set<Button> gather() {
        for (var future : this.futures) {
            try {
                future.get();
            } catch (InterruptedException | ExecutionException e) {
                AtlantisLogger.warn("Button future failed exceptionally: {}", e.getMessage());
            }
        }
        return this.gatherNow();
    }
}
