package de.waterdu.atlantis.mod.impl.common.event.pokemon;

import de.waterdu.atlantis.mod.impl.common.event.Cancellable;
import de.waterdu.atlantis.mod.wrapper.modloader.event.UEvent;
import de.waterdu.atlantis.mod.wrapper.pokemon.entity.UPokemonEntity;
import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.UPokemon;
import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.properties.UPokeBall;
import de.waterdu.atlantis.util.config.Key;
import net.minecraft.server.level.ServerPlayer;
import org.jspecify.annotations.Nullable;

public abstract class CaptureEventPayload extends PokemonEventPayload {

    private final @Nullable UPokeBall<?> ball;
    private final @Nullable UPokemonEntity<?> entity;

    protected CaptureEventPayload(Object source, ServerPlayer player, UPokemon<?> pokemon, @Nullable UPokeBall<?> ball, @Nullable UPokemonEntity<?> entity) {
        super(source, player, pokemon);
        this.ball = ball;
        this.entity = entity;
    }

    public @Nullable UPokeBall<?> ball() {
        return this.ball;
    }

    public @Nullable UPokemonEntity<?> entity() {
        return this.entity;
    }

    public static class Start extends CaptureEventPayload implements Cancellable {

        private boolean cancelled = false;

        public Start(Object source, ServerPlayer player, UPokemon<?> pokemon, @Nullable UPokeBall<?> ball, @Nullable UPokemonEntity<?> entity) {
            super(source, player, pokemon, ball, entity);
        }

        @Override
        public Key<?> type() {
            return UEvent.POKEMON_CAPTURE_START;
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

    public static class Success extends CaptureEventPayload {

        public Success(Object source, ServerPlayer player, UPokemon<?> pokemon, @Nullable UPokeBall<?> ball, @Nullable UPokemonEntity<?> entity) {
            super(source, player, pokemon, ball, entity);
        }

        @Override
        public Key<?> type() {
            return UEvent.POKEMON_CAPTURE_SUCCESS;
        }
    }

    public static class Fail extends CaptureEventPayload {

        public Fail(Object source, ServerPlayer player, UPokemon<?> pokemon, @Nullable UPokeBall<?> ball, @Nullable UPokemonEntity<?> entity) {
            super(source, player, pokemon, ball, entity);
        }

        @Override
        public Key<?> type() {
            return UEvent.POKEMON_CAPTURE_FAIL;
        }
    }
}
