package de.waterdu.atlantis.util.java.interfaces;

import de.waterdu.atlantis.mod.impl.common.event.EventPayload;

/**
 * Gives an event payload to a handler.
 *
 * @param <Payload> the type of the event payload
 * @param <Handler> the type of the handler
 */
@FunctionalInterface
public interface EventConsumer<Payload extends EventPayload, Handler> {

    /**
     * Gives the payload to the handler.
     *
     * @param payload the event payload
     * @param handler the handler that receives the payload
     */
    void consume(Payload payload, Handler handler);
}
