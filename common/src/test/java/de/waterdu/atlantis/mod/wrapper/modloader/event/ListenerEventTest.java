package de.waterdu.atlantis.mod.wrapper.modloader.event;

import de.waterdu.atlantis.mod.impl.common.event.Cancellable;
import de.waterdu.atlantis.mod.impl.common.event.EventPayload;
import de.waterdu.atlantis.util.config.Key;
import net.minecraft.world.InteractionResult;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests the listener order, cancellation and registration of {@link ListenerEvent}.
 */
class ListenerEventTest {

    /**
     * Creates an event key with a random path, so that each test gets its own event.
     *
     * @param <P> the payload type of the event
     * @return a new key in the {@code atlantis_test} namespace
     */
    private static <P extends EventPayload> Key<UEvent<P>> freshKey() {
        return Key.of("atlantis_test", "event/" + UUID.randomUUID());
    }

    /**
     * Checks that the same key always gives the same event, and that the event type is that key.
     */
    @Test
    void sameKeyReturnsSameEvent() {
        Key<UEvent<Plain>> key = freshKey();

        assertThat(ListenerEvent.of(key)).isSameAs(ListenerEvent.of(key));
        assertThat(ListenerEvent.of(key).eventType()).isEqualTo(key);
    }

    /**
     * Checks that listeners run in the order of {@link UEvent.Priority}, not in the order they are added.
     */
    @Test
    void listenersRunInPriorityOrder() {
        Key<UEvent<Plain>> key = freshKey();
        var event = ListenerEvent.of(key);
        var order = new ArrayList<UEvent.Priority>();
        for (var priority : List.of(UEvent.Priority.LOWEST, UEvent.Priority.NORMAL, UEvent.Priority.HIGHEST, UEvent.Priority.LOW, UEvent.Priority.HIGH)) {
            event.listen(priority, __ -> {
                order.add(priority);
                return InteractionResult.PASS;
            });
        }

        event.emit(new Plain(key));

        assertThat(order).containsExactly(UEvent.Priority.values());
    }

    /**
     * Checks that a listener added without a priority runs between the high and the low listeners.
     */
    @Test
    void defaultPriorityIsNormal() {
        Key<UEvent<Plain>> key = freshKey();
        var event = ListenerEvent.of(key);
        var order = new ArrayList<String>();
        event.listen(UEvent.Priority.LOW, __ -> pass(order, "low"));
        event.listen(__ -> pass(order, "default"));
        event.listen(UEvent.Priority.HIGH, __ -> pass(order, "high"));

        event.emit(new Plain(key));

        assertThat(order).containsExactly("high", "default", "low");
    }

    /**
     * Checks that a listener that returns a result other than {@link InteractionResult#PASS} cancels a cancellable
     * payload, and that the remaining listeners do not run.
     */
    @Test
    void nonPassCancelsAndSkipsRemainingListeners() {
        Key<UEvent<Cancelable>> key = freshKey();
        var event = ListenerEvent.of(key);
        var order = new ArrayList<String>();
        event.listen(UEvent.Priority.HIGH, __ -> pass(order, "first"));
        event.listen(UEvent.Priority.NORMAL, __ -> {
            order.add("canceller");
            return InteractionResult.FAIL;
        });
        event.listen(UEvent.Priority.LOW, __ -> pass(order, "skipped"));

        var payload = event.emit(new Cancelable(key));

        assertThat(payload.cancelled()).isTrue();
        assertThat(order).containsExactly("first", "canceller");
    }

    /**
     * Checks that no listener runs when the payload is already cancelled before it is emitted.
     */
    @Test
    void alreadyCancelledPayloadRunsNoListeners() {
        Key<UEvent<Cancelable>> key = freshKey();
        var event = ListenerEvent.of(key);
        var order = new ArrayList<String>();
        event.listen(__ -> pass(order, "listener"));
        var payload = new Cancelable(key);
        payload.cancel();

        event.emit(payload);

        assertThat(order).isEmpty();
    }

    /**
     * Checks that a result other than {@link InteractionResult#PASS} does not skip listeners when the payload is not
     * cancellable.
     */
    @Test
    void nonPassOnNonCancellablePayloadRunsEveryListener() {
        Key<UEvent<Plain>> key = freshKey();
        var event = ListenerEvent.of(key);
        var order = new ArrayList<String>();
        event.listen(UEvent.Priority.HIGH, __ -> {
            order.add("success");
            return InteractionResult.SUCCESS;
        });
        event.listen(UEvent.Priority.LOW, __ -> pass(order, "after"));

        event.emit(new Plain(key));

        assertThat(order).containsExactly("success", "after");
    }

    /**
     * Checks that {@code emit} returns the payload instance that it got.
     */
    @Test
    void emitReturnsTheSamePayload() {
        Key<UEvent<Plain>> key = freshKey();
        var payload = new Plain(key);

        assertThat(ListenerEvent.of(key).emit(payload)).isSameAs(payload);
    }

    /**
     * Checks that no listener is lost when eight threads add listeners at the same time.
     *
     * @throws InterruptedException if the test thread is interrupted while it waits for the threads to stop
     */
    @Test
    void concurrentRegistrationKeepsEveryListener() throws InterruptedException {
        Key<UEvent<Plain>> key = freshKey();
        var event = ListenerEvent.of(key);
        var threads = 8;
        var perThread = 250;
        var start = new CountDownLatch(1);
        var calls = new java.util.concurrent.atomic.AtomicInteger();
        try (var executor = Executors.newFixedThreadPool(threads)) {
            for (var t = 0; t < threads; t++) {
                executor.submit(() -> {
                    start.await();
                    for (var i = 0; i < perThread; i++) {
                        event.listen(UEvent.Priority.values()[i % 5], __ -> {
                            calls.incrementAndGet();
                            return InteractionResult.PASS;
                        });
                    }
                    return null;
                });
            }
            start.countDown();
            executor.shutdown();
            assertThat(executor.awaitTermination(30, TimeUnit.SECONDS)).isTrue();
        }

        event.emit(new Plain(key));

        assertThat(calls).hasValue(threads * perThread);
    }

    /**
     * Adds the name to the order list.
     *
     * @param order the list that records the order in which listeners run
     * @param name the name of the listener
     * @return {@link InteractionResult#PASS}
     */
    private static InteractionResult pass(List<String> order, String name) {
        order.add(name);
        return InteractionResult.PASS;
    }

    /**
     * Is a payload that cannot be cancelled.
     *
     * @param type the key of the event
     */
    private record Plain(Key<?> type) implements EventPayload {}

    /**
     * Is a payload that can be cancelled.
     */
    private static final class Cancelable implements EventPayload, Cancellable {

        private final Key<?> type;
        private boolean cancelled;

        /**
         * Creates a payload that is not cancelled.
         *
         * @param type the key of the event
         */
        private Cancelable(Key<?> type) {
            this.type = type;
        }

        /**
         * @return the key of the event
         */
        @Override
        public Key<?> type() {
            return this.type;
        }

        /**
         * Marks this payload as cancelled.
         */
        @Override
        public void cancel() {
            this.cancelled = true;
        }

        /**
         * @return {@code true} if {@link #cancel()} was called
         */
        @Override
        public boolean cancelled() {
            return this.cancelled;
        }
    }
}
