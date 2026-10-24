package de.waterdu.atlantis.mod.impl.common.event.pokemon;

import de.waterdu.atlantis.mod.impl.common.event.Cancellable;
import de.waterdu.atlantis.mod.wrapper.modloader.event.UEvent;
import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.UPokemon;
import de.waterdu.atlantis.util.config.Key;
import net.minecraft.server.level.ServerPlayer;

public class StarterChosenEventPayload extends PokemonEventPayload implements Cancellable {

    private boolean cancelled = false;

    public StarterChosenEventPayload(Object source, ServerPlayer player, UPokemon<?> pokemon) {
        super(source, player, pokemon);
    }

    @Override
    public Key<?> type() {
        return UEvent.POKEMON_STARTER_CHOSEN;
    }

    @Override
    public void cancel() {
        this.cancelled = true;
    }

    @Override
    public boolean cancelled() {
        return this.cancelled;
    }
}
