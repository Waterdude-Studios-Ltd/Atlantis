package de.waterdu.atlantis.mod.impl.common.event.pokemon;

import de.waterdu.atlantis.mod.wrapper.modloader.event.UEvent;
import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.UPokemon;
import de.waterdu.atlantis.util.config.Key;
import net.minecraft.server.level.ServerPlayer;
import org.jspecify.annotations.Nullable;

import java.util.UUID;

public class ReceivedEventPayload extends PokemonEventPayload {

    public static final String UNKNOWN_CAUSE = "unknown";

    private final UUID playerUUID;
    private final String cause;

    public ReceivedEventPayload(Object source, UUID playerUUID, @Nullable ServerPlayer player, UPokemon<?> pokemon, String cause) {
        super(source, player, pokemon);
        this.playerUUID = playerUUID;
        this.cause = cause;
    }

    @Override
    public Key<?> type() {
        return UEvent.POKEMON_RECEIVED;
    }

    public UUID playerUUID() {
        return this.playerUUID;
    }

    /**
     * How the Pokémon was received, as named by the Pokémon mod, or {@link #UNKNOWN_CAUSE} if the mod does not say.
     */
    public String cause() {
        return this.cause;
    }
}
