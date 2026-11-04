package de.waterdu.atlantis.mod.impl.common.event.pokemon;

import de.waterdu.atlantis.mod.impl.common.event.Cancellable;
import de.waterdu.atlantis.mod.wrapper.modloader.event.UEvent;
import de.waterdu.atlantis.mod.wrapper.pokemon.entity.UPokemonEntity;
import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.UPokemon;
import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.properties.UPokeBall;
import de.waterdu.atlantis.util.config.Key;
import net.minecraft.server.level.ServerPlayer;
import org.jspecify.annotations.Nullable;

/**
 * Base class of the payloads for the capture of a Pokémon by a player.
 */
public abstract class CaptureEventPayload extends PokemonEventPayload {

    private final @Nullable UPokeBall<?> ball;
    private final @Nullable UPokemonEntity<?> entity;

    /**
     * Creates the payload for a capture event.
     *
     * @param source the event of the Pokémon mod that this payload wraps
     * @param player the player who tries to capture the Pokémon
     * @param pokemon the Pokémon to capture
     * @param ball the Poké Ball of the capture, or {@code null} if it is not known
     * @param entity the entity of the Pokémon, or {@code null} if it is not known
     */
    protected CaptureEventPayload(Object source, ServerPlayer player, UPokemon<?> pokemon, @Nullable UPokeBall<?> ball, @Nullable UPokemonEntity<?> entity) {
        super(source, player, pokemon);
        this.ball = ball;
        this.entity = entity;
    }

    /**
     * @return the Poké Ball of the capture, or {@code null} if it is not known
     */
    public @Nullable UPokeBall<?> ball() {
        return this.ball;
    }

    /**
     * @return the entity of the Pokémon, or {@code null} if it is not known
     */
    public @Nullable UPokemonEntity<?> entity() {
        return this.entity;
    }

    /**
     * Holds the data of {@link UEvent#POKEMON_CAPTURE_START}, which fires when a capture starts.
     */
    public static class Start extends CaptureEventPayload implements Cancellable {

        private boolean cancelled = false;

        /**
         * Creates the payload for a capture that starts.
         *
         * @param source the event of the Pokémon mod that this payload wraps
         * @param player the player who tries to capture the Pokémon
         * @param pokemon the Pokémon to capture
         * @param ball the Poké Ball of the capture, or {@code null} if it is not known
         * @param entity the entity of the Pokémon, or {@code null} if it is not known
         */
        public Start(Object source, ServerPlayer player, UPokemon<?> pokemon, @Nullable UPokeBall<?> ball, @Nullable UPokemonEntity<?> entity) {
            super(source, player, pokemon, ball, entity);
        }

        /**
         * @return {@link UEvent#POKEMON_CAPTURE_START}
         */
        @Override
        public Key<?> type() {
            return UEvent.POKEMON_CAPTURE_START;
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

    /**
     * Holds the data of {@link UEvent#POKEMON_CAPTURE_SUCCESS}, which fires when a capture succeeds.
     */
    public static class Success extends CaptureEventPayload {

        /**
         * Creates the payload for a capture that succeeds.
         *
         * @param source the event of the Pokémon mod that this payload wraps
         * @param player the player who captures the Pokémon
         * @param pokemon the captured Pokémon
         * @param ball the Poké Ball of the capture, or {@code null} if it is not known
         * @param entity the entity of the Pokémon, or {@code null} if it is not known
         */
        public Success(Object source, ServerPlayer player, UPokemon<?> pokemon, @Nullable UPokeBall<?> ball, @Nullable UPokemonEntity<?> entity) {
            super(source, player, pokemon, ball, entity);
        }

        /**
         * @return {@link UEvent#POKEMON_CAPTURE_SUCCESS}
         */
        @Override
        public Key<?> type() {
            return UEvent.POKEMON_CAPTURE_SUCCESS;
        }
    }

    /**
     * Holds the data of {@link UEvent#POKEMON_CAPTURE_FAIL}, which fires when a capture fails.
     */
    public static class Fail extends CaptureEventPayload {

        /**
         * Creates the payload for a capture that fails.
         *
         * @param source the event of the Pokémon mod that this payload wraps
         * @param player the player who tries to capture the Pokémon
         * @param pokemon the Pokémon that is not captured
         * @param ball the Poké Ball of the capture, or {@code null} if it is not known
         * @param entity the entity of the Pokémon, or {@code null} if it is not known
         */
        public Fail(Object source, ServerPlayer player, UPokemon<?> pokemon, @Nullable UPokeBall<?> ball, @Nullable UPokemonEntity<?> entity) {
            super(source, player, pokemon, ball, entity);
        }

        /**
         * @return {@link UEvent#POKEMON_CAPTURE_FAIL}
         */
        @Override
        public Key<?> type() {
            return UEvent.POKEMON_CAPTURE_FAIL;
        }
    }
}
