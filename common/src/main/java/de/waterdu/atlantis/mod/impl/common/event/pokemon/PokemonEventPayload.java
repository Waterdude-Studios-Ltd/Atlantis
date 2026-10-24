package de.waterdu.atlantis.mod.impl.common.event.pokemon;

import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.UPokemon;
import net.minecraft.server.level.ServerPlayer;
import org.jspecify.annotations.Nullable;

public abstract class PokemonEventPayload extends PokemonModEventPayload {

    private final @Nullable ServerPlayer player;
    private final UPokemon<?> pokemon;

    protected PokemonEventPayload(Object source, @Nullable ServerPlayer player, UPokemon<?> pokemon) {
        super(source);
        this.player = player;
        this.pokemon = pokemon;
    }

    public @Nullable ServerPlayer player() {
        return this.player;
    }

    public UPokemon<?> pokemon() {
        return this.pokemon;
    }
}
