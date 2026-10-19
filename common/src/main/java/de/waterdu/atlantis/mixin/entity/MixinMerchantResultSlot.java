package de.waterdu.atlantis.mixin.entity;

import de.waterdu.atlantis.mod.Mods;
import de.waterdu.atlantis.mod.impl.common.event.entity.VillagerTradeEventPayload;
import de.waterdu.atlantis.mod.wrapper.modloader.event.UEvent;
import net.minecraft.server.TickTask;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MerchantContainer;
import net.minecraft.world.inventory.MerchantResultSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(value = MerchantResultSlot.class)
public abstract class MixinMerchantResultSlot {

    @Shadow
    protected abstract void checkTakeAchievements(ItemStack p_75208_1_);

    @Shadow
    @Final
    private MerchantContainer slots;

    @Shadow
    @Final
    private Merchant merchant;

    @Unique
    private static final int RESULT_SLOT = 2;

    /**
     * @author Water
     * @reason Atlantis event injection
     */
    @Overwrite
    public void onTake(Player player, ItemStack stack) {
        MerchantOffer merchantoffer = this.slots.getActiveOffer();
        if (merchantoffer != null) {
            ItemStack itemstack = this.slots.getItem(0);
            ItemStack itemstack1 = this.slots.getItem(1);
            ItemStack itemstackCopy = itemstack.copy();
            ItemStack itemstack1Copy = itemstack1.copy();
            var pre = Mods.modLoader().event(UEvent.VILLAGER_TRADE_PRE).emit(new VillagerTradeEventPayload.Pre(itemstack, itemstack1, stack, this.merchant, merchantoffer, player));
            if (pre.cancelled()) {
                this.atlantis$returnResult(player, stack, merchantoffer);
                return;
            }
            this.checkTakeAchievements(stack);
            if (merchantoffer.take(itemstack, itemstack1) || merchantoffer.take(itemstack1, itemstack)) {
                this.merchant.notifyTrade(merchantoffer);
                player.awardStat(Stats.TRADED_WITH_VILLAGER);
                this.slots.setItem(0, itemstack);
                this.slots.setItem(1, itemstack1);
                Mods.modLoader().event(UEvent.VILLAGER_TRADE_POST).emit(new VillagerTradeEventPayload.Post(itemstackCopy, itemstack1Copy, stack, this.merchant, merchantoffer, player));
            }
            VillagerTradeEventPayload.XP event = Mods.modLoader().event(UEvent.VILLAGER_TRADE_XP).emit(new VillagerTradeEventPayload.XP(itemstack, itemstack1, stack, this.merchant, merchantoffer, player, merchantoffer.getXp()));
            this.merchant.overrideXp(this.merchant.getVillagerXp() + event.xp());
        } else {
            this.checkTakeAchievements(stack);
        }
    }

    @Unique
    private void atlantis$returnResult(Player player, ItemStack stack, MerchantOffer offer) {
        if (!stack.isEmpty()) {
            stack.setCount(0);
            this.slots.updateSellItem();
            return;
        }
        var result = offer.getResult();
        player.getInventory().clearOrCountMatchingItems(it -> ItemStack.isSameItemSameComponents(it, result), result.getCount(), player.inventoryMenu.getCraftSlots());
        this.slots.setItem(RESULT_SLOT, ItemStack.EMPTY);
        var server = player.getServer();
        if (server != null) {
            server.tell(new TickTask(server.getTickCount(), this.slots::updateSellItem));
        }
    }
}