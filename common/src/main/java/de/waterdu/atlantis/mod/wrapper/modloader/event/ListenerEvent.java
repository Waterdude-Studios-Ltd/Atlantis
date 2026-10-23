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

public final class ListenerEvent<Payload extends EventPayload> extends UEvent<Payload> {

    public static final ModObjectKey TYPE = new ModObjectKey(Atlantis.ID, "event");

    private static final Map<Key<?>, ListenerEvent<?>> EVENTS = new ConcurrentHashMap<>();

    private final List<Listener<Payload>> listeners = new CopyOnWriteArrayList<>();

    private ListenerEvent(Key<UEvent<Payload>> eventType) {
        super(TYPE, eventType);
    }

    @SuppressWarnings("unchecked")
    public static <Payload extends EventPayload> ListenerEvent<Payload> of(Key<UEvent<Payload>> eventType) {
        return (ListenerEvent<Payload>) EVENTS.computeIfAbsent(eventType, __ -> new ListenerEvent<>(eventType));
    }

    @VisibleForTesting
    public static void clearForTesting() {
        EVENTS.clear();
    }

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

    @Override
    public synchronized void listen(Priority priority, Function<Payload, InteractionResult> handler) {
        this.listeners.add(new Listener<>(priority, handler));
        this.listeners.sort(Comparator.comparing(Listener::priority));
    }

    private record Listener<Payload>(Priority priority, Function<Payload, InteractionResult> handler) {}
}
