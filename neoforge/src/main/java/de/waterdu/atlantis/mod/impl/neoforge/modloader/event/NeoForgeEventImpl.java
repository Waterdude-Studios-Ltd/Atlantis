package de.waterdu.atlantis.mod.impl.neoforge.modloader.event;

import de.waterdu.atlantis.mod.impl.common.event.EventPayload;
import de.waterdu.atlantis.mod.wrapper.modloader.event.UEvent;
import de.waterdu.atlantis.util.config.Key;
import net.neoforged.bus.api.Event;

public class NeoForgeEventImpl<Payload extends EventPayload> extends Event {

    private final Key<UEvent<Payload>> type;
    private final Payload payload;

    public NeoForgeEventImpl(Key<UEvent<Payload>> type, Payload payload) {
        this.type = type;
        this.payload = payload;
    }

    public Key<UEvent<Payload>> type() {
        return this.type;
    }

    public Payload payload() {
        return this.payload;
    }
}
