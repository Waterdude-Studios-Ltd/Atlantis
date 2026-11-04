package de.waterdu.atlantis.mod.impl.common.event.pokemon;

import de.waterdu.atlantis.mod.impl.common.event.Cancellable;
import de.waterdu.atlantis.mod.wrapper.modloader.event.UEvent;
import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.UPokemon;
import de.waterdu.atlantis.util.config.Key;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * Base class of the payloads for a change of the held item of a Pokémon.
 */
public abstract class HeldItemEventPayload extends PokemonEventPayload {

    protected ItemStack item;

    /**
     * Creates the payload for a held item change.
     *
     * @param source the event of the Pokémon mod that this payload wraps
     * @param player the player of the event, or {@code null} if there is none
     * @param pokemon the Pokémon whose held item changes
     * @param item the new held item
     */
    protected HeldItemEventPayload(Object source, @Nullable ServerPlayer player, UPokemon<?> pokemon, ItemStack item) {
        super(source, player, pokemon);
        this.item = item;
    }

    /**
     * @return the new held item
     */
    public ItemStack item() {
        return this.item;
    }

    /**
     * Holds the data of {@link UEvent#POKEMON_HELD_ITEM_PRE}, which fires before the held item of a Pokémon changes.
     */
    public static class Pre extends HeldItemEventPayload implements Cancellable {

        private boolean cancelled = false;

        /**
         * Creates the payload for a held item that is about to change.
         *
         * @param source the event of the Pokémon mod that this payload wraps
         * @param player the player of the event, or {@code null} if there is none
         * @param pokemon the Pokémon whose held item changes
         * @param item the new held item
         */
        public Pre(Object source, @Nullable ServerPlayer player, UPokemon<?> pokemon, ItemStack item) {
            super(source, player, pokemon, item);
        }

        /**
         * @return {@link UEvent#POKEMON_HELD_ITEM_PRE}
         */
        @Override
        public Key<?> type() {
            return UEvent.POKEMON_HELD_ITEM_PRE;
        }

        /**
         * Sets the item that the Pokémon gets.
         *
         * @param item the new held item
         */
        public void setItem(ItemStack item) {
            this.item = item;
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
     * Holds the data of {@link UEvent#POKEMON_HELD_ITEM_POST}, which fires after the held item of a Pokémon changes.
     */
    public static class Post extends HeldItemEventPayload {

        /**
         * Creates the payload for a held item that has changed.
         *
         * @param source the event of the Pokémon mod that this payload wraps
         * @param player the player of the event, or {@code null} if there is none
         * @param pokemon the Pokémon whose held item changed
         * @param item the new held item
         */
        public Post(Object source, @Nullable ServerPlayer player, UPokemon<?> pokemon, ItemStack item) {
            super(source, player, pokemon, item);
        }

        /**
         * @return {@link UEvent#POKEMON_HELD_ITEM_POST}
         */
        @Override
        public Key<?> type() {
            return UEvent.POKEMON_HELD_ITEM_POST;
        }
    }
}
