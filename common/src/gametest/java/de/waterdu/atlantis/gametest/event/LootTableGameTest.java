package de.waterdu.atlantis.gametest.event;

import de.waterdu.atlantis.gametest.AtlantisGameTest;
import de.waterdu.atlantis.mod.Mods;
import de.waterdu.atlantis.mod.impl.common.event.item.LootTableEventPayload;
import de.waterdu.atlantis.mod.wrapper.modloader.event.UEvent;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.function.Consumer;

/**
 * Tests the {@link UEvent#LOOT_TABLE_PRE} and {@link UEvent#LOOT_TABLE_POST} events with the dirt block loot table.
 */
public final class LootTableGameTest {

    /**
     * Prevents instantiation.
     */
    private LootTableGameTest() {}

    /**
     * Checks that the loot table drops dirt when no listener changes it.
     *
     * @param helper the helper of the running test
     */
    @AtlantisGameTest
    public static void rollsNormallyWithoutListeners(GameTestHelper helper) {
        var drops = roll(helper, origin(helper));

        assertDrops(helper, drops, Items.DIRT);
        helper.succeed();
    }

    /**
     * Checks that the loot table drops nothing when a listener cancels the pre event.
     *
     * @param helper the helper of the running test
     */
    @AtlantisGameTest
    public static void cancellingPreDropsNothing(GameTestHelper helper) {
        var origin = origin(helper);
        listenPre(origin, LootTableEventPayload::cancel);

        assertDrops(helper, roll(helper, origin));
        helper.succeed();
    }

    /**
     * Checks that drops that a pre listener overrides replace the normal drops.
     *
     * @param helper the helper of the running test
     */
    @AtlantisGameTest
    public static void preCanOverrideDrops(GameTestHelper helper) {
        var origin = origin(helper);
        listenPre(origin, payload -> payload.overrideDrops(new ItemStack(Items.DIAMOND)));

        assertDrops(helper, roll(helper, origin), Items.DIAMOND);
        helper.succeed();
    }

    /**
     * Checks that drops that a pre listener adds come before the normal drops.
     *
     * @param helper the helper of the running test
     */
    @AtlantisGameTest
    public static void preCanAddDrops(GameTestHelper helper) {
        var origin = origin(helper);
        listenPre(origin, payload -> payload.addDrops(new ItemStack(Items.EMERALD)));

        assertDrops(helper, roll(helper, origin), Items.EMERALD, Items.DIRT);
        helper.succeed();
    }

    /**
     * Checks that the loot table drops nothing when a listener cancels the post event.
     *
     * @param helper the helper of the running test
     */
    @AtlantisGameTest
    public static void cancellingPostDropsNothing(GameTestHelper helper) {
        var origin = origin(helper);
        listenPost(origin, LootTableEventPayload::cancel);

        assertDrops(helper, roll(helper, origin));
        helper.succeed();
    }

    /**
     * Checks that drops that a post listener overrides replace the rolled drops, with their count.
     *
     * @param helper the helper of the running test
     */
    @AtlantisGameTest
    public static void postCanReplaceDrops(GameTestHelper helper) {
        var origin = origin(helper);
        listenPost(origin, payload -> payload.overrideDrops(new ItemStack(Items.GOLD_INGOT, 2)));

        var drops = roll(helper, origin);

        assertDrops(helper, drops, Items.GOLD_INGOT);
        helper.assertValueEqual(drops.getFirst().getCount(), 2, "count");
        helper.succeed();
    }

    /**
     * Checks that a post listener gets the drops that the loot table rolled.
     *
     * @param helper the helper of the running test
     */
    @AtlantisGameTest
    public static void postSeesTheRolledDrops(GameTestHelper helper) {
        var origin = origin(helper);
        var seen = new java.util.ArrayList<ItemStack>();
        listenPost(origin, payload -> seen.addAll(payload.drops()));

        roll(helper, origin);

        assertDrops(helper, seen, Items.DIRT);
        helper.succeed();
    }

    /**
     * Gets the loot origin of the test. Each test has its own structure, so the listeners see only their own rolls.
     *
     * @param helper the helper of the running test
     * @return the absolute position of the origin
     */
    private static Vec3 origin(GameTestHelper helper) {
        return helper.absoluteVec(new Vec3(1.5, 2, 1.5));
    }

    /**
     * Rolls the dirt block loot table at the origin, with an empty tool.
     *
     * @param helper the helper of the running test
     * @param origin the origin of the loot
     * @return the drops
     */
    private static List<ItemStack> roll(GameTestHelper helper, Vec3 origin) {
        var level = helper.getLevel();
        var table = level.getServer().reloadableRegistries().getLootTable(Blocks.DIRT.getLootTable());
        var params = new LootParams.Builder(level)
                .withParameter(LootContextParams.ORIGIN, origin)
                .withParameter(LootContextParams.TOOL, ItemStack.EMPTY)
                .withParameter(LootContextParams.BLOCK_STATE, Blocks.DIRT.defaultBlockState())
                .create(LootContextParamSets.BLOCK);
        return table.getRandomItems(params);
    }

    /**
     * Checks that the items of the drops are the expected items, in the same order.
     *
     * @param helper the helper of the running test
     * @param drops the drops to check
     * @param expected the expected items
     */
    private static void assertDrops(GameTestHelper helper, List<ItemStack> drops, net.minecraft.world.item.Item... expected) {
        helper.assertValueEqual(drops.stream().map(ItemStack::getItem).toList(), List.of(expected), "drops");
    }

    /**
     * Listens for {@link UEvent#LOOT_TABLE_PRE} and gives the payloads with the given origin to the listener.
     *
     * @param origin the origin to listen for
     * @param listener receives each payload with the origin
     */
    private static void listenPre(Vec3 origin, Consumer<LootTableEventPayload.Pre> listener) {
        Mods.modLoader().event(UEvent.LOOT_TABLE_PRE).listen(payload -> {
            if (payload.parameter(LootContextParams.ORIGIN).filter(origin::equals).isPresent()) {
                listener.accept(payload);
            }
            return InteractionResult.PASS;
        });
    }

    /**
     * Listens for {@link UEvent#LOOT_TABLE_POST} and gives the payloads with the given origin to the listener.
     *
     * @param origin the origin to listen for
     * @param listener receives each payload with the origin
     */
    private static void listenPost(Vec3 origin, Consumer<LootTableEventPayload.Post> listener) {
        Mods.modLoader().event(UEvent.LOOT_TABLE_POST).listen(payload -> {
            if (payload.parameter(LootContextParams.ORIGIN).filter(origin::equals).isPresent()) {
                listener.accept(payload);
            }
            return InteractionResult.PASS;
        });
    }
}
