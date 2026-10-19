package de.waterdu.atlantis.util.event;

import de.waterdu.atlantis.mod.Mods;
import de.waterdu.atlantis.mod.wrapper.modloader.event.UEventBus;

import java.util.HashMap;
import java.util.Map;

public final class MultiBusListener {

    private final Map<UEventBus<?, ?>, SingleBusListener> listeners;

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

        private final UEventBus<?, ?> bus;

        public SingleBusListener(UEventBus<?, ?> bus) {
            this.bus = bus;
        }

        public final UEventBus<?, ?> bus() {
            return this.bus;
        }

        public final void register() {
            this.bus.register(this);
        }

        public final void unregister() {
            this.bus.unregister(this);
        }
    }

    public abstract static class GameBusListener extends SingleBusListener {

        public GameBusListener() {
            super(Mods.modLoader().eventBus());
        }
    }

    public abstract static class ModBusListener extends SingleBusListener {

        public ModBusListener() {
            super(Mods.modLoader().modEventBus());
        }
    }
}
