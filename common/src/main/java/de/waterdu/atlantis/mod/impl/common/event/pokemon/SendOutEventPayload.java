package de.waterdu.atlantis.mod.impl.common.event.pokemon;

import de.waterdu.atlantis.mod.impl.common.event.Cancellable;
import de.waterdu.atlantis.mod.wrapper.modloader.event.UEvent;
import de.waterdu.atlantis.mod.wrapper.pokemon.entity.UPokemonEntity;
import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.UPokemon;
import de.waterdu.atlantis.util.config.Key;
import net.minecraft.server.level.ServerPlayer;
import org.jspecify.annotations.Nullable;

/**
 * Base class of the payloads for a Pokémon that is sent out of its Poké Ball.
 */
public abstract class SendOutEventPayload extends PokemonEventPayload {

    /**
     * Creates the payload for a send out event.
     *
     * @param source the event of the Pokémon mod that this payload wraps
     * @param player the owner of the Pokémon, or {@code null} if there is none
     * @param pokemon the Pokémon that is sent out
     */
    protected SendOutEventPayload(Object source, @Nullable ServerPlayer player, UPokemon<?> pokemon) {
        super(source, player, pokemon);
    }

    /**
     * Holds the data of {@link UEvent#POKEMON_SEND_OUT_PRE}, which fires before a Pokémon is sent out.
     */
    public static class Pre extends SendOutEventPayload implements Cancellable {

        private boolean cancelled = false;

        /**
         * Creates the payload for a Pokémon that is about to be sent out.
         *
         * @param source the event of the Pokémon mod that this payload wraps
         * @param player the owner of the Pokémon, or {@code null} if there is none
         * @param pokemon the Pokémon that is sent out
         */
        public Pre(Object source, @Nullable ServerPlayer player, UPokemon<?> pokemon) {
            super(source, player, pokemon);
        }

        /**
         * @return {@link UEvent#POKEMON_SEND_OUT_PRE}
         */
        @Override
        public Key<?> type() {
            return UEvent.POKEMON_SEND_OUT_PRE;
        }

        /**
         * Cancels the event. The Pokémon mod then cancels its own event, and the Pokémon is not sent out.
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
     * Holds the data of {@link UEvent#POKEMON_SEND_OUT_POST}, which fires after a Pokémon is sent out.
     */
    public static class Post extends SendOutEventPayload {

        private final UPokemonEntity<?> entity;

        /**
         * Creates the payload for a Pokémon that has been sent out.
         *
         * @param source the event of the Pokémon mod that this payload wraps
         * @param player the owner of the Pokémon, or {@code null} if there is none
         * @param pokemon the Pokémon that is sent out
         * @param entity the entity of the Pokémon in the world
         */
        public Post(Object source, @Nullable ServerPlayer player, UPokemon<?> pokemon, UPokemonEntity<?> entity) {
            super(source, player, pokemon);
            this.entity = entity;
        }

        /**
         * @return {@link UEvent#POKEMON_SEND_OUT_POST}
         */
        @Override
        public Key<?> type() {
            return UEvent.POKEMON_SEND_OUT_POST;
        }

        /**
         * @return the entity of the Pokémon in the world
         */
        public UPokemonEntity<?> entity() {
            return this.entity;
        }
    }
}
