package de.waterdu.atlantis.mod.impl.common.event.pokemon;

import de.waterdu.atlantis.mod.impl.common.event.Cancellable;
import de.waterdu.atlantis.mod.wrapper.modloader.event.UEvent;
import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.UPokemon;
import de.waterdu.atlantis.util.config.Key;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

public abstract class HeldItemEventPayload extends PokemonEventPayload {

    protected ItemStack item;

    protected HeldItemEventPayload(Object source, @Nullable ServerPlayer player, UPokemon<?> pokemon, ItemStack item) {
        super(source, player, pokemon);
        this.item = item;
    }

    public ItemStack item() {
        return this.item;
    }

    public static class Pre extends HeldItemEventPayload implements Cancellable {

        private boolean cancelled = false;

        public Pre(Object source, @Nullable ServerPlayer player, UPokemon<?> pokemon, ItemStack item) {
            super(source, player, pokemon, item);
        }

        @Override
        public Key<?> type() {
            return UEvent.POKEMON_HELD_ITEM_PRE;
        }

        public void setItem(ItemStack item) {
            this.item = item;
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

    public static class Post extends HeldItemEventPayload {

        public Post(Object source, @Nullable ServerPlayer player, UPokemon<?> pokemon, ItemStack item) {
            super(source, player, pokemon, item);
        }

        @Override
        public Key<?> type() {
            return UEvent.POKEMON_HELD_ITEM_POST;
        }
    }
}
