package de.waterdu.atlantis.mod.impl.common.event.pokemon;

import de.waterdu.atlantis.mod.wrapper.modloader.event.UEvent;
import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.UPokemon;
import de.waterdu.atlantis.util.config.Key;
import org.jspecify.annotations.Nullable;
import net.minecraft.server.level.ServerPlayer;

/**
 * Holds the data of {@link UEvent#POKEMON_FRIENDSHIP_CHANGE}, which fires when the friendship of a Pokémon changes.
 */
public class FriendshipEventPayload extends PokemonEventPayload {

    private final int oldFriendship;
    private final int newFriendship;

    /**
     * Creates the payload for a friendship change.
     *
     * @param source the event of the Pokémon mod that this payload wraps
     * @param player the owner of the Pokémon, or {@code null} if there is none
     * @param pokemon the Pokémon whose friendship changes
     * @param oldFriendship the friendship before the change
     * @param newFriendship the friendship after the change
     */
    public FriendshipEventPayload(Object source, @Nullable ServerPlayer player, UPokemon<?> pokemon, int oldFriendship, int newFriendship) {
        super(source, player, pokemon);
        this.oldFriendship = oldFriendship;
        this.newFriendship = newFriendship;
    }

    /**
     * @return {@link UEvent#POKEMON_FRIENDSHIP_CHANGE}
     */
    @Override
    public Key<?> type() {
        return UEvent.POKEMON_FRIENDSHIP_CHANGE;
    }

    /**
     * @return the friendship before the change
     */
    public int oldFriendship() {
        return this.oldFriendship;
    }

    /**
     * @return the friendship after the change
     */
    public int newFriendship() {
        return this.newFriendship;
    }
}
