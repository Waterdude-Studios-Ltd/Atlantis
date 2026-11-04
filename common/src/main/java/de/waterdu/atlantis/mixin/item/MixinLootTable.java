package de.waterdu.atlantis.mixin.item;

import de.waterdu.atlantis.mod.Mods;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootTable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.Consumer;

/**
 * Emits LOOT_TABLE_PRE and LOOT_TABLE_POST when a loot table makes its drops.
 */
@Mixin(value = LootTable.class)
public abstract class MixinLootTable {

    /**
     * Emits LOOT_TABLE_PRE. If a listener cancels it, the loot table makes no drops.
     *
     * @param context the loot context
     * @param consumer the consumer that receives the drops
     * @param ci the callback info
     */
    @Inject(method = "getRandomItemsRaw(Lnet/minecraft/world/level/storage/loot/LootContext;Ljava/util/function/Consumer;)V", at = @At(value = "HEAD"), cancellable = true)
    private void onGetRandomItemsRaw(LootContext context, Consumer<ItemStack> consumer, CallbackInfo ci) {
        if (Mods.events().emitLootTablePre(context, consumer).cancelled()) {
            ci.cancel();
        }
    }

    /**
     * Emits LOOT_TABLE_POST with the drops. If a listener cancels it, returns an empty list.
     *
     * @param context the loot context
     * @param cir the callback that holds the drops
     */
    @Inject(method = "getRandomItems(Lnet/minecraft/world/level/storage/loot/LootContext;)Lit/unimi/dsi/fastutil/objects/ObjectArrayList;", at = @At(value = "RETURN", ordinal = 0), cancellable = true)
    private void onGetRandomItems(LootContext context, CallbackInfoReturnable<ObjectArrayList<ItemStack>> cir) {
        if (Mods.events().emitLootTablePost(context, cir.getReturnValue()).cancelled()) {
            cir.setReturnValue(new ObjectArrayList<>());
        }
    }
}