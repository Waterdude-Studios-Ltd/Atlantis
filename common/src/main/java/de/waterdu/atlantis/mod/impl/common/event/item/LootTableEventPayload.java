package de.waterdu.atlantis.mod.impl.common.event.item;

import com.google.common.collect.Lists;
import de.waterdu.atlantis.mod.impl.common.event.Cancellable;
import de.waterdu.atlantis.mod.impl.common.event.EventPayload;
import de.waterdu.atlantis.mod.wrapper.modloader.event.UEvent;
import de.waterdu.atlantis.util.config.Key;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParam;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Holds the data of a loot table that makes its drops.
 */
public abstract class LootTableEventPayload implements EventPayload, Cancellable {

    private final LootContext lootContext;

    private boolean cancelled = false;

    /**
     * Creates a loot table payload.
     *
     * @param lootContext the context that the table rolls with
     */
    protected LootTableEventPayload(LootContext lootContext) {
        this.lootContext = lootContext;
    }

    /**
     * @return the context that the table rolls with
     */
    public final LootContext context() {
        return this.lootContext;
    }

    /**
     * Gets an entity parameter of the loot context, if it is a server player.
     *
     * @param parameter the entity parameter, for example {@code LootContextParams.THIS_ENTITY}
     * @return the player, or an empty optional if the parameter is absent or not a server player
     */
    public Optional<ServerPlayer> parameterAsPlayer(LootContextParam<? extends Entity> parameter) {
        var e = this.lootContext.getParamOrNull(parameter);
        if (e instanceof ServerPlayer player) {
            return Optional.of(player);
        } else {
            return Optional.empty();
        }
    }

    /**
     * Gets a parameter of the loot context.
     *
     * @param parameter the parameter to get
     * @param <T> the type of the parameter value
     * @return the value, or an empty optional if the context does not have the parameter
     */
    public <T> Optional<T> parameter(LootContextParam<T> parameter) {
        return Optional.ofNullable(this.lootContext.getParamOrNull(parameter));
    }

    /**
     * Marks this payload as cancelled.
     */
    @Override
    public void cancel() {
        this.cancelled = true;
    }

    /**
     * Checks if a listener cancelled this payload.
     *
     * @return {@code true} if the payload is cancelled
     */
    @Override
    public boolean cancelled() {
        return this.cancelled;
    }

    /**
     * Fires before the loot table rolls. If a listener cancels it, the table does not roll.
     */
    public static class Pre extends LootTableEventPayload {

        private final Consumer<ItemStack> itemConsumer;

        /**
         * Creates a payload for {@link UEvent#LOOT_TABLE_PRE}.
         *
         * @param lootContext the context that the table rolls with
         * @param itemConsumer receives the drops
         */
        public Pre(LootContext lootContext, Consumer<ItemStack> itemConsumer) {
            super(lootContext);
            this.itemConsumer = itemConsumer;
        }

        /**
         * @return {@link UEvent#LOOT_TABLE_PRE}
         */
        @Override
        public Key<?> type() {
            return UEvent.LOOT_TABLE_PRE;
        }

        /**
         * Gives the drops at once, before the table rolls. The rolled drops follow, unless the event is cancelled.
         */
        public void addDrops(ItemStack... drops) {
            for (var drop : drops) {
                this.itemConsumer.accept(drop);
            }
        }

        /**
         * Cancels the event and gives these drops instead of the rolled drops.
         */
        public void overrideDrops(ItemStack... drops) {
            this.cancel();
            this.addDrops(drops);
        }
    }

    /**
     * Fires after the loot table rolled. If a listener cancels it, the table returns no drops.
     */
    public static class Post extends LootTableEventPayload {

        private final List<ItemStack> drops;

        /**
         * Creates a payload for {@link UEvent#LOOT_TABLE_POST}.
         *
         * @param lootContext the context that the table rolled with
         * @param drops the drops that the table returns
         */
        public Post(LootContext lootContext, List<ItemStack> drops) {
            super(lootContext);
            this.drops = drops;
        }

        /**
         * @return {@link UEvent#LOOT_TABLE_POST}
         */
        @Override
        public Key<?> type() {
            return UEvent.LOOT_TABLE_POST;
        }

        /**
         * Removes all drops.
         */
        public void clearDrops() {
            this.drops.clear();
        }

        /**
         * The drops that the table returns. This list is live, so changes to it change the drops.
         */
        public List<ItemStack> drops() {
            return this.drops;
        }

        /**
         * Adds the drops to the end of the drop list.
         *
         * @param drops the drops to add
         */
        public void addDrops(ItemStack... drops) {
            this.drops.addAll(Lists.newArrayList(drops));
        }

        /**
         * Replaces all drops with these drops.
         *
         * @param drops the new drops
         */
        public void overrideDrops(ItemStack... drops) {
            this.clearDrops();
            this.addDrops(drops);
        }
    }
}
