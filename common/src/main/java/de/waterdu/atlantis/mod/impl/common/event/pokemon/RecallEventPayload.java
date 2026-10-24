package de.waterdu.atlantis.mod.impl.common.event.pokemon;

import de.waterdu.atlantis.mod.wrapper.modloader.event.UEvent;
import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.UPokemon;
import de.waterdu.atlantis.util.config.Key;
import net.minecraft.server.level.ServerPlayer;
import org.jspecify.annotations.Nullable;

public class RecallEventPayload extends PokemonEventPayload {

    public RecallEventPayload(Object source, @Nullable ServerPlayer player, UPokemon<?> pokemon) {
        super(source, player, pokemon);
    }

    @Override
    public Key<?> type() {
        return UEvent.POKEMON_RECALL;
    }
}
