package de.waterdu.atlantis.gametest.util;

import de.waterdu.atlantis.gametest.AtlantisGameTest;
import de.waterdu.atlantis.gametest.GameTestPlayers;
import de.waterdu.atlantis.util.item.BlockUtils;
import de.waterdu.atlantis.util.item.ConfigurableItemStack;
import de.waterdu.atlantis.util.item.ItemBuilder;
import de.waterdu.atlantis.util.item.ItemUtils;
import de.waterdu.atlantis.util.java.collection.Pair;
import de.waterdu.atlantis.util.level.ConfigurableLevel;
import de.waterdu.atlantis.util.level.LevelPosition;
import de.waterdu.atlantis.util.level.LocalPosition;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;

/**
 * ItemUtils, ItemBuilder, BlockUtils and ConfigurableItemStack parsing against live registries and a live world.
 */
public final class ItemUtilsGameTest {

    private static final String BATCH = "atlantis_items";
    private static final BlockPos INSIDE = new BlockPos(2, 2, 2);
    private static final BlockPos FLOOR = new BlockPos(2, 1, 2);

    private ItemUtilsGameTest() {}

    @AtlantisGameTest(batch = BATCH)
    public static void itemLookupResolvesIdsAndTreatsAirAsAbsent(GameTestHelper helper) {
        helper.assertTrue(ItemUtils.item("minecraft:diamond").orElse(null) == Items.DIAMOND, "diamond not found");
        helper.assertTrue(ItemUtils.item(ResourceLocation.withDefaultNamespace("stone")).orElse(null) == Items.STONE, "stone not found");
        helper.assertTrue(ItemUtils.item("minecraft:air").isEmpty(), "air is not absent by default");
        helper.assertTrue(ItemUtils.item("minecraft:air", false).orElse(null) == Items.AIR, "air not found with airIsNull=false");
        helper.assertValueEqual(ItemUtils.id(Items.DIAMOND), "minecraft:diamond", "item id");
        helper.succeed();
    }

    @AtlantisGameTest(batch = BATCH)
    public static void blockLookupResolvesIdsAndTreatsAirAsAbsent(GameTestHelper helper) {
        helper.assertTrue(ItemUtils.getBlock("minecraft:stone").orElse(null) == Blocks.STONE, "ItemUtils: stone not found");
        helper.assertTrue(ItemUtils.getBlock("minecraft:air").isEmpty(), "ItemUtils: air is not absent by default");
        helper.assertTrue(ItemUtils.getBlock("minecraft:air", false).orElse(null) == Blocks.AIR, "ItemUtils: air not found with airIsNull=false");
        helper.assertTrue(BlockUtils.getBlock("minecraft:gold_block").orElse(null) == Blocks.GOLD_BLOCK, "BlockUtils: gold block not found");
        helper.assertTrue(BlockUtils.getBlock("minecraft:air").isEmpty(), "BlockUtils: air is not absent by default");
        helper.assertTrue(BlockUtils.getBlock("minecraft:air", false).orElse(null) == Blocks.AIR, "BlockUtils: air not found with airIsNull=false");
        helper.assertValueEqual(ItemUtils.id(Blocks.STONE), "minecraft:stone", "block id");
        helper.succeed();
    }

    @AtlantisGameTest(batch = BATCH)
    public static void unknownIdsAreAbsentEvenWhenAirIsAllowed(GameTestHelper helper) {
        var missing = "atlantis_test:missing";
        helper.assertTrue(ItemUtils.item(missing).isEmpty(), "unknown item found");
        helper.assertTrue(ItemUtils.item(missing, false).isEmpty(), "unknown item found with airIsNull=false");
        helper.assertTrue(ItemUtils.getBlock(missing, false).isEmpty(), "ItemUtils: unknown block found with airIsNull=false");
        helper.assertTrue(BlockUtils.getBlock(missing, false).isEmpty(), "BlockUtils: unknown block found with airIsNull=false");
        helper.succeed();
    }

    @AtlantisGameTest(batch = BATCH)
    public static void setDisplayNameRenamesTheStack(GameTestHelper helper) {
        var stack = new ItemStack(Items.PAPER);

        ItemUtils.setDisplayName(stack, "Shiny Paper");

        helper.assertValueEqual(stack.getHoverName().getString(), "Shiny Paper", "display name");
        helper.succeed();
    }

    @AtlantisGameTest(batch = BATCH)
    public static void setDisplayNameReplacesAnExistingCustomName(GameTestHelper helper) {
        var stack = new ItemStack(Items.PAPER);
        stack.set(DataComponents.CUSTOM_NAME, Component.literal("Anvil Name"));

        ItemUtils.setDisplayName(stack, "New Name");

        helper.assertValueEqual(stack.getHoverName().getString(), "New Name", "display name");
        helper.succeed();
    }

    @AtlantisGameTest(batch = BATCH)
    public static void loreCanBeSetAddedAndPrepended(GameTestHelper helper) {
        var stack = new ItemStack(Items.PAPER);

        ItemUtils.setDisplayLore(stack, "A", "B");
        helper.assertValueEqual(lore(stack), List.of("A", "B"), "lore after set");

        ItemUtils.addDisplayLore(stack, "C");
        helper.assertValueEqual(lore(stack), List.of("A", "B", "C"), "lore after add");

        ItemUtils.prependDisplayLore(stack, "Z");
        helper.assertValueEqual(lore(stack), List.of("Z", "A", "B", "C"), "lore after prepend");

        ItemUtils.addDisplayLore(stack, List.of("D", "E"));
        ItemUtils.prependDisplayLore(stack, List.of("Y"));
        helper.assertValueEqual(lore(stack), List.of("Y", "Z", "A", "B", "C", "D", "E"), "lore after collection add and prepend");

        ItemUtils.setDisplayLore(stack, List.of("Only"));
        helper.assertValueEqual(lore(stack), List.of("Only"), "lore after collection set");
        helper.succeed();
    }

    @AtlantisGameTest(batch = BATCH)
    public static void enchantmentsCanBeAddedRemovedAndCleared(GameTestHelper helper) {
        var sharpness = enchantment(helper, Enchantments.SHARPNESS);
        var knockback = enchantment(helper, Enchantments.KNOCKBACK);
        var stack = new ItemStack(Items.DIAMOND_SWORD);

        ItemUtils.addEnchantment(stack, sharpness, 3);
        ItemUtils.addEnchantment(stack, knockback, 1);
        helper.assertTrue(ItemUtils.hasEnchantment(stack, sharpness), "sharpness missing");
        helper.assertTrue(ItemUtils.hasEnchantment(stack, knockback), "knockback missing");
        helper.assertValueEqual(ItemUtils.getEnchantments(stack).getLevel(sharpness), 3, "sharpness level");

        ItemUtils.removeEnchantments(stack, sharpness);
        helper.assertFalse(ItemUtils.hasEnchantment(stack, sharpness), "sharpness not removed");
        helper.assertTrue(ItemUtils.hasEnchantment(stack, knockback), "knockback removed with sharpness");

        ItemUtils.clearEnchantments(stack);
        helper.assertFalse(ItemUtils.hasEnchantment(stack, knockback), "knockback not cleared");
        helper.assertTrue(ItemUtils.getEnchantments(stack).isEmpty(), "enchantments not cleared");
        helper.succeed();
    }

    @AtlantisGameTest(batch = BATCH)
    public static void setEnchantmentsReplacesTheDummyEnchantment(GameTestHelper helper) {
        var infinity = enchantment(helper, Enchantments.INFINITY);
        var sharpness = enchantment(helper, Enchantments.SHARPNESS);
        var stack = new ItemStack(Items.BOW);

        ItemUtils.addDummyEnchantment(stack);
        helper.assertTrue(ItemUtils.hasEnchantment(stack, infinity), "dummy enchantment missing");

        var replacement = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
        replacement.set(sharpness, 2);
        ItemUtils.setEnchantments(stack, replacement.toImmutable());

        helper.assertFalse(ItemUtils.hasEnchantment(stack, infinity), "dummy enchantment not replaced");
        helper.assertValueEqual(ItemUtils.getEnchantments(stack).getLevel(sharpness), 2, "sharpness level");
        helper.succeed();
    }

    @AtlantisGameTest(batch = BATCH)
    public static void glowCanBeAppliedHiddenAndRemoved(GameTestHelper helper) {
        var stack = new ItemStack(Items.DIAMOND_SWORD);
        helper.assertFalse(stack.hasFoil(), "plain sword glows");

        ItemUtils.applyGlow(stack);
        helper.assertTrue(stack.hasFoil(), "applyGlow did not make the stack glow");

        ItemUtils.addEnchantment(stack, enchantment(helper, Enchantments.SHARPNESS), 1);
        ItemUtils.hideGlow(stack);
        helper.assertFalse(stack.hasFoil(), "hideGlow did not hide the enchantment glint");

        ItemUtils.removeGlow(stack);
        helper.assertFalse(stack.has(DataComponents.ENCHANTMENT_GLINT_OVERRIDE), "glint override not removed");
        helper.assertTrue(stack.hasFoil(), "enchanted stack lost its natural glint");
        helper.succeed();
    }

    @AtlantisGameTest(batch = BATCH)
    public static void hideTooltipsHidesAttributesAndAdditionalTooltips(GameTestHelper helper) {
        var stack = new ItemStack(Items.DIAMOND_SWORD);

        ItemUtils.hideTooltips(stack);

        var modifiers = stack.get(DataComponents.ATTRIBUTE_MODIFIERS);
        helper.assertTrue(modifiers != null && !modifiers.showInTooltip(), "attribute modifiers still shown");
        helper.assertTrue(stack.has(DataComponents.HIDE_ADDITIONAL_TOOLTIP), "additional tooltip not hidden");
        helper.assertFalse(stack.has(DataComponents.HIDE_TOOLTIP), "whole tooltip hidden");

        ItemUtils.hideAllTooltips(stack);
        helper.assertTrue(stack.has(DataComponents.HIDE_TOOLTIP), "whole tooltip not hidden");
        helper.succeed();
    }

    @AtlantisGameTest(batch = BATCH)
    public static void customModelDataCanBeSetReadAndRemoved(GameTestHelper helper) {
        var stack = new ItemStack(Items.PAPER);
        helper.assertValueEqual(ItemUtils.getCustomModelData(stack), -1, "custom model data before set");

        ItemUtils.setCustomModelData(stack, 42);
        helper.assertValueEqual(ItemUtils.getCustomModelData(stack), 42, "custom model data after set");

        ItemUtils.removeCustomModelData(stack);
        helper.assertValueEqual(ItemUtils.getCustomModelData(stack), -1, "custom model data after remove");
        helper.succeed();
    }

    @AtlantisGameTest(batch = BATCH)
    public static void tagWritesCustomData(GameTestHelper helper) {
        var stack = new ItemStack(Items.PAPER);
        helper.assertFalse(ItemUtils.hasTag(stack, "atlantis_test"), "fresh stack has the tag");

        ItemUtils.tag(stack, tag -> tag.putInt("atlantis_test", 4));

        helper.assertTrue(ItemUtils.hasTag(stack, "atlantis_test"), "tag not written");
        helper.assertValueEqual(ItemUtils.tag(stack).copyTag().getInt("atlantis_test"), 4, "tag value");
        helper.succeed();
    }

    @AtlantisGameTest(batch = BATCH)
    public static void tagCreatesEmptyCustomDataWhenAbsent(GameTestHelper helper) {
        var stack = new ItemStack(Items.PAPER);
        helper.assertFalse(stack.has(DataComponents.CUSTOM_DATA), "fresh stack has custom data");

        var data = ItemUtils.tag(stack);

        helper.assertTrue(data.isEmpty(), "created custom data is not empty");
        helper.assertTrue(stack.has(DataComponents.CUSTOM_DATA), "custom data not stored on the stack");
        helper.succeed();
    }

    @AtlantisGameTest(batch = BATCH)
    public static void computeIfAbsentOnlySetsMissingComponents(GameTestHelper helper) {
        var stack = new ItemStack(Items.PAPER);

        var first = ItemUtils.computeIfAbsent(stack, DataComponents.CUSTOM_MODEL_DATA, () -> new CustomModelData(3));
        var second = ItemUtils.computeIfAbsent(stack, DataComponents.CUSTOM_MODEL_DATA, () -> new CustomModelData(9));

        helper.assertValueEqual(first.value(), 3, "computed value");
        helper.assertValueEqual(second.value(), 3, "present value");
        helper.assertValueEqual(ItemUtils.getCustomModelData(stack), 3, "stored value");
        helper.succeed();
    }

    @AtlantisGameTest(batch = BATCH)
    public static void giveOrElseGivesWhenThereIsRoom(GameTestHelper helper) {
        var player = player(helper);
        var refused = new AtomicReference<ItemStack>();

        var given = ItemUtils.giveOrElse(player, new ItemStack(Items.EMERALD, 3), refused::set);

        helper.assertTrue(given, "giveOrElse reported a failure");
        helper.assertTrue(refused.get() == null, "orElse ran although there was room");
        helper.assertValueEqual(player.getInventory().countItem(Items.EMERALD), 3, "emeralds in the inventory");
        helper.succeed();
    }

    @AtlantisGameTest(batch = BATCH)
    public static void giveOrElseRunsOrElseWhenTheInventoryIsFull(GameTestHelper helper) {
        var player = player(helper);
        fill(player);

        var refused = new AtomicReference<ItemStack>();
        var given = ItemUtils.giveOrElse(player, new ItemStack(Items.EMERALD, 3), refused::set);

        helper.assertFalse(given, "giveOrElse reported success into a full inventory");
        helper.assertTrue(refused.get() != null && refused.get().is(Items.EMERALD), "orElse did not receive the stack");
        helper.assertValueEqual(refused.get().getCount(), 3, "refused count");
        helper.assertValueEqual(player.getInventory().countItem(Items.EMERALD), 0, "emeralds in the inventory");
        helper.succeed();
    }

    @AtlantisGameTest(batch = BATCH)
    public static void giveOrDropDropsTheRemainderOfAPartialGive(GameTestHelper helper) {
        var player = player(helper);
        fill(player);
        player.getInventory().items.set(0, new ItemStack(Items.DIAMOND, 60));
        var dropped = new AtomicReference<ItemEntity>();

        var given = ItemUtils.giveOrDrop(player, new ItemStack(Items.DIAMOND, 10), stack -> {}, dropped::set);

        helper.assertFalse(given, "giveOrDrop reported a complete give");
        helper.assertValueEqual(player.getInventory().items.getFirst().getCount(), 64, "diamonds given");
        helper.assertTrue(dropped.get() != null, "remainder was not dropped");
        helper.assertTrue(dropped.get().getItem().is(Items.DIAMOND), "dropped item is not a diamond");
        helper.assertValueEqual(dropped.get().getItem().getCount(), 6, "dropped count");
        helper.succeed();
    }

    @AtlantisGameTest(batch = BATCH)
    public static void dropSpawnsAnItemEntity(GameTestHelper helper) {
        var at = helper.absoluteVec(Vec3.atBottomCenterOf(INSIDE));
        var position = LevelPosition.of(ConfigurableLevel.of(helper.getLevel()), new LocalPosition(at.x, at.y, at.z));
        var modified = new AtomicReference<ItemEntity>();

        var dropped = ItemUtils.drop(new ItemStack(Items.EMERALD, 2), position, modified::set);

        helper.assertTrue(dropped.isPresent(), "no item entity created");
        var entity = dropped.get();
        helper.assertTrue(modified.get() == entity, "modifier did not receive the entity");
        helper.assertValueEqual(entity.getItem().getCount(), 2, "dropped count");
        helper.succeedWhen(() -> {
            helper.assertTrue(helper.getLevel().getEntity(entity.getUUID()) == entity, "item entity not in the level");
            helper.assertItemEntityPresent(Items.EMERALD, INSIDE, 1.0);
        });
    }

    @AtlantisGameTest(batch = BATCH)
    public static void builderBuildsAFullyConfiguredStack(GameTestHelper helper) {
        var sharpness = enchantment(helper, Enchantments.SHARPNESS);
        var tag = new CompoundTag();
        tag.putBoolean("atlantis_test", true);

        var stack = ItemBuilder.builder(Items.DIAMOND)
                .count(5)
                .name("Gem")
                .lore("First")
                .addLore("Second")
                .glow()
                .enchantment(sharpness, 2)
                .customModelData(11)
                .tag(tag)
                .component(DataComponents.RARITY, Rarity.EPIC)
                .buildAsStack();

        helper.assertTrue(stack.is(Items.DIAMOND), "wrong item");
        helper.assertValueEqual(stack.getCount(), 5, "count");
        helper.assertValueEqual(stack.getHoverName().getString(), "Gem", "name");
        helper.assertValueEqual(lore(stack), List.of("First", "Second"), "lore");
        helper.assertTrue(stack.hasFoil(), "no glow");
        helper.assertValueEqual(ItemUtils.getEnchantments(stack).getLevel(sharpness), 2, "sharpness level");
        helper.assertValueEqual(ItemUtils.getCustomModelData(stack), 11, "custom model data");
        helper.assertTrue(ItemUtils.hasTag(stack, "atlantis_test"), "tag missing");
        helper.assertValueEqual(stack.get(DataComponents.RARITY), Rarity.EPIC, "rarity");
        helper.succeed();
    }

    @AtlantisGameTest(batch = BATCH)
    public static void emptyBuilderTakesTheItem(GameTestHelper helper) {
        var stack = ItemBuilder.builder().item(Items.DIAMOND).buildAsStack();

        helper.assertTrue(stack.is(Items.DIAMOND), "builder without a stack did not take the item");
        helper.assertValueEqual(stack.getCount(), 1, "count");
        helper.succeed();
    }

    @AtlantisGameTest(batch = BATCH)
    public static void builderGiveOrDropGivesTheStack(GameTestHelper helper) {
        var player = player(helper);

        var given = ItemBuilder.builder(Items.EMERALD).count(3).giveOrDrop(player);

        helper.assertTrue(given, "giveOrDrop reported a failure");
        helper.assertValueEqual(player.getInventory().countItem(Items.EMERALD), 3, "emeralds in the inventory");
        helper.succeed();
    }

    @AtlantisGameTest(batch = BATCH)
    public static void setBlockPlacesIntoAir(GameTestHelper helper) {
        var player = player(helper);

        var placed = BlockUtils.setBlock(player, helper.absolutePos(INSIDE), Blocks.GOLD_BLOCK.defaultBlockState(), true);

        helper.assertTrue(placed, "setBlock reported a failure");
        helper.assertBlockPresent(Blocks.GOLD_BLOCK, INSIDE);
        helper.succeed();
    }

    @AtlantisGameTest(batch = BATCH)
    public static void setBlockIfAirLeavesOccupiedPositions(GameTestHelper helper) {
        var player = player(helper);

        var placed = BlockUtils.setBlock(player, helper.absolutePos(FLOOR), Blocks.GOLD_BLOCK.defaultBlockState(), true);

        helper.assertFalse(placed, "setBlock replaced a solid block with ifAir");
        helper.assertBlockPresent(Blocks.STONE, FLOOR);
        helper.succeed();
    }

    @AtlantisGameTest(batch = BATCH)
    public static void setBlockReplacesWithoutIfAir(GameTestHelper helper) {
        var player = player(helper);
        helper.setBlock(INSIDE, Blocks.DIRT);

        var placed = BlockUtils.setBlock(player, helper.absolutePos(INSIDE), Blocks.GOLD_BLOCK.defaultBlockState(), false);

        helper.assertTrue(placed, "setBlock reported a failure");
        helper.assertBlockPresent(Blocks.GOLD_BLOCK, INSIDE);
        helper.succeed();
    }

    @AtlantisGameTest(batch = BATCH)
    public static void removeBlockBreaksTheBlock(GameTestHelper helper) {
        var player = player(helper);
        helper.setBlock(INSIDE, Blocks.DIRT);
        var pos = helper.absolutePos(INSIDE);

        helper.assertTrue(BlockUtils.canPlaceBlock(player, pos), "player may not place");
        helper.assertTrue(BlockUtils.canRemoveBlock(player, pos), "player may not remove dirt");
        var removed = BlockUtils.removeBlock(player, pos);

        helper.assertTrue(removed, "removeBlock reported a failure");
        helper.assertBlockPresent(Blocks.AIR, INSIDE);
        helper.succeed();
    }

    @AtlantisGameTest(batch = BATCH)
    public static void removeBlockBreaksBlocksThatNeedAToolForDrops(GameTestHelper helper) {
        var player = player(helper);
        helper.setBlock(INSIDE, Blocks.STONE);
        var pos = helper.absolutePos(INSIDE);

        helper.assertTrue(BlockUtils.canRemoveBlock(player, pos), "player may not remove stone");
        var removed = BlockUtils.removeBlock(player, pos);

        helper.assertTrue(removed, "removeBlock reported a failure");
        helper.assertBlockPresent(Blocks.AIR, INSIDE);
        helper.succeed();
    }

    @AtlantisGameTest(batch = BATCH)
    public static void parsesCountShorthands(GameTestHelper helper) {
        helper.assertValueEqual(ConfigurableItemStack.of("minecraft:diamond").newStack().getCount(), 1, "default count");
        helper.assertValueEqual(ConfigurableItemStack.of("minecraft:diamond x3").newStack().getCount(), 3, "x count");
        helper.assertValueEqual(ConfigurableItemStack.of("minecraft:diamond 5").newStack().getCount(), 5, "bare count");
        helper.assertValueEqual(ConfigurableItemStack.of("minecraft:diamond count:7").newStack().getCount(), 7, "count processor");
        helper.succeed();
    }

    @AtlantisGameTest(batch = BATCH)
    public static void parsesDamage(GameTestHelper helper) {
        helper.assertValueEqual(ConfigurableItemStack.of("minecraft:diamond_sword #10").newStack().getDamageValue(), 10, "# damage");
        helper.assertValueEqual(ConfigurableItemStack.of("minecraft:diamond_sword damage:4").newStack().getDamageValue(), 4, "damage processor");
        helper.succeed();
    }

    @AtlantisGameTest(batch = BATCH)
    public static void parsesBuiltInProcessors(GameTestHelper helper) {
        var sharpness = enchantment(helper, Enchantments.SHARPNESS);
        var knockback = enchantment(helper, Enchantments.KNOCKBACK);

        var stack = ConfigurableItemStack.of("minecraft:diamond_sword enchantment:sharpness-3 enchantment:minecraft:knockback-2 glow name:Big_Sword model:7 lore:Line").newStack();

        helper.assertTrue(stack.is(Items.DIAMOND_SWORD), "wrong item");
        helper.assertValueEqual(ItemUtils.getEnchantments(stack).getLevel(sharpness), 3, "sharpness level");
        helper.assertValueEqual(ItemUtils.getEnchantments(stack).getLevel(knockback), 2, "knockback level");
        helper.assertValueEqual(stack.get(DataComponents.ENCHANTMENT_GLINT_OVERRIDE), Boolean.TRUE, "glint override");
        helper.assertValueEqual(stack.getHoverName().getString(), "Big Sword", "name");
        helper.assertValueEqual(ItemUtils.getCustomModelData(stack), 7, "custom model data");
        helper.assertValueEqual(lore(stack), List.of("Line"), "lore");
        helper.succeed();
    }

    @AtlantisGameTest(batch = BATCH)
    public static void parsesFlatComponents(GameTestHelper helper) {
        var spaced = ConfigurableItemStack.of("minecraft:diamond {\"minecraft:custom_model_data\":5}").newStack();
        var attached = ConfigurableItemStack.of("minecraft:diamond{\"minecraft:custom_model_data\":6}").newStack();

        helper.assertValueEqual(ItemUtils.getCustomModelData(spaced), 5, "custom model data after a space");
        helper.assertValueEqual(ItemUtils.getCustomModelData(attached), 6, "custom model data attached to the id");
        helper.succeed();
    }

    @AtlantisGameTest(batch = BATCH)
    public static void parsesNestedComponents(GameTestHelper helper) {
        var stack = ConfigurableItemStack.of("minecraft:diamond x2 {\"minecraft:custom_data\":{atlantis_test:1b}}").newStack();

        helper.assertTrue(stack.is(Items.DIAMOND), "wrong item");
        helper.assertValueEqual(stack.getCount(), 2, "count");
        helper.assertTrue(ItemUtils.hasTag(stack, "atlantis_test"), "nested custom data not applied");
        helper.succeed();
    }

    @AtlantisGameTest(batch = BATCH)
    public static void ofItemStackRoundTrips(GameTestHelper helper) {
        var original = ItemBuilder.builder(Items.DIAMOND).count(3).name("Gem").customModelData(8).buildAsStack();

        var parsed = ConfigurableItemStack.of(original).newStack();

        helper.assertTrue(ItemStack.matches(original, parsed), "round trip changed the stack: " + parsed + " " + parsed.getComponentsPatch());
        helper.succeed();
    }

    @AtlantisGameTest(batch = BATCH)
    public static void unknownItemParsesToAir(GameTestHelper helper) {
        var stack = ConfigurableItemStack.of("atlantis_test:missing_item x3").newStack();

        helper.assertTrue(stack.isEmpty(), "unknown item did not parse to an empty stack");
        helper.succeed();
    }

    @AtlantisGameTest(batch = BATCH)
    public static void registeredPreprocessorAndProcessorAreApplied(GameTestHelper helper) {
        ConfigurableItemStack.Preprocessor.register("#atlantis_gametest", (full, args) ->
                Pair.of(new ItemStack(Items.EMERALD), Arrays.copyOfRange(args, 1, args.length)));
        ConfigurableItemStack.Processor.register("atlantis_gametest_rarity", (kv, stack) ->
                stack.set(DataComponents.RARITY, Rarity.valueOf(kv[1].toUpperCase(Locale.ROOT))));

        var stack = ConfigurableItemStack.of("#atlantis_gametest atlantis_gametest_rarity:epic x4").newStack();

        helper.assertTrue(stack.is(Items.EMERALD), "preprocessor result not used");
        helper.assertValueEqual(stack.get(DataComponents.RARITY), Rarity.EPIC, "processor not applied");
        helper.assertValueEqual(stack.getCount(), 4, "remaining arguments not parsed");
        helper.succeed();
    }

    @AtlantisGameTest(batch = BATCH)
    public static void matchesItemsStacksAndStrings(GameTestHelper helper) {
        var configurable = ConfigurableItemStack.of("minecraft:diamond x2");

        var copy = configurable.newStack();
        copy.setCount(1);

        helper.assertTrue(configurable.matches(Items.DIAMOND), "does not match its item");
        helper.assertFalse(configurable.matches(Items.EMERALD), "matches another item");
        helper.assertTrue(configurable.matches(configurable.newStack()), "does not match its own stack");
        helper.assertFalse(configurable.matches(copy), "matches a stack with another count");
        helper.assertTrue(configurable.matches("MINECRAFT:DIAMOND X2"), "does not match its string ignoring case");
        helper.assertValueEqual(configurable.stack().getCount(), 2, "newStack shares state with the parsed stack");
        helper.assertTrue(ItemUtils.matches(Items.DIAMOND, ConfigurableItemStack.of("minecraft:emerald"), configurable), "ItemUtils.matches missed");
        helper.succeed();
    }

    private static ServerPlayer player(GameTestHelper helper) {
        var player = GameTestPlayers.mock(helper);
        var at = helper.absoluteVec(Vec3.atBottomCenterOf(INSIDE));
        player.moveTo(at.x, at.y, at.z);
        player.getAbilities().instabuild = false;
        return player;
    }

    private static void fill(ServerPlayer player) {
        var items = player.getInventory().items;
        for (var i = 0; i < items.size(); i++) {
            items.set(i, new ItemStack(Items.STONE, 64));
        }
    }

    private static Holder<Enchantment> enchantment(GameTestHelper helper, ResourceKey<Enchantment> key) {
        return helper.getLevel().registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolderOrThrow(key);
    }

    private static List<String> lore(ItemStack stack) {
        var lines = new ArrayList<String>();
        var lore = stack.get(DataComponents.LORE);
        if (lore != null) {
            for (var line : lore.lines()) {
                lines.add(line.getString());
            }
        }
        return lines;
    }
}
