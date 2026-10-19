package de.waterdu.atlantis.mod;

import de.waterdu.atlantis.mod.impl.common.event.entity.ActuallyHurtEventPayload;
import de.waterdu.atlantis.mod.impl.common.event.item.ItemNameEventPayload;
import de.waterdu.atlantis.mod.impl.common.event.item.LootTableEventPayload;
import de.waterdu.atlantis.mod.impl.common.event.meta.SetPlaceholderProviderEventPayload;
import de.waterdu.atlantis.mod.wrapper.modloader.event.UEvent;
import de.waterdu.atlantis.util.placeholder.impl.PlaceholderProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.storage.loot.LootContext;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public final class ModEvents {

    public static final ModEvents INSTANCE = new ModEvents();

    private ModEvents() {}

    public float emitActuallyHurt(LivingEntity entity, DamageSource source, float damage) {
        var payload = Mods.modLoader().event(UEvent.ACTUALLY_HURT).emit(new ActuallyHurtEventPayload(entity, source, damage));
        return payload.cancelled() ? 0F : payload.amount();
    }

    public LootTableEventPayload.Pre emitLootTablePre(LootContext context, Consumer<ItemStack> consumer) {
        return Mods.modLoader().event(UEvent.LOOT_TABLE_PRE).emit(new LootTableEventPayload.Pre(context, consumer));
    }

    public LootTableEventPayload.Post emitLootTablePost(LootContext context, List<ItemStack> drops) {
        return Mods.modLoader().event(UEvent.LOOT_TABLE_POST).emit(new LootTableEventPayload.Post(context, drops));
    }

    public ItemNameEventPayload emitItemName(ItemStack stack, @Nullable Player player, Item.TooltipContext context, AtomicReference<Component> name, TooltipFlag tooltipFlag) {
        return Mods.modLoader().event(UEvent.ITEM_NAME).emit(new ItemNameEventPayload(stack, player, context, name, tooltipFlag));
    }

    public SetPlaceholderProviderEventPayload emitSetPlaceholderProvider(PlaceholderProvider provider) {
        return Mods.modLoader().event(UEvent.SET_PLACEHOLDER_PROVIDER).emit(new SetPlaceholderProviderEventPayload(provider));
    }
}
