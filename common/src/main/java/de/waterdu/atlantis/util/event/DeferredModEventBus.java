package de.waterdu.atlantis.util.event;

import de.waterdu.atlantis.mod.wrapper.modloader.event.UEventBus;

import java.util.HashSet;
import java.util.Set;

public final class DeferredModEventBus {

    private final Set<Object> toRegister = new HashSet<>();
    private UEventBus<?, ?> bus = null;

    public void register(Object listener) {
        if (this.bus == null) {
            this.toRegister.add(listener);
        } else {
            this.bus.register(listener);
        }
    }

    public void unregister(Object listener) {
        if (this.bus == null) {
            this.toRegister.remove(listener);
        } else {
            this.bus.unregister(listener);
        }
    }

    @SuppressWarnings("unchecked")
    public <T> T post(T event) {
        if (this.bus != null) {
            return (T) this.bus.post(event);
        }
        return event;
    }

    public void setBus(UEventBus<?, ?> bus) {
        this.bus = bus;
        for (var listener : this.toRegister) {
            this.bus.register(listener);
        }
        this.toRegister.clear();
    }

    public UEventBus<?, ?> getBus() {
        return this.bus;
    }
}
