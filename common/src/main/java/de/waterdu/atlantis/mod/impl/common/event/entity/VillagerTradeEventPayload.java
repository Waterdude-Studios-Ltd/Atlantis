package de.waterdu.atlantis.mod.impl.common.event.entity;

import de.waterdu.atlantis.mod.impl.common.event.Cancellable;
import de.waterdu.atlantis.mod.impl.common.event.EventPayload;
import de.waterdu.atlantis.mod.wrapper.modloader.event.UEvent;
import de.waterdu.atlantis.util.config.Key;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffer;

public abstract class VillagerTradeEventPayload implements EventPayload, Cancellable {

    private final ItemStack offerItemA;
    private final ItemStack offerItemB;
    private final ItemStack resultItem;
    private final Merchant merchant;
    private final MerchantOffer offer;
    private final Player player;
    private boolean cancelled = false;

    protected VillagerTradeEventPayload(
            ItemStack offerItemA,
            ItemStack offerItemB,
            ItemStack resultItem,
            Merchant merchant,
            MerchantOffer offer,
            Player player
    ) {
        this.offerItemA = offerItemA;
        this.offerItemB = offerItemB;
        this.resultItem = resultItem;
        this.merchant = merchant;
        this.offer = offer;
        this.player = player;
    }

    public ItemStack offerItemA() {
        return this.offerItemA;
    }

    public ItemStack offerItemB() {
        return this.offerItemB;
    }

    public ItemStack resultItem() {
        return this.resultItem;
    }

    public Merchant merchant() {
        return this.merchant;
    }

    public MerchantOffer offer() {
        return this.offer;
    }

    public Player player() {
        return this.player;
    }

    @Override
    public void cancel() {
        this.cancelled = true;
    }

    @Override
    public boolean cancelled() {
        return this.cancelled;
    }

    public static class Pre extends VillagerTradeEventPayload {

        public Pre(
                ItemStack offerItemA,
                ItemStack offerItemB,
                ItemStack resultItem,
                Merchant merchant,
                MerchantOffer offer,
                Player player
        ) {
            super(offerItemA, offerItemB, resultItem, merchant, offer, player);
        }

        @Override
        public Key<?> type() {
            return UEvent.VILLAGER_TRADE_PRE;
        }
    }

    public static class Post extends VillagerTradeEventPayload {

        public Post(
                ItemStack offerItemA,
                ItemStack offerItemB,
                ItemStack resultItem,
                Merchant merchant,
                MerchantOffer offer,
                Player player
        ) {
            super(offerItemA, offerItemB, resultItem, merchant, offer, player);
        }

        @Override
        public Key<?> type() {
            return UEvent.VILLAGER_TRADE_POST;
        }
    }

    public static class XP extends VillagerTradeEventPayload {

        private int xp;

        public XP(
                ItemStack offerItemA,
                ItemStack offerItemB,
                ItemStack resultItem,
                Merchant merchant,
                MerchantOffer offer,
                Player player,
                int xp
        ) {
            super(offerItemA, offerItemB, resultItem, merchant, offer, player);
            this.xp = xp;
        }

        @Override
        public Key<?> type() {
            return UEvent.VILLAGER_TRADE_XP;
        }

        public int xp() {
            return this.cancelled() ? 0 : this.xp;
        }

        public void setXP(int xp) {
            this.xp = xp;
        }
    }
}
