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

public abstract class EventHandler<Handler extends EventHandler<Handler>> {

    private final Map<Key<?>, List<EventConsumer<?, Handler>>> listeners;
    private final Map<Key<?>, Function<?, ? extends Player>> playerGetters;

    public EventHandler() {
        this.listeners = new HashMap<>();
        this.playerGetters = new HashMap<>();
    }

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

    public <Payload extends EventPayload> void addEventHandler(
            final Key<UEvent<Payload>> eventKey,
            final EventConsumer<Payload, Handler> listener
    ) {
        this.addEventHandler(eventKey, listener, null);
    }

    public <Payload extends EventPayload> void addEventHandler(
            final UEvent<Payload> event,
            final EventConsumer<Payload, Handler> listener,
            final @Nullable Function<Payload, ? extends Player> playerGetter
    ) {
        this.addEventHandler(event.eventType(), listener, playerGetter);
    }

    public <Payload extends EventPayload> void addEventHandler(
            final UEvent<Payload> event,
            final EventConsumer<Payload, Handler> listener
    ) {
        this.addEventHandler(event.eventType(), listener, null);
    }

    public <Payload extends EventPayload> void clearEventHandlers(final Key<UEvent<Payload>> eventKey) {
        this.listeners.computeIfAbsent(eventKey, key -> new ArrayList<>()).clear();
    }

    public <Payload extends EventPayload> void clearEventHandlers(final UEvent<Payload> event) {
        this.clearEventHandlers(event.eventType());
    }

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

    public boolean playerValid(Player player) {
        return true;
    }
}
