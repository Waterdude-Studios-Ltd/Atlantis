package de.waterdu.atlantis.mod.impl.common.event.pokemon;

import de.waterdu.atlantis.mod.impl.common.event.Cancellable;
import de.waterdu.atlantis.mod.wrapper.modloader.event.UEvent;
import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.UPokemon;
import de.waterdu.atlantis.util.config.Key;
import net.minecraft.server.level.ServerPlayer;
import org.jspecify.annotations.Nullable;

/**
 * Holds the data of {@link UEvent#POKEMON_NICKNAME}, which fires when a player sets the nickname of a Pokémon.
 */
public class NicknameEventPayload extends PokemonEventPayload implements Cancellable {

    private @Nullable String nickname;
    private boolean cancelled = false;

    /**
     * Creates the payload for a nickname change.
     *
     * @param source the event of the Pokémon mod that this payload wraps
     * @param player the player who sets the nickname
     * @param pokemon the Pokémon that gets the nickname
     * @param nickname the new nickname, or {@code null} if there is none
     */
    public NicknameEventPayload(Object source, ServerPlayer player, UPokemon<?> pokemon, @Nullable String nickname) {
        super(source, player, pokemon);
        this.nickname = nickname;
    }

    /**
     * @return {@link UEvent#POKEMON_NICKNAME}
     */
    @Override
    public Key<?> type() {
        return UEvent.POKEMON_NICKNAME;
    }

    /**
     * @return the new nickname, or {@code null} if there is none
     */
    public @Nullable String nickname() {
        return this.nickname;
    }

    /**
     * @param nickname the new nickname. Null or an empty string removes the nickname.
     */
    public void setNickname(@Nullable String nickname) {
        this.nickname = nickname;
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
