package de.waterdu.atlantis.mod.impl.common.event.pokemon;

import de.waterdu.atlantis.mod.wrapper.modloader.event.UEvent;
import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.UPokemon;
import de.waterdu.atlantis.util.config.Key;
import net.minecraft.server.level.ServerPlayer;

/**
 * Holds the data of {@link UEvent#POKEMON_EGG_HATCH}, which fires when an egg hatches.
 */
public class EggHatchEventPayload extends PokemonEventPayload {

    /**
     * Creates the payload for a hatched egg.
     *
     * @param source the event of the Pokémon mod that this payload wraps
     * @param player the player who owns the egg
     * @param pokemon the Pokémon that hatches from the egg
     */
    public EggHatchEventPayload(Object source, ServerPlayer player, UPokemon<?> pokemon) {
        super(source, player, pokemon);
    }

    /**
     * @return {@link UEvent#POKEMON_EGG_HATCH}
     */
    @Override
    public Key<?> type() {
        return UEvent.POKEMON_EGG_HATCH;
    }
}
