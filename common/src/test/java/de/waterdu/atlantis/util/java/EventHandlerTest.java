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

/**
 * Tests how {@link EventHandler} adds, dispatches and clears event listeners.
 */
class EventHandlerTest {

    private static final Key<UEvent<Payload>> FIRST = Key.of("atlantis_test", "event_handler/first");
    private static final Key<UEvent<Payload>> SECOND = Key.of("atlantis_test", "event_handler/second");

    /**
     * Carries a string value for an event.
     *
     * @param type the key of the event
     * @param value the value that the listeners record
     */
    private record Payload(Key<?> type, String value) implements EventPayload {}

    /**
     * Records the values that its listeners handle.
     */
    private static final class Handler extends EventHandler<Handler> {

        final List<String> handled = new ArrayList<>();
        boolean playersValid = true;

        /**
         * Checks the {@code playersValid} flag, and ignores the player.
         *
         * @param player the player to check
         * @return the value of {@code playersValid}
         */
        @Override
        public boolean playerValid(Player player) {
            return this.playersValid;
        }
    }

    /**
     * Checks that a payload goes only to the listeners of its event key, in the order they were added.
     */
    @Test
    void dispatchesByEventKeyInRegistrationOrder() {
        var handler = new Handler();
        handler.addEventHandler(FIRST, (payload, self) -> self.handled.add("a:" + payload.value()));
        handler.addEventHandler(FIRST, (payload, self) -> self.handled.add("b:" + payload.value()));
        handler.addEventHandler(SECOND, (payload, self) -> self.handled.add("second"));

        handler.handleEvent(new Payload(FIRST, "x"));

        assertThat(handler.handled).containsExactly("a:x", "b:x");
    }

    /**
     * Checks that listeners can be added and cleared with an event instead of its key.
     */
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

    /**
     * Checks that a payload for an event without listeners does nothing.
     */
    @Test
    void unknownEventsAreIgnored() {
        var handler = new Handler();

        handler.handleEvent(new Payload(SECOND, "nothing"));

        assertThat(handler.handled).isEmpty();
    }

    /**
     * Checks that a listener with a player getter runs only when {@code playerValid} returns {@code true}.
     */
    @Test
    void playerGetterGatesListeners() {
        var handler = new Handler();
        handler.addEventHandler(FIRST, (payload, self) -> self.handled.add(payload.value()), payload -> null);

        handler.handleEvent(new Payload(FIRST, "valid"));
        handler.playersValid = false;
        handler.handleEvent(new Payload(FIRST, "invalid"));

        assertThat(handler.handled).containsExactly("valid");
    }

    /**
     * Checks that cleared listeners of an event key no longer run.
     */
    @Test
    void clearEventHandlers() {
        var handler = new Handler();
        handler.addEventHandler(FIRST, (payload, self) -> self.handled.add("removed"));

        handler.clearEventHandlers(FIRST);
        handler.handleEvent(new Payload(FIRST, "x"));

        assertThat(handler.handled).isEmpty();
    }
}
