package de.waterdu.atlantis.mod.impl.common.event;

import de.waterdu.atlantis.util.config.Key;

/**
 * Holds the data of a loader-neutral event.
 */
public interface EventPayload {

    /**
     * Casts this payload to the type that the caller expects. The cast is not checked.
     */
    @SuppressWarnings("unchecked")
    default <Payload extends EventPayload> Payload payload() {
        return (Payload) this;
    }

    /**
     * @return the key of the event that this payload belongs to
     */
    Key<?> type();
}
