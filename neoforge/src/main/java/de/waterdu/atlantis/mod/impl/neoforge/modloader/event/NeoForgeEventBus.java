package de.waterdu.atlantis.mod.impl.neoforge.modloader.event;

import de.waterdu.atlantis.mod.Mods;
import de.waterdu.atlantis.mod.object.ModObjectKey;
import de.waterdu.atlantis.mod.wrapper.modloader.event.UEventBus;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.IEventBus;

import java.util.function.Consumer;

public class NeoForgeEventBus extends UEventBus<IEventBus, Event> {

    public static final ModObjectKey TYPE = new ModObjectKey(Mods.NEOFORGE_ID, "event_bus");

    public NeoForgeEventBus(IEventBus bus) {
        super(TYPE, bus);
    }

    @Override
    public <E extends Event> void listen(Class<E> clazz, Consumer<E> callback) {
        this.dereference().addListener(clazz, callback);
    }

    @Override
    public Event post(Object event) {
        return this.dereference().post((Event) event);
    }

    @Override
    public void register(Object listener) {
        this.dereference().register(listener);
    }

    @Override
    public void unregister(Object listener) {
        this.dereference().unregister(listener);
    }
}
