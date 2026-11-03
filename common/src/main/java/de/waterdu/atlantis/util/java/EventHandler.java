package de.waterdu.atlantis.util.java;

import de.waterdu.atlantis.mod.impl.common.event.EventPayload;
import de.waterdu.atlantis.mod.wrapper.modloader.event.UEvent;
import de.waterdu.atlantis.util.config.Key;
import de.waterdu.atlantis.util.java.interfaces.EventConsumer;
import net.minecraft.world.entity.player.Player;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Holds listeners for events by event key and passes payloads to them in {@link #handleEvent(EventPayload)}.
 *
 * @param <Handler> the type of the subclass, which the listeners get with each payload
 */
public abstract class EventHandler<Handler extends EventHandler<Handler>> {

    private final Map<Key<?>, List<EventConsumer<?, Handler>>> listeners;
    private final Map<Key<?>, Function<?, ? extends Player>> playerGetters;

    /**
     * Creates a handler with no listeners.
     */
    public EventHandler() {
        this.listeners = new HashMap<>();
        this.playerGetters = new HashMap<>();
    }

    /**
     * @param playerGetter gets the player from the payload. Can be null. If set, the listeners of the event run only if
     *     {@link #playerValid(Player)} returns {@code true} for the player. It replaces the getter of earlier listeners
     *     of the event.
     */
    public <Payload extends EventPayload> void addEventHandler(
            final Key<UEvent<Payload>> eventKey,
            final EventConsumer<Payload, Handler> listener,
            final @Nullable Function<Payload, ? extends Player> playerGetter
    ) {
        this.listeners.computeIfAbsent(eventKey, __ -> new ArrayList<>()).add(listener);
        if (playerGetter != null) {
            this.playerGetters.put(eventKey, playerGetter);
        }
    }

    /**
     * Adds a listener for an event, with no player getter.
     *
     * @param <Payload> the payload type of the event
     * @param eventKey the key of the event
     * @param listener the listener to add
     */
    public <Payload extends EventPayload> void addEventHandler(
            final Key<UEvent<Payload>> eventKey,
            final EventConsumer<Payload, Handler> listener
    ) {
        this.addEventHandler(eventKey, listener, null);
    }

    /**
     * Adds a listener for an event, keyed by the event type of the event.
     *
     * @param <Payload> the payload type of the event
     * @param event the event
     * @param listener the listener to add
     * @param playerGetter gets the player from the payload. Can be null. See
     *     {@link #addEventHandler(Key, EventConsumer, Function)}.
     */
    public <Payload extends EventPayload> void addEventHandler(
            final UEvent<Payload> event,
            final EventConsumer<Payload, Handler> listener,
            final @Nullable Function<Payload, ? extends Player> playerGetter
    ) {
        this.addEventHandler(event.eventType(), listener, playerGetter);
    }

    /**
     * Adds a listener for an event, keyed by the event type of the event, with no player getter.
     *
     * @param <Payload> the payload type of the event
     * @param event the event
     * @param listener the listener to add
     */
    public <Payload extends EventPayload> void addEventHandler(
            final UEvent<Payload> event,
            final EventConsumer<Payload, Handler> listener
    ) {
        this.addEventHandler(event.eventType(), listener, null);
    }

    /**
     * Removes all listeners of an event. The player getter of the event stays.
     *
     * @param <Payload> the payload type of the event
     * @param eventKey the key of the event
     */
    public <Payload extends EventPayload> void clearEventHandlers(final Key<UEvent<Payload>> eventKey) {
        this.listeners.computeIfAbsent(eventKey, key -> new ArrayList<>()).clear();
    }

    /**
     * Removes all listeners of an event, keyed by the event type of the event.
     *
     * @param <Payload> the payload type of the event
     * @param event the event
     */
    public <Payload extends EventPayload> void clearEventHandlers(final UEvent<Payload> event) {
        this.clearEventHandlers(event.eventType());
    }

    /**
     * Passes the payload to each listener of its event type, in the order they were added. If the event has a player
     * getter, the listeners run only if {@link #playerValid(Player)} returns {@code true}.
     *
     * @param <Payload> the payload type of the event
     * @param payload the payload of the event
     */
    @SuppressWarnings("unchecked")
    public <Payload extends EventPayload> void handleEvent(final Payload payload) {
        var type = payload.type();
        var playerGetter = (Function<Payload, ? extends Player>) this.playerGetters.get(type);
        if (playerGetter == null || this.playerValid(playerGetter.apply(payload))) {
            var listeners = this.listeners.get(type);
            if (listeners != null) {
                for (var listener : listeners) {
                    ((EventConsumer<Payload, Handler>) listener).consume(payload, (Handler) this);
                }
            }
        }
    }

    /**
     * Decides if the listeners of an event that has a player getter run. The default accepts every player.
     */
    public boolean playerValid(Player player) {
        return true;
    }
}
