package de.waterdu.atlantis.mod.impl.common.event.pokemon;

import de.waterdu.atlantis.mod.wrapper.modloader.event.UEvent;
import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.UPokemon;
import de.waterdu.atlantis.util.config.Key;
import net.minecraft.server.level.ServerPlayer;
import org.jspecify.annotations.Nullable;

import java.util.UUID;

/**
 * Holds the data of {@link UEvent#POKEMON_RECEIVED}, which fires when a player receives a Pokémon.
 */
public class ReceivedEventPayload extends PokemonEventPayload {

    public static final String UNKNOWN_CAUSE = "unknown";

    private final UUID playerUUID;
    private final String cause;

    /**
     * Creates the payload for a received Pokémon.
     *
     * @param source the event of the Pokémon mod that this payload wraps
     * @param playerUUID the UUID of the player who receives the Pokémon
     * @param player the player who receives the Pokémon, or {@code null} if there is none
     * @param pokemon the received Pokémon
     * @param cause how the Pokémon was received, or {@link #UNKNOWN_CAUSE}
     */
    public ReceivedEventPayload(Object source, UUID playerUUID, @Nullable ServerPlayer player, UPokemon<?> pokemon, String cause) {
        super(source, player, pokemon);
        this.playerUUID = playerUUID;
        this.cause = cause;
    }

    /**
     * @return {@link UEvent#POKEMON_RECEIVED}
     */
    @Override
    public Key<?> type() {
        return UEvent.POKEMON_RECEIVED;
    }

    /**
     * @return the UUID of the player who receives the Pokémon, also when {@link #player()} is {@code null}
     */
    public UUID playerUUID() {
        return this.playerUUID;
    }

    /**
     * How the Pokémon was received, as the Pokémon mod names it, or {@link #UNKNOWN_CAUSE} if the mod does not say.
     */
    public String cause() {
        return this.cause;
    }
}
