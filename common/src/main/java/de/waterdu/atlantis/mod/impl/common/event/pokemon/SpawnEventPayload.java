package de.waterdu.atlantis.mod.impl.common.event.pokemon;

import de.waterdu.atlantis.mod.impl.common.event.Cancellable;
import de.waterdu.atlantis.mod.wrapper.modloader.event.UEvent;
import de.waterdu.atlantis.mod.wrapper.pokemon.entity.UPokemonEntity;
import de.waterdu.atlantis.util.config.Key;

/**
 * Holds the data of {@link UEvent#POKEMON_SPAWN}, which fires when a Pokémon entity spawns. {@link #player()} is
 * always {@code null}.
 */
public class SpawnEventPayload extends PokemonEventPayload implements Cancellable {

    private final UPokemonEntity<?> entity;
    private boolean cancelled = false;

    /**
     * Creates the payload for a spawning Pokémon. The Pokémon of the payload is the Pokémon of the entity.
     *
     * @param source the event of the Pokémon mod that this payload wraps
     * @param entity the entity that spawns
     */
    public SpawnEventPayload(Object source, UPokemonEntity<?> entity) {
        super(source, null, entity.pokemon());
        this.entity = entity;
    }

    /**
     * @return {@link UEvent#POKEMON_SPAWN}
     */
    @Override
    public Key<?> type() {
        return UEvent.POKEMON_SPAWN;
    }

    /**
     * @return the entity that spawns
     */
    public UPokemonEntity<?> entity() {
        return this.entity;
    }

    /**
     * Cancels the event. The Pokémon mod then cancels its own event, and the entity does not spawn.
     */
    @Override
    public void cancel() {
        this.cancelled = true;
    }

    /**
     * @return {@code true} if the event is cancelled
     */
    @Override
    public boolean cancelled() {
        return this.cancelled;
    }
}
