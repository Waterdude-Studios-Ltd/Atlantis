package de.waterdu.atlantis.gametest.ui;

import de.waterdu.atlantis.Atlantis;
import de.waterdu.atlantis.Settings;
import de.waterdu.atlantis.file.AtlantisData;
import de.waterdu.atlantis.gametest.AtlantisGameTest;
import de.waterdu.atlantis.gametest.GameTestConnections;
import de.waterdu.atlantis.gametest.GameTestPlayers;
import de.waterdu.atlantis.mod.Mods;
import de.waterdu.atlantis.mod.impl.common.event.meta.PageEventPayload;
import de.waterdu.atlantis.mod.wrapper.modloader.event.UEvent;
import de.waterdu.atlantis.self.decoration.DecorationEditor;
import de.waterdu.atlantis.self.decoration.DecorationPalette;
import de.waterdu.atlantis.ui.api.AtlantisPage;
import de.waterdu.atlantis.ui.api.AtlantisUI;
import de.waterdu.atlantis.ui.api.Button;
import de.waterdu.atlantis.ui.api.ButtonCollector;
import de.waterdu.atlantis.ui.api.Click;
import de.waterdu.atlantis.ui.api.Decorations;
import de.waterdu.atlantis.ui.api.Page;
import de.waterdu.atlantis.ui.api.PageOptions;
import de.waterdu.atlantis.ui.internal.AtlantisContainerMenuInternal;
import de.waterdu.atlantis.ui.internal.AtlantisInventoryInternal;
import de.waterdu.atlantis.ui.test.TestPage;
import de.waterdu.atlantis.util.entity.PlayerReference;
import de.waterdu.atlantis.util.item.ConfigurableItemStack;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.protocol.game.ClientboundOpenScreenPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Items;

import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * AtlantisUI on a mock player. Pages are built and clicks handled on Atlantis' shared thread pool, which every
 * player join also uses for a blocking Mojang skin lookup. A GameTest server ticks unthrottled, so the wait for the
 * pool is many ticks: the timeout covers roughly two minutes of that.
 */
public final class AtlantisUIGameTest {

    private static final String BATCH = "atlantis_ui";
    private static final int TIMEOUT = 200000;

    private AtlantisUIGameTest() {}

    @AtlantisGameTest(batch = BATCH, timeoutTicks = TIMEOUT)
    public static void opensAChestPageWithItsRowsTitleAndButtons(GameTestHelper helper) {
        var player = GameTestPlayers.mock(helper);
        GameTestConnections.sent(player);
        var page = AtlantisUI.createPage(
                PageOptions.builder().rows(2).title("Hello").build(),
                Button.builder().item(Items.DIAMOND).index(3).build()
        );

        AtlantisUI.open(player, page);

        helper.startSequence()
                .thenWaitUntil(() -> container(helper, player))
                .thenExecute(() -> {
                    var menu = container(helper, player);
                    helper.assertTrue(menu.getType() == MenuType.GENERIC_9x2, "menu type is " + menu.getType());
                    helper.assertValueEqual(menu.getAquaUIInventory().getContainerSize(), 18, "container size");
                    helper.assertTrue(menu.getAquaUIInventory().getItem(3).is(Items.DIAMOND), "button not in slot 3");
                    var open = GameTestConnections.sent(player).stream()
                            .filter(ClientboundOpenScreenPacket.class::isInstance)
                            .map(ClientboundOpenScreenPacket.class::cast)
                            .findFirst()
                            .orElseThrow(() -> new GameTestAssertException("no open screen packet was sent"));
                    helper.assertValueEqual(open.getTitle().getString(), "Hello", "title");
                    helper.assertTrue(open.getType() == MenuType.GENERIC_9x2, "packet menu type");
                })
                .thenSucceed();
    }

    @AtlantisGameTest(batch = BATCH)
    public static void everyFormFactorBuildsItsMenu(GameTestHelper helper) {
        var player = GameTestPlayers.mock(helper);
        var factors = Map.ofEntries(
                Map.entry(PageOptions.FormFactor.FURNACE, MenuType.FURNACE),
                Map.entry(PageOptions.FormFactor.ENCHANTING_TABLE, MenuType.ENCHANTMENT),
                Map.entry(PageOptions.FormFactor.VILLAGER_TRADE, MenuType.MERCHANT),
                Map.entry(PageOptions.FormFactor.ANVIL, MenuType.ANVIL),
                Map.entry(PageOptions.FormFactor.CRAFTING_TABLE, MenuType.CRAFTING),
                Map.entry(PageOptions.FormFactor.DISPENSER, MenuType.GENERIC_3x3),
                Map.entry(PageOptions.FormFactor.BREWING_STAND, MenuType.BREWING_STAND),
                Map.entry(PageOptions.FormFactor.HOPPER, MenuType.HOPPER),
                Map.entry(PageOptions.FormFactor.BEACON, MenuType.BEACON),
                Map.entry(PageOptions.FormFactor.BLAST_FURNACE, MenuType.BLAST_FURNACE),
                Map.entry(PageOptions.FormFactor.GRINDSTONE, MenuType.GRINDSTONE),
                Map.entry(PageOptions.FormFactor.LECTERN, MenuType.LECTERN),
                Map.entry(PageOptions.FormFactor.LOOM, MenuType.LOOM),
                Map.entry(PageOptions.FormFactor.SHULKER_BOX, MenuType.SHULKER_BOX),
                Map.entry(PageOptions.FormFactor.SMITHING_TABLE, MenuType.SMITHING),
                Map.entry(PageOptions.FormFactor.SMOKER, MenuType.SMOKER),
                Map.entry(PageOptions.FormFactor.CARTOGRAPHY_TABLE, MenuType.CARTOGRAPHY_TABLE),
                Map.entry(PageOptions.FormFactor.STONECUTTER, MenuType.STONECUTTER)
        );
        for (var entry : factors.entrySet()) {
            var factor = entry.getKey();
            var page = AtlantisUI.createPage(PageOptions.builder().formFactor(factor).build());
            var inventory = new AtlantisInventoryInternal(page, player, factor.slots());
            var menu = new AtlantisContainerMenuInternal(inventory, player);
            helper.assertTrue(menu.getType() == entry.getValue(), "menu type of " + entry.getValue());
            helper.assertValueEqual(inventory.getContainerSize(), factor.slots(), "slots of " + entry.getValue());
            helper.assertValueEqual(menu.slots.size(), factor.slots() + 36, "menu slots of " + entry.getValue());
        }
        for (var rows = 1; rows <= 6; rows++) {
            var page = AtlantisUI.createPage(PageOptions.builder().rows(rows).build());
            var inventory = new AtlantisInventoryInternal(page, player, rows * 9);
            var menu = new AtlantisContainerMenuInternal(inventory, player);
            helper.assertValueEqual(inventory.getContainerSize(), rows * 9, "slots of " + rows + " rows");
            helper.assertValueEqual(menu.slots.size(), rows * 9 + 36, "menu slots of " + rows + " rows");
        }
        helper.succeed();
    }

    @AtlantisGameTest(batch = BATCH, timeoutTicks = TIMEOUT)
    public static void cancellingConstructPageBlocksTheOpen(GameTestHelper helper) {
        var player = GameTestPlayers.mock(helper);
        var page = AtlantisUI.createPage(PageOptions.builder().build());
        var fired = new AtomicBoolean();
        listen(UEvent.CONSTRUCT_PAGE, player, payload -> {
            fired.set(true);
            payload.cancel();
        });

        AtlantisUI.open(player, page);

        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(fired.get(), "CONSTRUCT_PAGE did not fire"))
                .thenIdle(10)
                .thenExecute(() -> helper.assertTrue(player.containerMenu == player.inventoryMenu, "the page opened"))
                .thenSucceed();
    }

    @AtlantisGameTest(batch = BATCH, timeoutTicks = TIMEOUT)
    public static void constructPageCanAddButtons(GameTestHelper helper) {
        var player = GameTestPlayers.mock(helper);
        var page = AtlantisUI.createPage(PageOptions.builder().build());
        listen(UEvent.CONSTRUCT_PAGE, player, payload -> payload.addButton(Button.builder().item(Items.EMERALD).index(4).build()));

        AtlantisUI.open(player, page);

        helper.succeedWhen(() -> helper.assertTrue(container(helper, player).getAquaUIInventory().getItem(4).is(Items.EMERALD), "added button missing"));
    }

    @AtlantisGameTest(batch = BATCH, timeoutTicks = TIMEOUT)
    public static void waitForScreenOpensOnceTheCurrentScreenCloses(GameTestHelper helper) {
        var player = GameTestPlayers.mock(helper);
        var other = new ChestMenu(MenuType.GENERIC_9x1, 7, player.getInventory(), new SimpleContainer(9), 1);
        player.containerMenu = other;
        var page = AtlantisUI.createPage(PageOptions.builder().build());

        AtlantisUI.open(player, page, true);

        helper.startSequence()
                .thenExecute(() -> helper.assertTrue(AtlantisUI.FUTURE_OPEN.get(player.getUUID()) == page, "page not queued"))
                .thenIdle(10)
                .thenExecute(() -> {
                    helper.assertTrue(player.containerMenu == other, "page opened over another screen");
                    player.containerMenu = player.inventoryMenu;
                })
                .thenWaitUntil(() -> container(helper, player))
                .thenExecute(() -> helper.assertFalse(AtlantisUI.FUTURE_OPEN.containsKey(player.getUUID()), "page still queued"))
                .thenSucceed();
    }

    @AtlantisGameTest(batch = BATCH, timeoutTicks = TIMEOUT)
    public static void decorationsAreAppliedAndDecoratePageFires(GameTestHelper helper) {
        var player = GameTestPlayers.mock(helper);
        Atlantis.CONFIG.put(new Decorations(DecoratedPage.class.getName(), Map.of(5, ConfigurableItemStack.of(Items.GOLD_BLOCK))));
        AtlantisData.mapDecorations();
        var fired = new AtomicBoolean();
        listen(UEvent.DECORATE_PAGE, player, payload -> fired.set(true));

        AtlantisUI.open(player, new DecoratedPage());

        helper.succeedWhen(() -> {
            helper.assertTrue(fired.get(), "DECORATE_PAGE did not fire");
            helper.assertTrue(container(helper, player).getAquaUIInventory().getItem(5).is(Items.GOLD_BLOCK), "decoration missing");
        });
    }

    @AtlantisGameTest(batch = BATCH, timeoutTicks = TIMEOUT)
    public static void clickingAButtonFiresPressPageAndRunsItsAction(GameTestHelper helper) {
        var player = GameTestPlayers.mock(helper);
        var clicks = new AtomicInteger();
        var pressed = new AtomicInteger(-1);
        var page = AtlantisUI.createPage(
                PageOptions.builder().build(),
                Button.builder().item(Items.STONE).index(2).onLeftClick(__ -> clicks.incrementAndGet()).build()
        );
        listen(UEvent.PRESS_PAGE, player, payload -> pressed.set(payload.button().index()));

        AtlantisUI.open(player, page);

        helper.startSequence()
                .thenWaitUntil(() -> unlocked(helper, player))
                .thenExecute(() -> container(helper, player).clicked(2, 0, ClickType.PICKUP, player))
                .thenWaitUntil(() -> helper.assertValueEqual(clicks.get(), 1, "left click actions"))
                .thenExecute(() -> helper.assertValueEqual(pressed.get(), 2, "PRESS_PAGE button"))
                .thenSucceed();
    }

    @AtlantisGameTest(batch = BATCH, timeoutTicks = TIMEOUT)
    public static void cancellingPressPageSuppressesTheAction(GameTestHelper helper) {
        var player = GameTestPlayers.mock(helper);
        var clicks = new AtomicInteger();
        var fired = new AtomicBoolean();
        var page = AtlantisUI.createPage(
                PageOptions.builder().build(),
                Button.builder().item(Items.STONE).index(2).onLeftClick(__ -> clicks.incrementAndGet()).build()
        );
        listen(UEvent.PRESS_PAGE, player, payload -> {
            fired.set(true);
            payload.cancel();
        });

        AtlantisUI.open(player, page);

        helper.startSequence()
                .thenWaitUntil(() -> unlocked(helper, player))
                .thenExecute(() -> container(helper, player).clicked(2, 0, ClickType.PICKUP, player))
                .thenWaitUntil(() -> helper.assertTrue(fired.get(), "PRESS_PAGE did not fire"))
                .thenIdle(10)
                .thenExecute(() -> helper.assertValueEqual(clicks.get(), 0, "actions of a cancelled press"))
                .thenSucceed();
    }

    @AtlantisGameTest(batch = BATCH, timeoutTicks = TIMEOUT)
    public static void twoQuickClicksAreADoubleClick(GameTestHelper helper) {
        var settings = Settings.get();
        var delay = settings.getDoubleClickDelay();
        settings.setDoubleClickDelay(TimeUnit.MINUTES.toMillis(1));
        var player = GameTestPlayers.mock(helper);
        var doubles = new AtomicInteger();
        var singles = new AtomicInteger();
        var singleClickedAt = new AtomicLong();
        var page = AtlantisUI.createPage(
                PageOptions.builder().build(),
                Button.builder().item(Items.STONE).index(0)
                        .onLeftClick(__ -> {
                            singleClickedAt.set(System.currentTimeMillis());
                            singles.incrementAndGet();
                        })
                        .onLeftDoubleClick(__ -> doubles.incrementAndGet())
                        .build()
        );

        AtlantisUI.open(player, page);

        helper.startSequence()
                .thenWaitUntil(() -> unlocked(helper, player))
                .thenExecute(() -> container(helper, player).clicked(0, 0, ClickType.PICKUP, player))
                .thenWaitUntil(() -> helper.assertValueEqual(singles.get(), 1, "single clicks"))
                .thenWaitUntil(() -> helper.assertTrue(System.currentTimeMillis() - singleClickedAt.get() >= 250, "waiting for the click's lock"))
                .thenWaitUntil(() -> unlocked(helper, player))
                .thenExecute(() -> container(helper, player).clicked(0, 0, ClickType.PICKUP, player))
                .thenWaitUntil(() -> helper.assertValueEqual(doubles.get(), 1, "double clicks"))
                .thenExecute(() -> settings.setDoubleClickDelay(delay))
                .thenSucceed();
    }

    @AtlantisGameTest(batch = BATCH, timeoutTicks = TIMEOUT)
    public static void clickingOutsideTheScreenRunsTheOption(GameTestHelper helper) {
        var player = GameTestPlayers.mock(helper);
        var outside = new AtomicInteger();
        var page = AtlantisUI.createPage(PageOptions.builder().outsideScreenClick(__ -> outside.incrementAndGet()).build());

        AtlantisUI.open(player, page);

        helper.startSequence()
                .thenWaitUntil(() -> unlocked(helper, player))
                .thenExecute(() -> container(helper, player).clicked(AtlantisContainerMenuInternal.OUTSIDE_SCREEN_INDEX, 0, ClickType.PICKUP, player))
                .thenWaitUntil(() -> helper.assertValueEqual(outside.get(), 1, "outside clicks"))
                .thenSucceed();
    }

    @AtlantisGameTest(batch = BATCH, timeoutTicks = TIMEOUT)
    public static void paginationMovesBetweenPages(GameTestHelper helper) {
        var player = GameTestPlayers.mock(helper);
        var page = new PagedPage();

        AtlantisUI.open(player, page);

        helper.startSequence()
                .thenWaitUntil(() -> container(helper, player))
                .thenExecute(() -> {
                    var inventory = container(helper, player).getAquaUIInventory();
                    helper.assertValueEqual(AtlantisUI.currentPage(player, 0), 0, "first page");
                    helper.assertTrue(inventory.getItem(0).is(PagedPage.ITEMS[0]), "page 0 shown");
                    helper.assertTrue(AtlantisUI.hasNextPage(player, 0), "has a next page");
                    helper.assertFalse(AtlantisUI.hasPrevPage(player, 0), "first page has a previous page");

                    helper.assertTrue(AtlantisUI.nextPage(player), "nextPage");
                    helper.assertValueEqual(AtlantisUI.currentPage(player, 0), 1, "after nextPage");
                    helper.assertTrue(inventory.getItem(0).is(PagedPage.ITEMS[1]), "page 1 shown");

                    helper.assertTrue(AtlantisUI.setPage(player, 2), "setPage");
                    helper.assertValueEqual(AtlantisUI.currentPage(player, 0), 2, "after setPage");
                    helper.assertFalse(AtlantisUI.nextPage(player, false), "nextPage past the end without looping");
                    helper.assertValueEqual(AtlantisUI.currentPage(player, 0), 2, "after a failed nextPage");
                    helper.assertTrue(AtlantisUI.nextPage(player, true), "looped nextPage");
                    helper.assertValueEqual(AtlantisUI.currentPage(player, 0), 0, "after a looped nextPage");

                    helper.assertTrue(AtlantisUI.prevPage(player, true), "looped prevPage");
                    helper.assertValueEqual(AtlantisUI.currentPage(player, 0), 2, "after a looped prevPage");
                    helper.assertTrue(AtlantisUI.changePage(player, 0, -2, false), "changePage");
                    helper.assertValueEqual(AtlantisUI.currentPage(player, 0), 0, "after changePage");

                    helper.assertTrue(AtlantisUI.nextPage(player, 1), "nextPage of group 1");
                    helper.assertValueEqual(AtlantisUI.currentPage(player, 1), 1, "group 1 page");
                    helper.assertValueEqual(AtlantisUI.currentPage(player, 0), 0, "group 0 page after paging group 1");
                    helper.assertTrue(inventory.getItem(9).is(PagedPage.ITEMS[1]), "group 1 page 1 shown");
                })
                .thenSucceed();
    }

    @AtlantisGameTest(batch = BATCH, timeoutTicks = TIMEOUT)
    public static void updateTasksRunAtTheRefreshRate(GameTestHelper helper) {
        var player = GameTestPlayers.mock(helper);
        var runs = new AtomicInteger();
        var page = AtlantisUI.createPage(
                PageOptions.builder().refreshRate(1).build(),
                Button.builder().item(Items.CLOCK).index(0).updateTask((button, tick, task) -> runs.incrementAndGet()).build()
        );

        AtlantisUI.open(player, page);

        helper.succeedWhen(() -> helper.assertTrue(runs.get() >= 5, "update task ran " + runs.get() + " times"));
    }

    @AtlantisGameTest(batch = BATCH, timeoutTicks = TIMEOUT)
    public static void closingFiresDestructPage(GameTestHelper helper) {
        var player = GameTestPlayers.mock(helper);
        var destructed = new AtomicBoolean();
        var page = AtlantisUI.createPage(PageOptions.builder().build());
        listen(UEvent.DESTRUCT_PAGE, player, payload -> destructed.set(true));

        AtlantisUI.open(player, page);

        helper.startSequence()
                .thenWaitUntil(() -> unlocked(helper, player))
                .thenExecute(() -> AtlantisUI.close(player))
                .thenWaitUntil(() -> {
                    helper.assertTrue(destructed.get(), "DESTRUCT_PAGE did not fire");
                    helper.assertTrue(player.containerMenu == player.inventoryMenu, "page still open");
                })
                .thenSucceed();
    }

    @AtlantisGameTest(batch = BATCH, timeoutTicks = TIMEOUT)
    public static void cancellingDestructPageReopensThePage(GameTestHelper helper) {
        var player = GameTestPlayers.mock(helper);
        var destructs = new AtomicInteger();
        var page = AtlantisUI.createPage(PageOptions.builder().build());
        listen(UEvent.DESTRUCT_PAGE, player, payload -> {
            if (destructs.incrementAndGet() == 1) {
                payload.cancel();
            }
        });

        AtlantisUI.open(player, page);

        helper.startSequence()
                .thenWaitUntil(() -> unlocked(helper, player))
                .thenExecute(player::closeContainer)
                .thenWaitUntil(() -> helper.assertValueEqual(destructs.get(), 1, "destructs"))
                .thenWaitUntil(() -> helper.assertTrue(container(helper, player).getAquaUIInventory().page() == page, "page was not reopened"))
                .thenSucceed();
    }

    @AtlantisGameTest(batch = BATCH, timeoutTicks = TIMEOUT)
    public static void allowExitFalseReopensThePage(GameTestHelper helper) {
        var player = GameTestPlayers.mock(helper);
        var page = AtlantisPage.builder().allowExit(false).build();

        AtlantisUI.open(player, page);

        helper.startSequence()
                .thenWaitUntil(() -> unlocked(helper, player))
                .thenExecute(player::closeContainer)
                .thenExecute(() -> helper.assertTrue(AtlantisUI.FUTURE_OPEN.get(player.getUUID()) == page, "page was not queued to reopen"))
                .thenWaitUntil(() -> helper.assertTrue(container(helper, player).getAquaUIInventory().page() == page, "page was not reopened"))
                .thenSucceed();
    }

    @AtlantisGameTest(batch = BATCH, timeoutTicks = TIMEOUT)
    public static void draggablePagesHoldRealItems(GameTestHelper helper) {
        var player = GameTestPlayers.mock(helper);
        player.getInventory().setItem(0, Items.APPLE.getDefaultInstance());
        var page = AtlantisUI.createPage(PageOptions.builder().rows(1).draggable(true).build());

        AtlantisUI.open(player, page);

        helper.startSequence()
                .thenWaitUntil(() -> container(helper, player))
                .thenExecute(() -> {
                    var menu = container(helper, player);
                    helper.assertTrue(menu.getAquaUIInventory().isDraggable(), "page not draggable");
                    menu.quickMoveStack(player, 9 + 27);
                    helper.assertTrue(menu.getAquaUIInventory().getItem(0).is(Items.APPLE), "item was not moved into the page");
                    helper.assertTrue(player.getInventory().getItem(0).isEmpty(), "item still in the hotbar");
                })
                .thenSucceed();
    }

    @AtlantisGameTest(batch = BATCH, timeoutTicks = TIMEOUT)
    public static void builtInPagesOpen(GameTestHelper helper) {
        var testPagePlayer = GameTestPlayers.mock(helper);
        var editorPlayer = GameTestPlayers.mock(helper);
        var palettePlayer = GameTestPlayers.mock(helper);

        AtlantisUI.open(testPagePlayer, new TestPage());
        AtlantisUI.open(editorPlayer, new DecorationEditor(EditedPage.class));
        AtlantisUI.open(palettePlayer, new DecorationPalette(new DecorationEditor(EditedPage.class)));

        helper.succeedWhen(() -> {
            helper.assertTrue(container(helper, testPagePlayer).getAquaUIInventory().page() instanceof TestPage, "TestPage");
            helper.assertTrue(container(helper, editorPlayer).getAquaUIInventory().page() instanceof DecorationEditor, "DecorationEditor");
            helper.assertTrue(container(helper, palettePlayer).getAquaUIInventory().page() instanceof DecorationPalette, "DecorationPalette");
        });
    }

    private static AtlantisContainerMenuInternal container(GameTestHelper helper, ServerPlayer player) {
        return AtlantisUI.currentContainer(player).orElseThrow(() -> new GameTestAssertException("no page is open"));
    }

    private static void unlocked(GameTestHelper helper, ServerPlayer player) {
        container(helper, player);
        helper.assertFalse(AtlantisUI.LOCKED.contains(player.getUUID()), "player is locked");
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static <P extends PageEventPayload> void listen(de.waterdu.atlantis.util.config.Key<? extends UEvent<P>> key, ServerPlayer player, Consumer<P> listener) {
        var reference = PlayerReference.get(player);
        Mods.modLoader().event((de.waterdu.atlantis.util.config.Key) key).listen(payload -> {
            if (((PageEventPayload) payload).player() == reference) {
                listener.accept((P) payload);
            }
            return InteractionResult.PASS;
        });
    }

    public static final class DecoratedPage implements Page {}

    public static final class EditedPage implements Page {}

    private static final class PagedPage implements Page {

        static final net.minecraft.world.item.Item[] ITEMS = {Items.RED_WOOL, Items.GREEN_WOOL, Items.BLUE_WOOL};

        @Override
        public PageOptions options(PlayerReference player) {
            return PageOptions.builder().rows(2).build();
        }

        @Override
        public void addButtons(PlayerReference player, ButtonCollector collector) {
            for (var page = 0; page < 3; page++) {
                collector.collect(Button.builder().item(ITEMS[page]).index(0).page(page).group(0).build());
            }
            for (var page = 0; page < 2; page++) {
                collector.collect(Button.builder().item(ITEMS[page]).index(9).page(page).group(1).build());
            }
        }
    }
}
