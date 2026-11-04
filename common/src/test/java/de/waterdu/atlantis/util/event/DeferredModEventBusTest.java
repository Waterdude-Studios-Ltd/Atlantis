package de.waterdu.atlantis.util.event;

import de.waterdu.atlantis.mod.object.ModObjectKey;
import de.waterdu.atlantis.mod.wrapper.modloader.event.UEventBus;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests how {@link DeferredModEventBus} buffers listeners and forwards calls to its bus.
 */
class DeferredModEventBusTest {

    /**
     * Records every post, register and unregister call as a string.
     */
    private static final class RecordingBus extends UEventBus<Object, Object> {

        final List<String> calls = new ArrayList<>();

        /**
         * Creates a recording bus with no loader bus.
         */
        RecordingBus() {
            super(new ModObjectKey("atlantis_test", "recording_bus"), null);
        }

        /**
         * Does nothing.
         *
         * @param clazz the type of the event
         * @param callback the listener of the event
         * @param <E> the type of the event
         */
        @Override
        public <E> void listen(Class<E> clazz, Consumer<E> callback) {}

        /**
         * Records the post.
         *
         * @param event the event to post
         * @return {@code posted } followed by the event
         */
        @Override
        public Object post(Object event) {
            this.calls.add("post " + event);
            return "posted " + event;
        }

        /**
         * Records the registration.
         *
         * @param listener the listener to register
         */
        @Override
        public void register(Object listener) {
            this.calls.add("register " + listener);
        }

        /**
         * Records the removal.
         *
         * @param listener the listener to unregister
         */
        @Override
        public void unregister(Object listener) {
            this.calls.add("unregister " + listener);
        }
    }

    /**
     * Checks that listeners registered before the bus is set are registered on it, without the ones that were
     * unregistered.
     */
    @Test
    void buffersListenersUntilTheBusIsSet() {
        var deferred = new DeferredModEventBus();
        var bus = new RecordingBus();
        deferred.register("a");
        deferred.register("b");
        deferred.unregister("b");

        assertThat(deferred.getBus()).isNull();
        deferred.setBus(bus);

        assertThat(deferred.getBus()).isSameAs(bus);
        assertThat(bus.calls).containsExactly("register a");
    }

    /**
     * Checks that register, unregister and post calls go directly to the bus after it is set.
     */
    @Test
    void forwardsOnceTheBusIsSet() {
        var deferred = new DeferredModEventBus();
        var bus = new RecordingBus();
        deferred.setBus(bus);

        deferred.register("a");
        deferred.unregister("a");
        String result = deferred.post("event");

        assertThat(result).isEqualTo("posted event");
        assertThat(bus.calls).containsExactly("register a", "unregister a", "post event");
    }

    /**
     * Checks that a post without a bus returns the event unchanged.
     */
    @Test
    void postWithoutABusReturnsTheEvent() {
        assertThat(new DeferredModEventBus().post("event")).isEqualTo("event");
    }

    /**
     * Checks that buffered listeners go only to the first bus that is set, and not to a later one.
     */
    @Test
    void bufferedListenersAreRegisteredOnlyOnce() {
        var deferred = new DeferredModEventBus();
        var first = new RecordingBus();
        var second = new RecordingBus();
        deferred.register("a");

        deferred.setBus(first);
        deferred.setBus(second);

        assertThat(first.calls).containsExactly("register a");
        assertThat(second.calls).isEmpty();
    }
}
