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

class ListenerEventTest {

    private static <P extends EventPayload> Key<UEvent<P>> freshKey() {
        return Key.of("atlantis_test", "event/" + UUID.randomUUID());
    }

    @Test
    void sameKeyReturnsSameEvent() {
        Key<UEvent<Plain>> key = freshKey();

        assertThat(ListenerEvent.of(key)).isSameAs(ListenerEvent.of(key));
        assertThat(ListenerEvent.of(key).eventType()).isEqualTo(key);
    }

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

    @Test
    void emitReturnsTheSamePayload() {
        Key<UEvent<Plain>> key = freshKey();
        var payload = new Plain(key);

        assertThat(ListenerEvent.of(key).emit(payload)).isSameAs(payload);
    }

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

    private static InteractionResult pass(List<String> order, String name) {
        order.add(name);
        return InteractionResult.PASS;
    }

    private record Plain(Key<?> type) implements EventPayload {}

    private static final class Cancelable implements EventPayload, Cancellable {

        private final Key<?> type;
        private boolean cancelled;

        private Cancelable(Key<?> type) {
            this.type = type;
        }

        @Override
        public Key<?> type() {
            return this.type;
        }

        @Override
        public void cancel() {
            this.cancelled = true;
        }

        @Override
        public boolean cancelled() {
            return this.cancelled;
        }
    }
}
