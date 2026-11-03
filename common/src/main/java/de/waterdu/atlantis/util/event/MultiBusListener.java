package de.waterdu.atlantis.util.event;

import de.waterdu.atlantis.mod.Mods;
import de.waterdu.atlantis.mod.wrapper.modloader.event.UEventBus;

import java.util.HashMap;
import java.util.Map;

/**
 * Groups listeners for different event buses, so that they register and unregister together. It keeps one listener
 * per bus.
 */
public final class MultiBusListener {

    private final Map<UEventBus<?, ?>, SingleBusListener> listeners;

    /**
     * Creates a group with no listeners.
     */
    public MultiBusListener() {
        this.listeners = new HashMap<>();
    }

    /**
     * Adds the listener. It replaces the previous listener for the same bus. It does not register the listener.
     *
     * @param listener the listener to add
     * @return this group
     */
    public MultiBusListener add(SingleBusListener listener) {
        this.listeners.put(listener.bus(), listener);
        return this;
    }

    /**
     * Registers each listener on its bus.
     *
     * @return this group
     */
    public MultiBusListener register() {
        this.listeners.values().forEach(SingleBusListener::register);
        return this;
    }

    /**
     * Unregisters each listener from its bus.
     *
     * @return this group
     */
    public MultiBusListener unregister() {
        this.listeners.values().forEach(SingleBusListener::unregister);
        return this;
    }

    /**
     * Listens for events on one event bus.
     */
    public abstract static class SingleBusListener {

        private final UEventBus<?, ?> bus;

        /**
         * @param bus the bus that this listener registers on
         */
        public SingleBusListener(UEventBus<?, ?> bus) {
            this.bus = bus;
        }

        /**
         * @return the bus that this listener registers on
         */
        public final UEventBus<?, ?> bus() {
            return this.bus;
        }

        /**
         * Registers this listener on its bus.
         */
        public final void register() {
            this.bus.register(this);
        }

        /**
         * Unregisters this listener from its bus.
         */
        public final void unregister() {
            this.bus.unregister(this);
        }
    }

    /**
     * Listens for events on the game event bus of the loader.
     */
    public abstract static class GameBusListener extends SingleBusListener {

        /**
         * Creates a listener for {@link de.waterdu.atlantis.mod.proxy.delegate.ModLoaderDelegate#eventBus()}.
         */
        public GameBusListener() {
            super(Mods.modLoader().eventBus());
        }
    }

    /**
     * Listens for events on the mod event bus of the loader.
     */
    public abstract static class ModBusListener extends SingleBusListener {

        /**
         * Creates a listener for {@link de.waterdu.atlantis.mod.proxy.delegate.ModLoaderDelegate#modEventBus()}.
         */
        public ModBusListener() {
            super(Mods.modLoader().modEventBus());
        }
    }
}
