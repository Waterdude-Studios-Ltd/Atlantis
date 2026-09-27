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

public abstract class LootTableEventPayload implements EventPayload, Cancellable {

    private final LootContext lootContext;

    private boolean cancelled = false;

    protected LootTableEventPayload(LootContext lootContext) {
        this.lootContext = lootContext;
    }

    public final LootContext context() {
        return this.lootContext;
    }

    public Optional<ServerPlayer> parameterAsPlayer(LootContextParam<? extends Entity> parameter) {
        var e = this.lootContext.getParamOrNull(parameter);
        if (e instanceof ServerPlayer player) {
            return Optional.of(player);
        } else {
            return Optional.empty();
        }
    }

    public <T> Optional<T> parameter(LootContextParam<T> parameter) {
        return Optional.ofNullable(this.lootContext.getParamOrNull(parameter));
    }

    @Override
    public void cancel() {
        this.cancelled = true;
    }

    @Override
    public boolean cancelled() {
        return this.cancelled;
    }

    public static class Pre extends LootTableEventPayload {

        private final Consumer<ItemStack> itemConsumer;

        public Pre(LootContext lootContext, Consumer<ItemStack> itemConsumer) {
            super(lootContext);
            this.itemConsumer = itemConsumer;
        }

        @Override
        public Key<?> type() {
            return UEvent.LOOT_TABLE_PRE;
        }

        public void addDrops(ItemStack... drops) {
            for (var drop : drops) {
                this.itemConsumer.accept(drop);
            }
        }

        public void overrideDrops(ItemStack... drops) {
            this.cancel();
            this.addDrops(drops);
        }
    }

    public static class Post extends LootTableEventPayload {

        private final List<ItemStack> drops;

        public Post(LootContext lootContext, List<ItemStack> drops) {
            super(lootContext);
            this.drops = drops;
        }

        @Override
        public Key<?> type() {
            return UEvent.LOOT_TABLE_POST;
        }

        public void clearDrops() {
            this.drops.clear();
        }

        public List<ItemStack> drops() {
            return this.drops;
        }

        public void addDrops(ItemStack... drops) {
            this.drops.addAll(Lists.newArrayList(drops));
        }

        public void overrideDrops(ItemStack... drops) {
            this.clearDrops();
            this.addDrops(drops);
        }
    }
}
