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

    /**
     * Creates the payload of a trade.
     *
     * @param source the event of the Pokémon mod that this payload wraps
     * @param player1 the first player, or {@code null} if no player is known
     * @param pokemon1 the Pokémon that the first player gives
     * @param player2 the second player, or {@code null} if no player is known
     * @param pokemon2 the Pokémon that the second player gives
     */
    protected TradeEventPayload(Object source, @Nullable ServerPlayer player1, UPokemon<?> pokemon1, @Nullable ServerPlayer player2, UPokemon<?> pokemon2) {
        super(source);
        this.player1 = player1;
        this.pokemon1 = pokemon1;
        this.player2 = player2;
        this.pokemon2 = pokemon2;
    }

    /**
     * @return the first player, or {@code null} if no player is known
     */
    public @Nullable ServerPlayer player1() {
        return this.player1;
    }

    /**
     * @return the Pokémon that the first player gives
     */
    public UPokemon<?> pokemon1() {
        return this.pokemon1;
    }

    /**
     * @return the second player, or {@code null} if no player is known
     */
    public @Nullable ServerPlayer player2() {
        return this.player2;
    }

    /**
     * @return the Pokémon that the second player gives
     */
    public UPokemon<?> pokemon2() {
        return this.pokemon2;
    }

    /**
     * Holds the data of {@link UEvent#POKEMON_TRADE_PRE}, which fires before the trade. Cancel it to stop the trade.
     */
    public static class Pre extends TradeEventPayload implements Cancellable {

        private boolean cancelled = false;

        /**
         * Creates the payload that fires before a trade.
         *
         * @param source the event of the Pokémon mod that this payload wraps
         * @param player1 the first player, or {@code null} if no player is known
         * @param pokemon1 the Pokémon that the first player gives
         * @param player2 the second player, or {@code null} if no player is known
         * @param pokemon2 the Pokémon that the second player gives
         */
        public Pre(Object source, @Nullable ServerPlayer player1, UPokemon<?> pokemon1, @Nullable ServerPlayer player2, UPokemon<?> pokemon2) {
            super(source, player1, pokemon1, player2, pokemon2);
        }

        /**
         * @return {@link UEvent#POKEMON_TRADE_PRE}
         */
        @Override
        public Key<?> type() {
            return UEvent.POKEMON_TRADE_PRE;
        }

        /**
         * Marks this trade as cancelled.
         */
        @Override
        public void cancel() {
            this.cancelled = true;
        }

        /**
         * @return {@code true} if a listener cancelled this trade
         */
        @Override
        public boolean cancelled() {
            return this.cancelled;
        }
    }

    /**
     * Holds the data of {@link UEvent#POKEMON_TRADE_POST}, which fires after the trade.
     */
    public static class Post extends TradeEventPayload {

        /**
         * Creates the payload that fires after a trade.
         *
         * @param source the event of the Pokémon mod that this payload wraps
         * @param player1 the first player, or {@code null} if no player is known
         * @param pokemon1 the Pokémon that the first player gave
         * @param player2 the second player, or {@code null} if no player is known
         * @param pokemon2 the Pokémon that the second player gave
         */
        public Post(Object source, @Nullable ServerPlayer player1, UPokemon<?> pokemon1, @Nullable ServerPlayer player2, UPokemon<?> pokemon2) {
            super(source, player1, pokemon1, player2, pokemon2);
        }

        /**
         * @return {@link UEvent#POKEMON_TRADE_POST}
         */
        @Override
        public Key<?> type() {
            return UEvent.POKEMON_TRADE_POST;
        }
    }
}
