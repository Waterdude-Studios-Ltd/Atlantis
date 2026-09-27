package de.waterdu.atlantis.util.java.interfaces;

import de.waterdu.atlantis.mod.impl.common.event.EventPayload;

@FunctionalInterface
public interface EventConsumer<Payload extends EventPayload, Handler> {

    void consume(Payload payload, Handler handler);
}
