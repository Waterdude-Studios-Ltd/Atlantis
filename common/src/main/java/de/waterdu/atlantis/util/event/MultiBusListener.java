package de.waterdu.atlantis.util.event;

import de.waterdu.atlantis.Atlantis;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;

import java.util.HashMap;
import java.util.Map;

public final class MultiBusListener {

    private final Map<IEventBus, SingleBusListener> listeners;

    public MultiBusListener() {
        this.listeners = new HashMap<>();
    }

    public MultiBusListener add(SingleBusListener listener) {
        this.listeners.put(listener.bus(), listener);
        return this;
    }

    public MultiBusListener register() {
        this.listeners.values().forEach(SingleBusListener::register);
        return this;
    }

    public MultiBusListener unregister() {
        this.listeners.values().forEach(SingleBusListener::unregister);
        return this;
    }

    public abstract static class SingleBusListener {

        private final IEventBus bus;

        public SingleBusListener(IEventBus bus) {
            this.bus = bus;
        }

        public final IEventBus bus() {
            return this.bus;
        }

        public final void register() {
            this.bus.register(this);
        }

        public final void unregister() {
            this.bus.unregister(this);
        }
    }

    public abstract static class NeoForgeListener extends SingleBusListener {

        public NeoForgeListener() {
            super(NeoForge.EVENT_BUS);
        }
    }

    public abstract static class AtlantisListener extends SingleBusListener {

        public AtlantisListener() {
            super(Atlantis.EVENT_BUS);
        }
    }
}
