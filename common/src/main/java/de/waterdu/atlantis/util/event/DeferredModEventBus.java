package de.waterdu.atlantis.util.event;

import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.IEventBus;

import java.util.HashSet;
import java.util.Set;

public final class DeferredModEventBus {

    private final Set<Object> toRegister = new HashSet<>();
    private IEventBus bus = null;

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

    public <T extends Event> T post(T event) {
        if (this.bus != null) {
            return this.bus.post(event);
        }
        return event;
    }

    public void setBus(IEventBus bus) {
        this.bus = bus;
        for (var listener : this.toRegister) {
            this.bus.register(listener);
        }
        this.toRegister.clear();
    }

    public IEventBus getBus() {
        return this.bus;
    }
}
