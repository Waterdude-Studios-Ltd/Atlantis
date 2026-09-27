package de.waterdu.atlantis.mod.wrapper.modloader.event;

import de.waterdu.atlantis.Atlantis;
import de.waterdu.atlantis.mod.object.ModObject;
import de.waterdu.atlantis.mod.object.ModObjectKey;
import de.waterdu.atlantis.mod.wrapper.ModWrapper;

import java.util.function.Consumer;

public abstract class UEventBus<Bus, Event> extends ModWrapper<Bus> {

    protected UEventBus(ModObjectKey type, Bus bus) {
        super(new ModObject(type, bus));
    }

    public abstract <E extends Event> void listen(Class<E> clazz, Consumer<E> callback);

    public abstract Event post(Object event);

    public static class SinkEventBus extends UEventBus<Object, Object> {

        public static final SinkEventBus INSTANCE = new SinkEventBus();
        public static final ModObjectKey TYPE = new ModObjectKey(Atlantis.ID, "sink_event_bus");

        private SinkEventBus() {
            super(TYPE, null);
        }

        @Override
        public <E> void listen(Class<E> clazz, Consumer<E> callback) {
            // noop
        }

        @Override
        public Object post(Object event) {
            return event;
        }
    }
}
