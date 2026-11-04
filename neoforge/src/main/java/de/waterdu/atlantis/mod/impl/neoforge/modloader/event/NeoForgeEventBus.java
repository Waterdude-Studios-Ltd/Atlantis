package de.waterdu.atlantis.mod.impl.neoforge.modloader.event;

import de.waterdu.atlantis.mod.Mods;
import de.waterdu.atlantis.mod.object.ModObjectKey;
import de.waterdu.atlantis.mod.wrapper.modloader.event.UEventBus;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.IEventBus;

import java.util.function.Consumer;

/**
 * Wraps a NeoForge {@link IEventBus}.
 */
public class NeoForgeEventBus extends UEventBus<IEventBus, Event> {

    public static final ModObjectKey TYPE = new ModObjectKey(Mods.NEOFORGE_ID, "event_bus");

    /**
     * Creates a wrapper for the event bus.
     *
     * @param bus the event bus to wrap
     */
    public NeoForgeEventBus(IEventBus bus) {
        super(TYPE, bus);
    }

    /**
     * Adds a listener to the NeoForge bus.
     *
     * @param clazz the class of the event
     * @param callback the callback to run when the bus posts the event
     * @param <E> the type of the event
     */
    @Override
    public <E extends Event> void listen(Class<E> clazz, Consumer<E> callback) {
        this.dereference().addListener(clazz, callback);
    }

    /**
     * Posts the event on the NeoForge bus.
     *
     * @param event the event to post. It must be a NeoForge {@link Event}.
     * @return the event that the bus returns
     */
    @Override
    public Event post(Object event) {
        return this.dereference().post((Event) event);
    }

    /**
     * Registers the listener object on the NeoForge bus.
     *
     * @param listener the listener object
     */
    @Override
    public void register(Object listener) {
        this.dereference().register(listener);
    }

    /**
     * Unregisters the listener object from the NeoForge bus.
     *
     * @param listener the listener object
     */
    @Override
    public void unregister(Object listener) {
        this.dereference().unregister(listener);
    }
}
