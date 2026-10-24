package de.waterdu.atlantis.mod.impl.common.event.pokemon;

import de.waterdu.atlantis.mod.impl.common.event.EventPayload;

public abstract class PokemonModEventPayload implements EventPayload {

    private final Object source;

    protected PokemonModEventPayload(Object source) {
        this.source = source;
    }

    public Object source() {
        return this.source;
    }

    @SuppressWarnings("unchecked")
    public <T> T source(Class<T> type) {
        return (T) this.source;
    }
}
