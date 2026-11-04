package de.waterdu.atlantis.mod.wrapper.modloader.event;

import com.google.common.annotations.VisibleForTesting;
import de.waterdu.atlantis.Atlantis;
import de.waterdu.atlantis.mod.impl.common.event.Cancellable;
import de.waterdu.atlantis.mod.impl.common.event.EventPayload;
import de.waterdu.atlantis.mod.object.ModObjectKey;
import de.waterdu.atlantis.util.config.Key;
import net.minecraft.world.InteractionResult;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;

/**
 * Keeps a list of listeners for one event type and calls them when the event is emitted. Both loaders use this
 * implementation.
 *
 * @param <Payload> the type of the event payload
 */
public final class ListenerEvent<Payload extends EventPayload> extends UEvent<Payload> {

    public static final ModObjectKey TYPE = new ModObjectKey(Atlantis.ID, "event");

    private static final Map<Key<?>, ListenerEvent<?>> EVENTS = new ConcurrentHashMap<>();

    private final List<Listener<Payload>> listeners = new CopyOnWriteArrayList<>();

    /**
     * Creates an event with no listeners.
     *
     * @param eventType the key of the event type
     */
    private ListenerEvent(Key<UEvent<Payload>> eventType) {
        super(TYPE, eventType);
    }

    /**
     * Gets the event for the event type. Creates the event on the first call for the type.
     *
     * @param eventType the key of the event type
     * @param <Payload> the type of the event payload
     * @return the one event for the event type
     */
    @SuppressWarnings("unchecked")
    public static <Payload extends EventPayload> ListenerEvent<Payload> of(Key<UEvent<Payload>> eventType) {
        return (ListenerEvent<Payload>) EVENTS.computeIfAbsent(eventType, __ -> new ListenerEvent<>(eventType));
    }

    /**
     * Removes all events, together with their listeners. Use only in tests.
     */
    @VisibleForTesting
    public static void clearForTesting() {
        EVENTS.clear();
    }

    /**
     * Calls each listener in priority order. If the payload is {@link Cancellable}, a result other than
     * {@link InteractionResult#PASS} cancels it, and the loop stops before the next listener.
     *
     * @param payload the payload to give to the listeners
     * @return the same payload
     */
    @Override
    public Payload emit(Payload payload) {
        var cancellable = payload instanceof Cancellable it ? it : null;
        for (var listener : this.listeners) {
            if (cancellable != null && cancellable.cancelled()) {
                break;
            }
            if (listener.handler().apply(payload) != InteractionResult.PASS && cancellable != null) {
                cancellable.cancel();
            }
        }
        return payload;
    }

    /**
     * Adds the listener and sorts the list by priority. The sort is stable, so listeners with the same priority keep
     * the order that they were added in.
     *
     * @param priority the priority of the listener
     * @param handler the handler to call when the event is emitted
     */
    @Override
    public synchronized void listen(Priority priority, Function<Payload, InteractionResult> handler) {
        this.listeners.add(new Listener<>(priority, handler));
        this.listeners.sort(Comparator.comparing(Listener::priority));
    }

    /**
     * Holds one listener of the event.
     *
     * @param priority the priority of the listener
     * @param handler the handler to call when the event is emitted
     * @param <Payload> the type of the event payload
     */
    private record Listener<Payload>(Priority priority, Function<Payload, InteractionResult> handler) {}
}
