package de.waterdu.atlantis.util.event;

import de.waterdu.atlantis.mod.object.ModObjectKey;
import de.waterdu.atlantis.mod.wrapper.modloader.event.UEventBus;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

class DeferredModEventBusTest {

    private static final class RecordingBus extends UEventBus<Object, Object> {

        final List<String> calls = new ArrayList<>();

        RecordingBus() {
            super(new ModObjectKey("atlantis_test", "recording_bus"), null);
        }

        @Override
        public <E> void listen(Class<E> clazz, Consumer<E> callback) {}

        @Override
        public Object post(Object event) {
            this.calls.add("post " + event);
            return "posted " + event;
        }

        @Override
        public void register(Object listener) {
            this.calls.add("register " + listener);
        }

        @Override
        public void unregister(Object listener) {
            this.calls.add("unregister " + listener);
        }
    }

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

    @Test
    void postWithoutABusReturnsTheEvent() {
        assertThat(new DeferredModEventBus().post("event")).isEqualTo("event");
    }

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
