package de.waterdu.atlantis.mod.impl.common.event.pokemon;

import de.waterdu.atlantis.mod.impl.common.event.Cancellable;
import de.waterdu.atlantis.mod.wrapper.modloader.event.UEvent;
import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.UPokemon;
import de.waterdu.atlantis.util.config.Key;
import net.minecraft.server.level.ServerPlayer;
import org.jspecify.annotations.Nullable;

public abstract class EvolveEventPayload extends PokemonEventPayload {

    protected EvolveEventPayload(Object source, @Nullable ServerPlayer player, UPokemon<?> pokemon) {
        super(source, player, pokemon);
    }

    /**
     * A Pokémon is about to evolve. {@link #pokemon()} is still the unevolved Pokémon.
     */
    public static class Pre extends EvolveEventPayload implements Cancellable {

        private boolean cancelled = false;

        public Pre(Object source, @Nullable ServerPlayer player, UPokemon<?> pokemon) {
            super(source, player, pokemon);
        }

        @Override
        public Key<?> type() {
            return UEvent.POKEMON_EVOLVE_PRE;
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

    /**
     * A Pokémon has evolved. {@link #pokemon()} is the evolved Pokémon.
     */
    public static class Post extends EvolveEventPayload {

        public Post(Object source, @Nullable ServerPlayer player, UPokemon<?> pokemon) {
            super(source, player, pokemon);
        }

        @Override
        public Key<?> type() {
            return UEvent.POKEMON_EVOLVE_POST;
        }
    }
}
