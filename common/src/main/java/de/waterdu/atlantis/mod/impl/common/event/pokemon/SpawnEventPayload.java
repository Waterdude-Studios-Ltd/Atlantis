package de.waterdu.atlantis.mod.impl.common.event.pokemon;

import de.waterdu.atlantis.mod.impl.common.event.Cancellable;
import de.waterdu.atlantis.mod.wrapper.modloader.event.UEvent;
import de.waterdu.atlantis.mod.wrapper.pokemon.entity.UPokemonEntity;
import de.waterdu.atlantis.util.config.Key;

public class SpawnEventPayload extends PokemonEventPayload implements Cancellable {

    private final UPokemonEntity<?> entity;
    private boolean cancelled = false;

    public SpawnEventPayload(Object source, UPokemonEntity<?> entity) {
        super(source, null, entity.pokemon());
        this.entity = entity;
    }

    @Override
    public Key<?> type() {
        return UEvent.POKEMON_SPAWN;
    }

    public UPokemonEntity<?> entity() {
        return this.entity;
    }

    @Override
    public void cancel() {
        this.cancelled = true;
    }

    @Override
    public boolean cancelled() {
        return this.cancelled;
    }
}
