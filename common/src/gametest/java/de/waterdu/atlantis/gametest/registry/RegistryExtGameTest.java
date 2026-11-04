package de.waterdu.atlantis.gametest.registry;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import de.waterdu.atlantis.gametest.AtlantisGameTest;
import de.waterdu.atlantis.util.config.Configurable;
import de.waterdu.atlantis.util.item.ConfigurableItemStack;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.flat.FlatLayerInfo;

/**
 * The registry helpers resolve through ServerUtils.registry. That method needs a running server.
 */
public final class RegistryExtGameTest {

    private static final ResourceLocation DIAMOND = ResourceLocation.withDefaultNamespace("diamond");

    /**
     * Prevents instantiation.
     */
    private RegistryExtGameTest() {}

    /**
     * Checks that the {@code ResourceKeyExt} methods resolve values, holders and registries for items and biomes.
     *
     * @param helper the GameTest helper
     */
    @AtlantisGameTest
    public static void resourceKeyExt(GameTestHelper helper) {
        var key = ResourceKey.create(Registries.ITEM, DIAMOND);

        helper.assertTrue(key.get() == Items.DIAMOND, "get()");
        helper.assertTrue(key.holder().value() == Items.DIAMOND, "holder()");
        helper.assertTrue(key.registryAccess() == BuiltInRegistries.ITEM, "registryAccess()");
        helper.assertTrue(Registries.ITEM.registryFromRegistryKey() == BuiltInRegistries.ITEM, "registryFromRegistryKey()");
        var plains = ResourceKey.create(Registries.BIOME, Biomes.PLAINS.location());
        helper.assertTrue(plains.get() != null, "biome get()");
        helper.assertTrue(plains.holder().is(Biomes.PLAINS), "biome holder()");
        helper.succeed();
    }

    /**
     * Checks that the {@code ResourceLocationExt} methods convert a resource location to keys, registry values, items,
     * blocks and configurable item stacks.
     *
     * @param helper the GameTest helper
     */
    @AtlantisGameTest
    public static void resourceLocationExt(GameTestHelper helper) {
        var registries = helper.getLevel().getServer().registryAccess();

        helper.assertTrue(DIAMOND.toResourceKey(Registries.ITEM).equals(ResourceKey.create(Registries.ITEM, DIAMOND)), "toResourceKey()");
        helper.assertTrue(DIAMOND.get(BuiltInRegistries.ITEM) == Items.DIAMOND, "get(Registry)");
        helper.assertTrue(Biomes.PLAINS.location().<Biome>get(Registries.BIOME, registries) != null, "get(key, provider)");
        helper.assertTrue(DIAMOND.asItem() == Items.DIAMOND, "asItem()");
        helper.assertTrue(ResourceLocation.withDefaultNamespace("stone").asBlock() == Blocks.STONE, "asBlock()");
        helper.assertTrue(DIAMOND.asConfigurableItemStack().stack().is(Items.DIAMOND), "asConfigurableItemStack()");
        helper.succeed();
    }

    /**
     * Checks that {@link ConfigurableItemStack} parses the damage, name, lore, glow and model properties.
     *
     * @param helper the GameTest helper
     */
    @AtlantisGameTest
    public static void configurableItemStackParsesProperties(GameTestHelper helper) {
        var stack = ConfigurableItemStack.of("minecraft:diamond_sword #5 name:Cool_Sword lore:Sharp glow model:7").stack();

        helper.assertTrue(stack.is(Items.DIAMOND_SWORD), "item");
        helper.assertValueEqual(stack.getDamageValue(), 5, "damage");
        helper.assertValueEqual(stack.get(DataComponents.ITEM_NAME).getString(), "Cool Sword", "name");
        helper.assertValueEqual(stack.get(DataComponents.LORE).lines().getFirst().getString(), "Sharp", "lore");
        helper.assertValueEqual(stack.get(DataComponents.ENCHANTMENT_GLINT_OVERRIDE), Boolean.TRUE, "glow");
        helper.assertValueEqual(stack.get(DataComponents.CUSTOM_MODEL_DATA).value(), 7, "model");
        helper.succeed();
    }

    /**
     * Checks that {@link ConfigurableItemStack} parses {@code x} and bare counts, and that unknown items become air.
     *
     * @param helper the GameTest helper
     */
    @AtlantisGameTest
    public static void configurableItemStackParsesCounts(GameTestHelper helper) {
        helper.assertValueEqual(ConfigurableItemStack.of("minecraft:diamond x12").stack().getCount(), 12, "x count");
        helper.assertValueEqual(ConfigurableItemStack.of("minecraft:diamond 7").stack().getCount(), 7, "bare count");
        helper.assertTrue(ConfigurableItemStack.of("minecraft:not_an_item").stack().is(Items.AIR), "unknown items are air");
        helper.succeed();
    }

    /**
     * Checks that {@link ConfigurableItemStack} parses an enchantment with its level.
     *
     * @param helper the GameTest helper
     */
    @AtlantisGameTest
    public static void configurableItemStackParsesEnchantments(GameTestHelper helper) {
        var stack = ConfigurableItemStack.of("minecraft:diamond_sword enchantment:minecraft:sharpness-3").stack();
        var sharpness = helper.getLevel().registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolderOrThrow(Enchantments.SHARPNESS);

        helper.assertValueEqual(stack.getEnchantments().getLevel(sharpness), 3, "sharpness level");
        helper.succeed();
    }

    /**
     * Checks that {@link ConfigurableItemStack} parses a JSON object of data components.
     *
     * @param helper the GameTest helper
     */
    @AtlantisGameTest
    public static void configurableItemStackParsesComponents(GameTestHelper helper) {
        var stack = ConfigurableItemStack.of("minecraft:diamond {\"minecraft:max_stack_size\":3}").stack();

        helper.assertValueEqual(stack.getMaxStackSize(), 3, "max stack size component");
        helper.succeed();
    }

    /**
     * Checks that the default {@link Configurable} contexts deserialize registry values from JSON, and serialize an
     * item back to its ID.
     *
     * @param helper the GameTest helper
     */
    @AtlantisGameTest
    public static void defaultConfigurableContextsResolveRegistries(GameTestHelper helper) {
        var builder = new GsonBuilder();
        Configurable.registerDefaultContexts(builder);
        builder.registerTypeAdapter(ConfigurableItemStack.class, new ConfigurableItemStack.TypeAdapter());
        var gson = builder.create();

        helper.assertTrue(deserialize(gson, "\"minecraft:diamond\"", Item.class) == Items.DIAMOND, "Item");
        helper.assertTrue(deserialize(gson, "\"minecraft:stone\"", Block.class) == Blocks.STONE, "Block");
        helper.assertTrue(deserialize(gson, "\"minecraft:entity.experience_orb.pickup\"", SoundEvent.class) == SoundEvents.EXPERIENCE_ORB_PICKUP, "SoundEvent");
        helper.assertTrue(deserialize(gson, "\"minecraft:speed\"", MobEffect.class) == MobEffects.MOVEMENT_SPEED.value(), "MobEffect");
        helper.assertTrue(deserialize(gson, "\"minecraft:plains\"", Biome.class) != null, "Biome");
        var layer = deserialize(gson, "{\"height\":3,\"block\":\"minecraft:stone\"}", FlatLayerInfo.class);
        helper.assertValueEqual(layer.getHeight(), 3, "FlatLayerInfo height");
        helper.assertTrue(layer.getBlockState().is(Blocks.STONE), "FlatLayerInfo block");

        var serialized = gson.toJson(Configurable.of(Items.DIAMOND), TypeToken.getParameterized(Configurable.class, Item.class).getType());
        helper.assertValueEqual(JsonParser.parseString(serialized).getAsString(), "minecraft:diamond", "Item round trip");
        helper.succeed();
    }

    /**
     * Deserializes the JSON as a {@link Configurable} of the type and gets its value.
     *
     * @param gson the Gson instance to use
     * @param json the JSON to read
     * @param type the class of the value
     * @param <T> the type of the value
     * @return the value of the deserialized configurable
     */
    private static <T> T deserialize(com.google.gson.Gson gson, String json, Class<T> type) {
        Configurable<T> configurable = gson.fromJson(json, TypeToken.getParameterized(Configurable.class, type).getType());
        return configurable.get();
    }
}
