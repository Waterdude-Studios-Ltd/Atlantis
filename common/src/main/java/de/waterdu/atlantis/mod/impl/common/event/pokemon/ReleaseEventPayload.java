package de.waterdu.atlantis.mod.impl.common.event.pokemon;

import de.waterdu.atlantis.mod.wrapper.modloader.event.UEvent;
import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.UPokemon;
import de.waterdu.atlantis.util.config.Key;
import net.minecraft.server.level.ServerPlayer;

/**
 * Holds the data of {@link UEvent#POKEMON_RELEASE}, which fires when a player releases a Pokémon.
 */
public class ReleaseEventPayload extends PokemonEventPayload {

    /**
     * Creates the payload for a released Pokémon.
     *
     * @param source the event of the Pokémon mod that this payload wraps
     * @param player the player who releases the Pokémon
     * @param pokemon the released Pokémon
     */
    public ReleaseEventPayload(Object source, ServerPlayer player, UPokemon<?> pokemon) {
        super(source, player, pokemon);
    }

    /**
     * @return {@link UEvent#POKEMON_RELEASE}
     */
    @Override
    public Key<?> type() {
        return UEvent.POKEMON_RELEASE;
    }
}
