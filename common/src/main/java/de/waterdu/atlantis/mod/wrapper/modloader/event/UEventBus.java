package de.waterdu.atlantis.mod.wrapper.modloader.event;

import de.waterdu.atlantis.Atlantis;
import de.waterdu.atlantis.mod.object.ModObject;
import de.waterdu.atlantis.mod.object.ModObjectKey;
import de.waterdu.atlantis.mod.wrapper.ModWrapper;

import java.util.function.Consumer;

/**
 * Wraps the event bus of a loader.
 *
 * @param <Bus> the type of the wrapped event bus
 * @param <Event> the base type of the events on the bus
 */
public abstract class UEventBus<Bus, Event> extends ModWrapper<Bus> {

    /**
     * Creates a wrapper for the event bus.
     *
     * @param type the type of the wrapper
     * @param bus the event bus to wrap
     */
    protected UEventBus(ModObjectKey type, Bus bus) {
        super(new ModObject(type, bus));
    }

    /**
     * Adds a listener for one event class.
     *
     * @param clazz the class of the event
     * @param callback the callback to run when the bus posts the event
     * @param <E> the type of the event
     */
    public abstract <E extends Event> void listen(Class<E> clazz, Consumer<E> callback);

    /**
     * Posts the event on the bus.
     *
     * @param event the event to post
     * @return the event that the bus returns
     */
    public abstract Event post(Object event);

    /**
     * Registers an object whose methods listen for events.
     *
     * @param listener the listener object
     */
    public abstract void register(Object listener);

    /**
     * Unregisters an object that {@link #register} registered.
     *
     * @param listener the listener object
     */
    public abstract void unregister(Object listener);

    /**
     * Ignores all listeners and events. Fabric uses it, because Fabric has no event bus.
     */
    public static class SinkEventBus extends UEventBus<Object, Object> {

        public static final SinkEventBus INSTANCE = new SinkEventBus();
        public static final ModObjectKey TYPE = new ModObjectKey(Atlantis.ID, "sink_event_bus");

        /**
         * Creates the bus. Use {@link #INSTANCE}.
         */
        private SinkEventBus() {
            super(TYPE, null);
        }

        /**
         * Does nothing.
         *
         * @param clazz the class of the event
         * @param callback the callback, which never runs
         * @param <E> the type of the event
         */
        @Override
        public <E> void listen(Class<E> clazz, Consumer<E> callback) {
            // noop
        }

        /**
         * Does not post the event.
         *
         * @param event the event
         * @return the same event
         */
        @Override
        public Object post(Object event) {
            return event;
        }

        /**
         * Does nothing.
         *
         * @param listener the listener object
         */
        @Override
        public void register(Object listener) {
            // noop
        }

        /**
         * Does nothing.
         *
         * @param listener the listener object
         */
        @Override
        public void unregister(Object listener) {
            // noop
        }
    }
}
