package de.waterdu.atlantis.mod.impl.common.event.pokemon;

import de.waterdu.atlantis.mod.impl.common.event.EventPayload;

/**
 * Base class of the payloads that wrap an event of a Pokémon mod.
 */
public abstract class PokemonModEventPayload implements EventPayload {

    private final Object source;

    /**
     * Creates the payload for an event of the Pokémon mod.
     *
     * @param source the event of the Pokémon mod that this payload wraps
     */
    protected PokemonModEventPayload(Object source) {
        this.source = source;
    }

    /**
     * The event of the Pokémon mod that this payload wraps, for example a Pixelmon or Cobblemon event.
     */
    public Object source() {
        return this.source;
    }

    /**
     * {@link #source()} cast to {@code type}. The cast is not checked, so a wrong type fails where the result is used.
     */
    @SuppressWarnings("unchecked")
    public <T> T source(Class<T> type) {
        return (T) this.source;
    }
}
