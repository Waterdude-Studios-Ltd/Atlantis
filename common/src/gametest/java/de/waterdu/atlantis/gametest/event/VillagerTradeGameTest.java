package de.waterdu.atlantis.gametest.event;

import de.waterdu.atlantis.gametest.AtlantisGameTest;
import de.waterdu.atlantis.gametest.GameTestPlayers;
import de.waterdu.atlantis.mod.Mods;
import de.waterdu.atlantis.mod.impl.common.event.EventPayload;
import de.waterdu.atlantis.mod.impl.common.event.entity.VillagerTradeEventPayload;
import de.waterdu.atlantis.mod.wrapper.modloader.event.UEvent;
import de.waterdu.atlantis.util.config.Key;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Tests the {@link UEvent#VILLAGER_TRADE_PRE}, {@link UEvent#VILLAGER_TRADE_POST} and {@link UEvent#VILLAGER_TRADE_XP}
 * events.
 */
public final class VillagerTradeGameTest {

    private static final int PAYMENT_SLOT = 0;
    private static final int RESULT_SLOT = 2;
    private static final int OFFER_XP = 5;

    /**
     * Prevents instantiation.
     */
    private VillagerTradeGameTest() {}

    /**
     * Checks that a trade fires the pre event and then the post event, gives the result, takes the payment and grants
     * the villager the offer XP.
     *
     * @param helper the helper of the running test
     */
    @AtlantisGameTest
    public static void tradeFiresPreAndPostAndGrantsXp(GameTestHelper helper) {
        var trade = Trade.create(helper);
        var events = new ArrayList<String>();
        trade.listen(UEvent.VILLAGER_TRADE_PRE, __ -> events.add("pre"));
        trade.listen(UEvent.VILLAGER_TRADE_POST, __ -> events.add("post"));
        var xp = trade.villager.getVillagerXp();

        trade.menu.clicked(RESULT_SLOT, 0, ClickType.PICKUP, trade.player);

        helper.assertValueEqual(events, List.of("pre", "post"), "events");
        helper.assertTrue(trade.menu.getCarried().is(Items.DIAMOND), "player did not get the result");
        helper.assertTrue(trade.menu.getSlot(PAYMENT_SLOT).getItem().isEmpty(), "payment was not taken");
        helper.assertValueEqual(trade.villager.getVillagerXp(), xp + OFFER_XP, "villager xp");
        helper.succeed();
    }

    /**
     * Checks that cancelling the pre event undoes a trade made with a normal click.
     *
     * @param helper the helper of the running test
     */
    @AtlantisGameTest
    public static void cancellingPreUndoesAClick(GameTestHelper helper) {
        var trade = Trade.create(helper);
        var post = new ArrayList<String>();
        trade.listen(UEvent.VILLAGER_TRADE_PRE, VillagerTradeEventPayload::cancel);
        trade.listen(UEvent.VILLAGER_TRADE_POST, __ -> post.add("post"));
        var xp = trade.villager.getVillagerXp();

        trade.menu.clicked(RESULT_SLOT, 0, ClickType.PICKUP, trade.player);

        trade.assertNotTraded(post, xp);
        helper.succeed();
    }

    /**
     * Checks that cancelling the pre event undoes a trade made with a shift click.
     *
     * @param helper the helper of the running test
     */
    @AtlantisGameTest
    public static void cancellingPreUndoesAShiftClick(GameTestHelper helper) {
        var trade = Trade.create(helper);
        var post = new ArrayList<String>();
        trade.listen(UEvent.VILLAGER_TRADE_PRE, VillagerTradeEventPayload::cancel);
        trade.listen(UEvent.VILLAGER_TRADE_POST, __ -> post.add("post"));
        var xp = trade.villager.getVillagerXp();

        trade.menu.clicked(RESULT_SLOT, 0, ClickType.QUICK_MOVE, trade.player);

        helper.succeedWhen(() -> trade.assertNotTraded(post, xp));
    }

    /**
     * Checks that the villager gets the XP that an XP event listener sets.
     *
     * @param helper the helper of the running test
     */
    @AtlantisGameTest
    public static void xpListenerOverridesXp(GameTestHelper helper) {
        var trade = Trade.create(helper);
        trade.<VillagerTradeEventPayload.XP>listen(UEvent.VILLAGER_TRADE_XP, payload -> payload.setXP(42));
        var xp = trade.villager.getVillagerXp();

        trade.menu.clicked(RESULT_SLOT, 0, ClickType.PICKUP, trade.player);

        helper.assertValueEqual(trade.villager.getVillagerXp(), xp + 42, "villager xp");
        helper.succeed();
    }

    /**
     * Checks that cancelling the XP event grants the villager no XP, but the player still gets the result.
     *
     * @param helper the helper of the running test
     */
    @AtlantisGameTest
    public static void cancellingXpGrantsNone(GameTestHelper helper) {
        var trade = Trade.create(helper);
        trade.listen(UEvent.VILLAGER_TRADE_XP, VillagerTradeEventPayload::cancel);
        var xp = trade.villager.getVillagerXp();

        trade.menu.clicked(RESULT_SLOT, 0, ClickType.PICKUP, trade.player);

        helper.assertTrue(trade.menu.getCarried().is(Items.DIAMOND), "player did not get the result");
        helper.assertValueEqual(trade.villager.getVillagerXp(), xp, "villager xp");
        helper.succeed();
    }

    /**
     * Holds a villager with one offer and a mock player who has the merchant menu of the villager open.
     *
     * @param helper the helper of the running test
     * @param villager the villager that trades
     * @param player the player who trades with the villager
     * @param menu the merchant menu of the player
     */
    private record Trade(GameTestHelper helper, Villager villager, ServerPlayer player, MerchantMenu menu) {

        /**
         * Spawns a villager without AI that trades one emerald for one diamond, and opens its menu for a mock player.
         * The emerald is put into the payment slot.
         *
         * @param helper the helper of the running test
         * @return the trade
         */
        static Trade create(GameTestHelper helper) {
            var villager = helper.spawn(EntityType.VILLAGER, 2, 2, 2);
            villager.setNoAi(true);
            var offers = new MerchantOffers();
            offers.add(new MerchantOffer(new ItemCost(Items.EMERALD, 1), new ItemStack(Items.DIAMOND), 10, OFFER_XP, 0.05F));
            villager.setOffers(offers);
            var player = GameTestPlayers.mock(helper);
            villager.setTradingPlayer(player);
            var menu = new MerchantMenu(1, player.getInventory(), villager);
            player.containerMenu = menu;
            menu.getSlot(PAYMENT_SLOT).set(new ItemStack(Items.EMERALD));
            helper.assertTrue(menu.getSlot(RESULT_SLOT).getItem().is(Items.DIAMOND), "offer result not shown");
            return new Trade(helper, villager, player, menu);
        }

        /**
         * Listens for a villager trade event and gives the payloads of this villager to the listener.
         *
         * @param <P> the payload type of the event
         * @param key the key of the event
         * @param listener receives each payload of this villager
         */
        @SuppressWarnings({"unchecked", "rawtypes"})
        <P extends VillagerTradeEventPayload> void listen(Key<? extends UEvent<? extends EventPayload>> key, Consumer<P> listener) {
            Mods.modLoader().event((Key) key).listen(payload -> {
                if (payload instanceof VillagerTradeEventPayload trade && trade.merchant() == this.villager) {
                    listener.accept((P) trade);
                }
                return InteractionResult.PASS;
            });
        }

        /**
         * Checks that no trade happened: the post event did not fire, the player has no result, the payment and the
         * result slot are unchanged and the villager XP did not change.
         *
         * @param post the post events that were recorded
         * @param xp the villager XP before the trade
         */
        void assertNotTraded(List<String> post, int xp) {
            this.helper.assertTrue(post.isEmpty(), "VILLAGER_TRADE_POST fired for a cancelled trade");
            this.helper.assertTrue(this.menu.getCarried().isEmpty(), "cursor still holds the result");
            this.helper.assertFalse(this.player.getInventory().contains(new ItemStack(Items.DIAMOND)), "inventory still holds the result");
            this.helper.assertTrue(this.menu.getSlot(PAYMENT_SLOT).getItem().is(Items.EMERALD), "payment was taken");
            this.helper.assertTrue(this.menu.getSlot(RESULT_SLOT).getItem().is(Items.DIAMOND), "result slot was not restored");
            this.helper.assertValueEqual(this.villager.getVillagerXp(), xp, "villager xp");
        }
    }
}
