package de.waterdu.atlantis.mod.impl.common.event.pokemon;

import de.waterdu.atlantis.mod.wrapper.modloader.event.UEvent;
import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.UPokemon;
import de.waterdu.atlantis.util.config.Key;
import net.minecraft.server.level.ServerPlayer;
import org.jspecify.annotations.Nullable;

/**
 * Holds the data of {@link UEvent#POKEMON_FAINT}, which fires after a Pokémon faints.
 */
public class FaintEventPayload extends PokemonEventPayload {

    /**
     * Creates the payload for a fainted Pokémon.
     *
     * @param source the event of the Pokémon mod that this payload wraps
     * @param player the owner of the Pokémon, or {@code null} if there is none
     * @param pokemon the Pokémon that faints
     */
    public FaintEventPayload(Object source, @Nullable ServerPlayer player, UPokemon<?> pokemon) {
        super(source, player, pokemon);
    }

    /**
     * @return {@link UEvent#POKEMON_FAINT}
     */
    @Override
    public Key<?> type() {
        return UEvent.POKEMON_FAINT;
    }
}
