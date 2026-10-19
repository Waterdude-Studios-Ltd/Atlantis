package de.waterdu.atlantis.mixin.entity;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import de.waterdu.atlantis.mod.impl.common.event.entity.VillagerTradeXpOverride;
import net.minecraft.world.entity.npc.Villager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Villager.class)
public abstract class MixinVillager {

    @ModifyExpressionValue(method = "rewardTradeXp", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/trading/MerchantOffer;getXp()I"))
    private int atlantis$applyTradeXp(int offerXp) {
        return VillagerTradeXpOverride.apply(() -> offerXp);
    }
}
