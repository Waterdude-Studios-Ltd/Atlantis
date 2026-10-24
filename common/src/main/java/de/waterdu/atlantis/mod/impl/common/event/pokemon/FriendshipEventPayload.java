package de.waterdu.atlantis.mod.impl.common.event.pokemon;

import de.waterdu.atlantis.mod.wrapper.modloader.event.UEvent;
import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.UPokemon;
import de.waterdu.atlantis.util.config.Key;
import org.jspecify.annotations.Nullable;
import net.minecraft.server.level.ServerPlayer;

public class FriendshipEventPayload extends PokemonEventPayload {

    private final int oldFriendship;
    private final int newFriendship;

    public FriendshipEventPayload(Object source, @Nullable ServerPlayer player, UPokemon<?> pokemon, int oldFriendship, int newFriendship) {
        super(source, player, pokemon);
        this.oldFriendship = oldFriendship;
        this.newFriendship = newFriendship;
    }

    @Override
    public Key<?> type() {
        return UEvent.POKEMON_FRIENDSHIP_CHANGE;
    }

    public int oldFriendship() {
        return this.oldFriendship;
    }

    public int newFriendship() {
        return this.newFriendship;
    }
}
