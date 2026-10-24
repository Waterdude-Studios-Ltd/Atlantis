package de.waterdu.atlantis.mod.impl.common.event.pokemon;

import de.waterdu.atlantis.mod.wrapper.modloader.event.UEvent;
import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.UPokemon;
import de.waterdu.atlantis.util.config.Key;
import net.minecraft.server.level.ServerPlayer;

public class EggHatchEventPayload extends PokemonEventPayload {

    public EggHatchEventPayload(Object source, ServerPlayer player, UPokemon<?> pokemon) {
        super(source, player, pokemon);
    }

    @Override
    public Key<?> type() {
        return UEvent.POKEMON_EGG_HATCH;
    }
}
