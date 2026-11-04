package de.waterdu.atlantis.mod.impl.common.event.pokemon;

import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.UPokemon;
import net.minecraft.server.level.ServerPlayer;
import org.jspecify.annotations.Nullable;

/**
 * Base class of the Pokémon mod payloads that are about one Pokémon and, if there is one, a player.
 */
public abstract class PokemonEventPayload extends PokemonModEventPayload {

    private final @Nullable ServerPlayer player;
    private final UPokemon<?> pokemon;

    /**
     * Creates the payload for an event about one Pokémon.
     *
     * @param source the event of the Pokémon mod that this payload wraps
     * @param player the player of the event, or {@code null} if there is none
     * @param pokemon the Pokémon of the event
     */
    protected PokemonEventPayload(Object source, @Nullable ServerPlayer player, UPokemon<?> pokemon) {
        super(source);
        this.player = player;
        this.pokemon = pokemon;
    }

    /**
     * @return the player of the event, or {@code null} if there is none
     */
    public @Nullable ServerPlayer player() {
        return this.player;
    }

    /**
     * @return the Pokémon of the event
     */
    public UPokemon<?> pokemon() {
        return this.pokemon;
    }
}
