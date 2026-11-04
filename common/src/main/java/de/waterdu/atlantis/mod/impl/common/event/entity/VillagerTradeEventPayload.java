package de.waterdu.atlantis.mod.impl.common.event.entity;

import de.waterdu.atlantis.mod.impl.common.event.Cancellable;
import de.waterdu.atlantis.mod.impl.common.event.EventPayload;
import de.waterdu.atlantis.mod.wrapper.modloader.event.UEvent;
import de.waterdu.atlantis.util.config.Key;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffer;

/**
 * Holds the data of a trade between a player and a merchant.
 */
public abstract class VillagerTradeEventPayload implements EventPayload, Cancellable {

    private final ItemStack offerItemA;
    private final ItemStack offerItemB;
    private final ItemStack resultItem;
    private final Merchant merchant;
    private final MerchantOffer offer;
    private final Player player;
    private boolean cancelled = false;

    /**
     * Creates a trade payload.
     *
     * @param offerItemA the stack in the first payment slot
     * @param offerItemB the stack in the second payment slot
     * @param resultItem the stack that the player takes
     * @param merchant the merchant that trades
     * @param offer the active offer
     * @param player the player who trades
     */
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

    /**
     * The stack in the first payment slot. In {@link Pre} and {@link XP}, this is the live slot stack. In
     * {@link Post}, it is a copy from before the trade took the payment.
     */
    public ItemStack offerItemA() {
        return this.offerItemA;
    }

    /**
     * The stack in the second payment slot. See {@link #offerItemA()}.
     */
    public ItemStack offerItemB() {
        return this.offerItemB;
    }

    /**
     * @return the stack that the player takes from the result slot
     */
    public ItemStack resultItem() {
        return this.resultItem;
    }

    /**
     * @return the merchant that trades
     */
    public Merchant merchant() {
        return this.merchant;
    }

    /**
     * @return the active offer of the trade
     */
    public MerchantOffer offer() {
        return this.offer;
    }

    /**
     * @return the player who trades
     */
    public Player player() {
        return this.player;
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
     * Fires before the trade takes the payment. If a listener cancels it, the trade does not happen and the player
     * does not keep the result.
     */
    public static class Pre extends VillagerTradeEventPayload {

        /**
         * Creates a payload for {@link UEvent#VILLAGER_TRADE_PRE}.
         *
         * @param offerItemA the stack in the first payment slot
         * @param offerItemB the stack in the second payment slot
         * @param resultItem the stack that the player takes
         * @param merchant the merchant that trades
         * @param offer the active offer
         * @param player the player who trades
         */
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

        /**
         * @return {@link UEvent#VILLAGER_TRADE_PRE}
         */
        @Override
        public Key<?> type() {
            return UEvent.VILLAGER_TRADE_PRE;
        }
    }

    /**
     * Fires after the trade took the payment. Cancelling it has no effect on the trade.
     */
    public static class Post extends VillagerTradeEventPayload {

        /**
         * Creates a payload for {@link UEvent#VILLAGER_TRADE_POST}.
         *
         * @param offerItemA a copy of the stack in the first payment slot, from before the trade
         * @param offerItemB a copy of the stack in the second payment slot, from before the trade
         * @param resultItem the stack that the player takes
         * @param merchant the merchant that trades
         * @param offer the active offer
         * @param player the player who trades
         */
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

        /**
         * @return {@link UEvent#VILLAGER_TRADE_POST}
         */
        @Override
        public Key<?> type() {
            return UEvent.VILLAGER_TRADE_POST;
        }
    }

    /**
     * Fires before the trade takes the payment, to decide the XP that the merchant gains.
     */
    public static class XP extends VillagerTradeEventPayload {

        private int xp;

        /**
         * Creates a payload for {@link UEvent#VILLAGER_TRADE_XP}.
         *
         * @param offerItemA the stack in the first payment slot
         * @param offerItemB the stack in the second payment slot
         * @param resultItem the stack that the player takes
         * @param merchant the merchant that trades
         * @param offer the active offer
         * @param player the player who trades
         * @param xp the XP that the merchant gains from the offer
         */
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

        /**
         * @return {@link UEvent#VILLAGER_TRADE_XP}
         */
        @Override
        public Key<?> type() {
            return UEvent.VILLAGER_TRADE_XP;
        }

        /**
         * The XP that the merchant gains from this trade. This is 0 if a listener cancels the event. The XP orbs that
         * the player gets do not change.
         */
        public int xp() {
            return this.cancelled() ? 0 : this.xp;
        }

        /**
         * Sets the XP that the merchant gains from this trade.
         *
         * @param xp the new XP
         */
        public void setXP(int xp) {
            this.xp = xp;
        }
    }
}
