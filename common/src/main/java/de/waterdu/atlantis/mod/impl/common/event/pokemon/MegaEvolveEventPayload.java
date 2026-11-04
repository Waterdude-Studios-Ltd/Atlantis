package de.waterdu.atlantis.mod.impl.common.event.pokemon;

import de.waterdu.atlantis.mod.wrapper.modloader.event.UEvent;
import de.waterdu.atlantis.mod.wrapper.pokemon.battle.UBattle;
import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.UPokemon;
import de.waterdu.atlantis.util.config.Key;
import net.minecraft.server.level.ServerPlayer;
import org.jspecify.annotations.Nullable;

/**
 * Holds the data of {@link UEvent#POKEMON_MEGA_EVOLVE}, which fires when a Pokémon mega evolves.
 */
public class MegaEvolveEventPayload extends PokemonEventPayload {

    private final @Nullable UBattle<?> battle;

    /**
     * Creates the payload for a Pokémon that mega evolves.
     *
     * @param source the event of the Pokémon mod that this payload wraps
     * @param player the player of the event, or {@code null} if there is none
     * @param pokemon the Pokémon that mega evolves
     * @param battle the battle in which the Pokémon mega evolves, or {@code null} if there is none
     */
    public MegaEvolveEventPayload(Object source, @Nullable ServerPlayer player, UPokemon<?> pokemon, @Nullable UBattle<?> battle) {
        super(source, player, pokemon);
        this.battle = battle;
    }

    /**
     * @return {@link UEvent#POKEMON_MEGA_EVOLVE}
     */
    @Override
    public Key<?> type() {
        return UEvent.POKEMON_MEGA_EVOLVE;
    }

    /**
     * @return the battle in which the Pokémon mega evolves, or {@code null} if there is none
     */
    public @Nullable UBattle<?> battle() {
        return this.battle;
    }
}
