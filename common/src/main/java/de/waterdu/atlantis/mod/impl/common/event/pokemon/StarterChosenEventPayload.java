package de.waterdu.atlantis.mod.impl.common.event.pokemon;

import de.waterdu.atlantis.mod.impl.common.event.Cancellable;
import de.waterdu.atlantis.mod.wrapper.modloader.event.UEvent;
import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.UPokemon;
import de.waterdu.atlantis.util.config.Key;
import net.minecraft.server.level.ServerPlayer;

/**
 * Holds the data of {@link UEvent#POKEMON_STARTER_CHOSEN}, which fires when a player chooses a starter Pokémon.
 */
public class StarterChosenEventPayload extends PokemonEventPayload implements Cancellable {

    private boolean cancelled = false;

    /**
     * Creates the payload for a chosen starter.
     *
     * @param source the event of the Pokémon mod that this payload wraps
     * @param player the player who chooses the starter
     * @param pokemon the chosen starter Pokémon
     */
    public StarterChosenEventPayload(Object source, ServerPlayer player, UPokemon<?> pokemon) {
        super(source, player, pokemon);
    }

    /**
     * @return {@link UEvent#POKEMON_STARTER_CHOSEN}
     */
    @Override
    public Key<?> type() {
        return UEvent.POKEMON_STARTER_CHOSEN;
    }

    /**
     * Cancels the event. The Pokémon mod then cancels its own event.
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
