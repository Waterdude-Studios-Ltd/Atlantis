package de.waterdu.atlantis.mixin.entity;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import de.waterdu.atlantis.mod.impl.common.event.entity.VillagerTradeXpOverride;
import net.minecraft.world.entity.npc.Villager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Applies the trade XP from the villager trade XP event to the villager.
 */
@Mixin(Villager.class)
public abstract class MixinVillager {

    /**
     * Replaces the XP of the offer with the XP that {@link VillagerTradeXpOverride} holds for this thread, if any.
     *
     * @param offerXp the XP of the offer
     * @return the XP to reward
     */
    @ModifyExpressionValue(method = "rewardTradeXp", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/trading/MerchantOffer;getXp()I"))
    private int atlantis$applyTradeXp(int offerXp) {
        return VillagerTradeXpOverride.apply(() -> offerXp);
    }
}
