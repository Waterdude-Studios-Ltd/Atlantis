package de.waterdu.atlantis.util.java;

import de.waterdu.atlantis.mod.impl.common.event.EventPayload;
import de.waterdu.atlantis.mod.wrapper.modloader.event.ListenerEvent;
import de.waterdu.atlantis.mod.wrapper.modloader.event.UEvent;
import de.waterdu.atlantis.util.config.Key;
import net.minecraft.world.entity.player.Player;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class EventHandlerTest {

    private static final Key<UEvent<Payload>> FIRST = Key.of("atlantis_test", "event_handler/first");
    private static final Key<UEvent<Payload>> SECOND = Key.of("atlantis_test", "event_handler/second");

    private record Payload(Key<?> type, String value) implements EventPayload {}

    private static final class Handler extends EventHandler<Handler> {

        final List<String> handled = new ArrayList<>();
        boolean playersValid = true;

        @Override
        public boolean playerValid(Player player) {
            return this.playersValid;
        }
    }

    @Test
    void dispatchesByEventKeyInRegistrationOrder() {
        var handler = new Handler();
        handler.addEventHandler(FIRST, (payload, self) -> self.handled.add("a:" + payload.value()));
        handler.addEventHandler(FIRST, (payload, self) -> self.handled.add("b:" + payload.value()));
        handler.addEventHandler(SECOND, (payload, self) -> self.handled.add("second"));

        handler.handleEvent(new Payload(FIRST, "x"));

        assertThat(handler.handled).containsExactly("a:x", "b:x");
    }

    @Test
    void acceptsAnEventInsteadOfAKey() {
        var handler = new Handler();
        var event = ListenerEvent.of(FIRST);
        handler.addEventHandler(event, (payload, self) -> self.handled.add(payload.value()));

        handler.handleEvent(new Payload(FIRST, "event"));
        handler.clearEventHandlers(event);
        handler.handleEvent(new Payload(FIRST, "cleared"));

        assertThat(handler.handled).containsExactly("event");
    }

    @Test
    void unknownEventsAreIgnored() {
        var handler = new Handler();

        handler.handleEvent(new Payload(SECOND, "nothing"));

        assertThat(handler.handled).isEmpty();
    }

    @Test
    void playerGetterGatesListeners() {
        var handler = new Handler();
        handler.addEventHandler(FIRST, (payload, self) -> self.handled.add(payload.value()), payload -> null);

        handler.handleEvent(new Payload(FIRST, "valid"));
        handler.playersValid = false;
        handler.handleEvent(new Payload(FIRST, "invalid"));

        assertThat(handler.handled).containsExactly("valid");
    }

    @Test
    void clearEventHandlers() {
        var handler = new Handler();
        handler.addEventHandler(FIRST, (payload, self) -> self.handled.add("removed"));

        handler.clearEventHandlers(FIRST);
        handler.handleEvent(new Payload(FIRST, "x"));

        assertThat(handler.handled).isEmpty();
    }
}
