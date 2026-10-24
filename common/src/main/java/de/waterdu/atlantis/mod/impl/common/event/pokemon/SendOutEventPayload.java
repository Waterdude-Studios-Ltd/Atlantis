package de.waterdu.atlantis.mod.impl.common.event.pokemon;

import de.waterdu.atlantis.mod.impl.common.event.Cancellable;
import de.waterdu.atlantis.mod.wrapper.modloader.event.UEvent;
import de.waterdu.atlantis.mod.wrapper.pokemon.entity.UPokemonEntity;
import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.UPokemon;
import de.waterdu.atlantis.util.config.Key;
import net.minecraft.server.level.ServerPlayer;
import org.jspecify.annotations.Nullable;

public abstract class SendOutEventPayload extends PokemonEventPayload {

    protected SendOutEventPayload(Object source, @Nullable ServerPlayer player, UPokemon<?> pokemon) {
        super(source, player, pokemon);
    }

    public static class Pre extends SendOutEventPayload implements Cancellable {

        private boolean cancelled = false;

        public Pre(Object source, @Nullable ServerPlayer player, UPokemon<?> pokemon) {
            super(source, player, pokemon);
        }

        @Override
        public Key<?> type() {
            return UEvent.POKEMON_SEND_OUT_PRE;
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

    public static class Post extends SendOutEventPayload {

        private final UPokemonEntity<?> entity;

        public Post(Object source, @Nullable ServerPlayer player, UPokemon<?> pokemon, UPokemonEntity<?> entity) {
            super(source, player, pokemon);
            this.entity = entity;
        }

        @Override
        public Key<?> type() {
            return UEvent.POKEMON_SEND_OUT_POST;
        }

        public UPokemonEntity<?> entity() {
            return this.entity;
        }
    }
}
