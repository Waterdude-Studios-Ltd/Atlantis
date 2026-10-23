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

public final class LootTableGameTest {

    private LootTableGameTest() {}

    @AtlantisGameTest
    public static void rollsNormallyWithoutListeners(GameTestHelper helper) {
        var drops = roll(helper, origin(helper));

        assertDrops(helper, drops, Items.DIRT);
        helper.succeed();
    }

    @AtlantisGameTest
    public static void cancellingPreDropsNothing(GameTestHelper helper) {
        var origin = origin(helper);
        listenPre(origin, LootTableEventPayload::cancel);

        assertDrops(helper, roll(helper, origin));
        helper.succeed();
    }

    @AtlantisGameTest
    public static void preCanOverrideDrops(GameTestHelper helper) {
        var origin = origin(helper);
        listenPre(origin, payload -> payload.overrideDrops(new ItemStack(Items.DIAMOND)));

        assertDrops(helper, roll(helper, origin), Items.DIAMOND);
        helper.succeed();
    }

    @AtlantisGameTest
    public static void preCanAddDrops(GameTestHelper helper) {
        var origin = origin(helper);
        listenPre(origin, payload -> payload.addDrops(new ItemStack(Items.EMERALD)));

        assertDrops(helper, roll(helper, origin), Items.EMERALD, Items.DIRT);
        helper.succeed();
    }

    @AtlantisGameTest
    public static void cancellingPostDropsNothing(GameTestHelper helper) {
        var origin = origin(helper);
        listenPost(origin, LootTableEventPayload::cancel);

        assertDrops(helper, roll(helper, origin));
        helper.succeed();
    }

    @AtlantisGameTest
    public static void postCanReplaceDrops(GameTestHelper helper) {
        var origin = origin(helper);
        listenPost(origin, payload -> payload.overrideDrops(new ItemStack(Items.GOLD_INGOT, 2)));

        var drops = roll(helper, origin);

        assertDrops(helper, drops, Items.GOLD_INGOT);
        helper.assertValueEqual(drops.getFirst().getCount(), 2, "count");
        helper.succeed();
    }

    @AtlantisGameTest
    public static void postSeesTheRolledDrops(GameTestHelper helper) {
        var origin = origin(helper);
        var seen = new java.util.ArrayList<ItemStack>();
        listenPost(origin, payload -> seen.addAll(payload.drops()));

        roll(helper, origin);

        assertDrops(helper, seen, Items.DIRT);
        helper.succeed();
    }

    private static Vec3 origin(GameTestHelper helper) {
        return helper.absoluteVec(new Vec3(1.5, 2, 1.5));
    }

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

    private static void assertDrops(GameTestHelper helper, List<ItemStack> drops, net.minecraft.world.item.Item... expected) {
        helper.assertValueEqual(drops.stream().map(ItemStack::getItem).toList(), List.of(expected), "drops");
    }

    private static void listenPre(Vec3 origin, Consumer<LootTableEventPayload.Pre> listener) {
        Mods.modLoader().event(UEvent.LOOT_TABLE_PRE).listen(payload -> {
            if (payload.parameter(LootContextParams.ORIGIN).filter(origin::equals).isPresent()) {
                listener.accept(payload);
            }
            return InteractionResult.PASS;
        });
    }

    private static void listenPost(Vec3 origin, Consumer<LootTableEventPayload.Post> listener) {
        Mods.modLoader().event(UEvent.LOOT_TABLE_POST).listen(payload -> {
            if (payload.parameter(LootContextParams.ORIGIN).filter(origin::equals).isPresent()) {
                listener.accept(payload);
            }
            return InteractionResult.PASS;
        });
    }
}
