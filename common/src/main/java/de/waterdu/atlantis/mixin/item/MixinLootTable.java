package de.waterdu.atlantis.mixin.item;

import de.waterdu.atlantis.mod.Mods;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootTable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@Mixin(value = LootTable.class)
public abstract class MixinLootTable {

    @Inject(method = "getRandomItemsRaw(Lnet/minecraft/world/level/storage/loot/LootContext;Ljava/util/function/Consumer;)V", at = @At(value = "HEAD"), cancellable = true)
    private void onGetRandomItemsRaw(LootContext context, Consumer<ItemStack> consumer, CallbackInfo ci) {
        if (Mods.events().emitLootTablePre(context, consumer).cancelled()) {
            ci.cancel();
        }
    }

    @Inject(method = "getRandomItems(Lnet/minecraft/world/level/storage/loot/LootContext;)Lit/unimi/dsi/fastutil/objects/ObjectArrayList;", at = @At(value = "RETURN", ordinal = 0), cancellable = true)
    private void onGetRandomItems(LootContext context, CallbackInfoReturnable<List<ItemStack>> cir) {
        var properties = Mods.events().emitLootTablePost(context, cir.getReturnValue());
        if (properties.cancelled()) {
            cir.setReturnValue(new ArrayList<>());
        } else {
            cir.setReturnValue(properties.drops());
        }
    }
}