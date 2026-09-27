package de.waterdu.atlantis.mod.impl.common.event;

import de.waterdu.atlantis.util.config.Key;

public interface EventPayload {

    @SuppressWarnings("unchecked")
    default <Payload extends EventPayload> Payload payload() {
        return (Payload) this;
    }

    Key<?> type();
}
