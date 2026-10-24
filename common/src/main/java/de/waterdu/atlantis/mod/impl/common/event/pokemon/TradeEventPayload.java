package de.waterdu.atlantis.mod.impl.common.event.pokemon;

import de.waterdu.atlantis.mod.impl.common.event.Cancellable;
import de.waterdu.atlantis.mod.wrapper.modloader.event.UEvent;
import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.UPokemon;
import de.waterdu.atlantis.util.config.Key;
import net.minecraft.server.level.ServerPlayer;
import org.jspecify.annotations.Nullable;

/**
 * Two players trade Pokémon: {@link #player1()} gives {@link #pokemon1()} and {@link #player2()} gives
 * {@link #pokemon2()}.
 */
public abstract class TradeEventPayload extends PokemonModEventPayload {

    private final @Nullable ServerPlayer player1;
    private final UPokemon<?> pokemon1;
    private final @Nullable ServerPlayer player2;
    private final UPokemon<?> pokemon2;

    protected TradeEventPayload(Object source, @Nullable ServerPlayer player1, UPokemon<?> pokemon1, @Nullable ServerPlayer player2, UPokemon<?> pokemon2) {
        super(source);
        this.player1 = player1;
        this.pokemon1 = pokemon1;
        this.player2 = player2;
        this.pokemon2 = pokemon2;
    }

    public @Nullable ServerPlayer player1() {
        return this.player1;
    }

    public UPokemon<?> pokemon1() {
        return this.pokemon1;
    }

    public @Nullable ServerPlayer player2() {
        return this.player2;
    }

    public UPokemon<?> pokemon2() {
        return this.pokemon2;
    }

    public static class Pre extends TradeEventPayload implements Cancellable {

        private boolean cancelled = false;

        public Pre(Object source, @Nullable ServerPlayer player1, UPokemon<?> pokemon1, @Nullable ServerPlayer player2, UPokemon<?> pokemon2) {
            super(source, player1, pokemon1, player2, pokemon2);
        }

        @Override
        public Key<?> type() {
            return UEvent.POKEMON_TRADE_PRE;
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

    public static class Post extends TradeEventPayload {

        public Post(Object source, @Nullable ServerPlayer player1, UPokemon<?> pokemon1, @Nullable ServerPlayer player2, UPokemon<?> pokemon2) {
            super(source, player1, pokemon1, player2, pokemon2);
        }

        @Override
        public Key<?> type() {
            return UEvent.POKEMON_TRADE_POST;
        }
    }
}
