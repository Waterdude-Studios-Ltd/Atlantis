package de.waterdu.atlantis.mod.impl.common.event.pokemon;

import de.waterdu.atlantis.mod.impl.common.event.Cancellable;
import de.waterdu.atlantis.mod.wrapper.modloader.event.UEvent;
import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.UPokemon;
import de.waterdu.atlantis.util.config.Key;
import net.minecraft.server.level.ServerPlayer;
import org.jspecify.annotations.Nullable;

/**
 * Base class of the payloads for the evolution of a Pokémon.
 */
public abstract class EvolveEventPayload extends PokemonEventPayload {

    /**
     * Creates the payload for an evolution event.
     *
     * @param source the event of the Pokémon mod that this payload wraps
     * @param player the owner of the Pokémon, or {@code null} if there is none
     * @param pokemon the Pokémon that evolves
     */
    protected EvolveEventPayload(Object source, @Nullable ServerPlayer player, UPokemon<?> pokemon) {
        super(source, player, pokemon);
    }

    /**
     * A Pokémon is about to evolve. {@link #pokemon()} is still the unevolved Pokémon.
     */
    public static class Pre extends EvolveEventPayload implements Cancellable {

        private boolean cancelled = false;

        /**
         * Creates the payload for a Pokémon that is about to evolve.
         *
         * @param source the event of the Pokémon mod that this payload wraps
         * @param player the owner of the Pokémon, or {@code null} if there is none
         * @param pokemon the unevolved Pokémon
         */
        public Pre(Object source, @Nullable ServerPlayer player, UPokemon<?> pokemon) {
            super(source, player, pokemon);
        }

        /**
         * @return {@link UEvent#POKEMON_EVOLVE_PRE}
         */
        @Override
        public Key<?> type() {
            return UEvent.POKEMON_EVOLVE_PRE;
        }

        /**
         * Cancels the event. The Pokémon mod then cancels its own event, and the Pokémon does not evolve.
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

    /**
     * A Pokémon has evolved. {@link #pokemon()} is the evolved Pokémon.
     */
    public static class Post extends EvolveEventPayload {

        /**
         * Creates the payload for a Pokémon that has evolved.
         *
         * @param source the event of the Pokémon mod that this payload wraps
         * @param player the owner of the Pokémon, or {@code null} if there is none
         * @param pokemon the evolved Pokémon
         */
        public Post(Object source, @Nullable ServerPlayer player, UPokemon<?> pokemon) {
            super(source, player, pokemon);
        }

        /**
         * @return {@link UEvent#POKEMON_EVOLVE_POST}
         */
        @Override
        public Key<?> type() {
            return UEvent.POKEMON_EVOLVE_POST;
        }
    }
}
