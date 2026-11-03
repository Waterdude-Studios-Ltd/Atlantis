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

/**
 * Collects the buttons of a page. Each collected button goes to a callback at once.
 */
public class ButtonCollector {

    private static final AtlantisThreadPool THREAD_POOL = AtlantisThreadPool.createNewThreadPool(Atlantis.NAME + "-ButtonCollector", 2, true);

    private final Set<Button> buttons;
    private final Consumer<Button> callback;

    private final Set<ScheduledFuture<?>> futures;

    private final Semaphore semaphore = new Semaphore(0);

    private boolean unlocked = false;

    /**
     * Creates a locked collector with no buttons.
     *
     * @param callback receives each button when it is collected
     */
    private ButtonCollector(Consumer<Button> callback) {
        this.buttons = Sets.newConcurrentHashSet();
        this.callback = callback;
        this.futures = new HashSet<>();
    }

    /**
     * Creates a locked collector with no buttons.
     *
     * @param callback receives each button when it is collected
     * @return the new collector
     */
    public static ButtonCollector of(Consumer<Button> callback) {
        return new ButtonCollector(callback);
    }

    /**
     * Collects a button. This is the same as {@link #collect(Button)}.
     *
     * @param button the button to collect
     */
    public void add(Button button) {
        this.collect(button);
    }

    /**
     * Collects each button in the collection. This is the same as {@link #collect(Collection)}.
     *
     * @param buttons the buttons to collect
     */
    public void addAll(Collection<Button> buttons) {
        this.collect(buttons);
    }

    /**
     * Collects each given button. This is the same as {@link #collect(Button...)}.
     *
     * @param buttons the buttons to collect
     */
    public void addAll(Button... buttons) {
        this.collect(buttons);
    }

    /**
     * Creates a new {@link Button.Builder} for each consumer, lets the consumer configure it, then collects the
     * built button.
     *
     * @param builders the consumers that configure each button
     */
    @SafeVarargs
    public final void build(Consumer<Button.Builder>... builders) {
        for (var consumer : builders) {
            var builder = Button.builder();
            consumer.accept(builder);
            this.collect(builder);
        }
    }

    /**
     * Adds the button to the collected buttons and sends it to the callback.
     *
     * @param button the button to collect
     */
    public void collect(Button button) {
        this.buttons.add(button);
        this.invokeCallback(button);
    }

    /**
     * Adds each button in the collection to the collected buttons and sends it to the callback.
     *
     * @param buttons the buttons to collect
     */
    public void collect(Collection<Button> buttons) {
        for (var button : buttons) {
            this.buttons.add(button);
            this.invokeCallback(button);
        }
    }

    /**
     * Adds each given button to the collected buttons and sends it to the callback.
     *
     * @param buttons the buttons to collect
     */
    public void collect(Button... buttons) {
        for (var button : buttons) {
            this.buttons.add(button);
            this.invokeCallback(button);
        }
    }

    /**
     * Builds the button and collects it.
     *
     * @param builder the builder of the button to collect
     */
    public void collect(Button.Builder builder) {
        this.collect(builder.build());
    }

    /**
     * Builds each button and collects it.
     *
     * @param builders the builders of the buttons to collect
     */
    public void collect(Button.Builder... builders) {
        for (var builder : builders) {
            this.collect(builder);
        }
    }

    /**
     * Runs the supplier on a separate thread pool after {@link #unlock()}. The button is collected when the supplier
     * completes. This can be after the page opens.
     */
    public void collectLater(Supplier<Button> supplier) {
        this.futures.add(THREAD_POOL.submit(() -> {
            this.semaphore.acquireUninterruptibly();
            this.collect(supplier.get());
            this.semaphore.release();
        }));
    }

    /**
     * Sends the button to the callback.
     *
     * @param button the collected button
     */
    private void invokeCallback(Button button) {
        this.callback.accept(button);
    }

    /**
     * Lets the {@link #collectLater(Supplier)} tasks run. Atlantis calls this after the page adds its buttons.
     */
    public void unlock() {
        this.semaphore.release();
        this.unlocked = true;
    }

    /**
     * Checks if {@link #unlock()} was called.
     *
     * @return {@code true} if the collector is unlocked
     */
    public boolean isUnlocked() {
        return this.unlocked;
    }

    /**
     * Gets the buttons that are collected now. It does not wait for the {@link #collectLater(Supplier)} tasks.
     *
     * @return a new set with the collected buttons
     */
    public Set<Button> gatherNow() {
        return Sets.newHashSet(this.buttons);
    }

    /**
     * Waits for all {@link #collectLater(Supplier)} tasks to complete. Do not call it before {@link #unlock()},
     * because the tasks cannot run until then.
     */
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
