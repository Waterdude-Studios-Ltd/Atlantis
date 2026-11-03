package de.waterdu.atlantis.util.event;

import de.waterdu.atlantis.mod.wrapper.modloader.event.UEventBus;

import java.util.HashSet;
import java.util.Set;

/**
 * Holds listeners until a {@link UEventBus} is set, then registers them on it.
 */
public final class DeferredModEventBus {

    private final Set<Object> toRegister = new HashSet<>();
    private UEventBus<?, ?> bus = null;

    /**
     * If the bus is not set yet, keeps the listener and registers it in {@link #setBus(UEventBus)}.
     */
    public void register(Object listener) {
        if (this.bus == null) {
            this.toRegister.add(listener);
        } else {
            this.bus.register(listener);
        }
    }

    /**
     * Unregisters the listener from the bus. If the bus is not set yet, removes the listener from the kept listeners.
     *
     * @param listener the listener to unregister
     */
    public void unregister(Object listener) {
        if (this.bus == null) {
            this.toRegister.remove(listener);
        } else {
            this.bus.unregister(listener);
        }
    }

    /**
     * @return the event. If the bus is not set yet, the event is not posted and is returned unchanged.
     */
    @SuppressWarnings("unchecked")
    public <T> T post(T event) {
        if (this.bus != null) {
            return (T) this.bus.post(event);
        }
        return event;
    }

    /**
     * Sets the bus and registers all kept listeners on it.
     *
     * @param bus the bus to use
     */
    public void setBus(UEventBus<?, ?> bus) {
        this.bus = bus;
        for (var listener : this.toRegister) {
            this.bus.register(listener);
        }
        this.toRegister.clear();
    }

    /**
     * @return the bus, or {@code null} if it is not set yet
     */
    public UEventBus<?, ?> getBus() {
        return this.bus;
    }
}
