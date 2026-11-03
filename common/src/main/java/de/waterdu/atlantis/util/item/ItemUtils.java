package de.waterdu.atlantis.util.item;

import de.waterdu.atlantis.util.entity.EntityUtils;
import de.waterdu.atlantis.util.java.ExceptionUtils;
import de.waterdu.atlantis.util.java.collection.CollectionUtils;
import de.waterdu.atlantis.util.java.interfaces.Lazy;
import de.waterdu.atlantis.util.level.LevelPosition;
import de.waterdu.atlantis.util.text.TextUtils;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

/**
 * Gets items and blocks by ID and changes the names, lore, enchantments and data components of item stacks.
 */
public final class ItemUtils {

    private static final String TAG_TOOLTIP = "tooltip";
    private static final Map<ResourceLocation, Optional<Item>> ITEMS = new HashMap<>();
    private static final Map<ResourceLocation, Optional<Block>> BLOCKS = new HashMap<>();
    private static final Lazy<ItemEnchantments> DUMMY_ENCHANTMENT = Lazy.of(() -> {
        var mutable = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
        mutable.set(Enchantments.INFINITY.holder(), 1);
        return mutable.toImmutable();
    });
    private static final CustomModelData NO_CUSTOM_MODEL_DATA = new CustomModelData(-1);

    private ItemUtils() {
        ExceptionUtils.throwCannotInstantiateClassException();
    }

    /**
     * @return the item registry
     */
    public static Registry<Item> itemRegistry() {
        return Registries.ITEM.registryFromRegistryKey();
    }

    /**
     * @return the block registry
     */
    public static Registry<Block> blockRegistry() {
        return Registries.BLOCK.registryFromRegistryKey();
    }

    /**
     * Gets the registry key of the item.
     *
     * @param item the item
     * @return the registry key
     */
    public static ResourceKey<Item> key(Item item) {
        return item.key();
    }

    /**
     * Gets the registry key of the block.
     *
     * @param block the block
     * @return the registry key
     */
    public static ResourceKey<Block> key(Block block) {
        return block.key();
    }

    /**
     * Gets the ID of the item as a string.
     *
     * @param item the item
     * @return the ID, such as {@code minecraft:diamond}
     */
    public static String id(Item item) {
        return item.id().toString();
    }

    /**
     * Gets the ID of the block as a string.
     *
     * @param block the block
     * @return the ID, such as {@code minecraft:stone}
     */
    public static String id(Block block) {
        return block.id().toString();
    }

    /**
     * Gets the item with the ID. Air counts as no item.
     *
     * @param item the ID of the item
     * @return the item, or empty if the ID is not valid, not registered or air
     */
    public static Optional<Item> item(String item) {
        return ItemUtils.item(item, true);
    }

    /**
     * Gets the item with the ID.
     *
     * @param item the ID of the item
     * @param airIsNull {@code true} to return empty for air
     * @return the item, or empty if the ID is not valid or not registered
     */
    public static Optional<Item> item(String item, boolean airIsNull) {
        return ItemUtils.item(ResourceLocation.tryParse(item), airIsNull);
    }

    /**
     * Gets the item with the ID. Air counts as no item.
     *
     * @param location the ID of the item
     * @return the item, or empty if the ID is {@code null}, not registered or air
     */
    public static Optional<Item> item(ResourceLocation location) {
        return ItemUtils.item(location, true);
    }

    /**
     * Gets the item with the ID and caches it.
     *
     * @param location the ID of the item, or {@code null}
     * @param airIsNull {@code true} to return empty for air
     * @return the item, or empty if the ID is {@code null} or not registered
     */
    public static Optional<Item> item(ResourceLocation location, boolean airIsNull) {
        var item = location == null ? null : ItemUtils.itemRegistry().getOptional(location).orElse(null);
        if (item == null || (airIsNull && item == Items.AIR)) {
            return Optional.empty();
        }
        return ITEMS.computeIfAbsent(location, k -> Optional.of(item));
    }


    /**
     * Gets the block with the ID. Air counts as no block.
     *
     * @param block the ID of the block
     * @return the block, or empty if the ID is not valid, not registered or air
     */
    public static Optional<Block> getBlock(String block) {
        return ItemUtils.getBlock(block, true);
    }

    /**
     * Gets the block with the ID.
     *
     * @param block the ID of the block
     * @param airIsNull {@code true} to return empty for air
     * @return the block, or empty if the ID is not valid or not registered
     */
    public static Optional<Block> getBlock(String block, boolean airIsNull) {
        return ItemUtils.getBlock(ResourceLocation.tryParse(block), airIsNull);
    }

    /**
     * Gets the block with the ID. Air counts as no block.
     *
     * @param location the ID of the block
     * @return the block, or empty if the ID is {@code null}, not registered or air
     */
    public static Optional<Block> getBlock(ResourceLocation location) {
        return ItemUtils.getBlock(location, true);
    }

    /**
     * Gets the block with the ID and caches it.
     *
     * @param location the ID of the block, or {@code null}
     * @param airIsNull {@code true} to return empty for air
     * @return the block, or empty if the ID is {@code null} or not registered
     */
    public static Optional<Block> getBlock(ResourceLocation location, boolean airIsNull) {
        var block = location == null ? null : Registries.BLOCK.registryFromRegistryKey().getOptional(location).orElse(null);
        if (block == null || (airIsNull && block == Blocks.AIR)) {
            return Optional.empty();
        }
        return BLOCKS.computeIfAbsent(location, k -> Optional.of(block));
    }

    /**
     * Sets the item name component and removes the custom name. The item name is not shown in italics.
     *
     * @param name converted with {@link TextUtils#asComponent(Object)}
     */
    public static <T> void setDisplayName(ItemStack stack, T name) {
        stack.set(DataComponents.ITEM_NAME, TextUtils.asComponent(name));
        stack.remove(DataComponents.CUSTOM_NAME);
    }

    /**
     * Sets the name of the stack and parses its placeholders. See {@link #setDisplayName(ItemStack, Object)}.
     *
     * @param <T> the type of the name
     * @param stack the stack to change
     * @param name the name
     * @param contexts the contexts for the placeholders, such as a player
     */
    public static <T> void setDisplayNameWithPlaceholders(ItemStack stack, T name, Object... contexts) {
        ItemUtils.setDisplayName(stack, TextUtils.asComponentWithPlaceholders(name, contexts));
    }

    /**
     * Sets the name of the stack and parses its placeholders. See {@link #setDisplayName(ItemStack, Object)}.
     *
     * @param <T> the type of the name
     * @param stack the stack to change
     * @param name the name
     * @param contexts the contexts for the placeholders, such as a player
     */
    public static <T> void setDisplayNameWithPlaceholders(ItemStack stack, T name, Collection<Object> contexts) {
        ItemUtils.setDisplayName(stack, TextUtils.asComponentWithPlaceholders(name, contexts));
    }

    /**
     * Replaces the lore of the stack.
     *
     * @param <T> the type of the lines
     * @param stack the stack to change
     * @param lore the lines, converted with {@link TextUtils#asComponent(Object)}
     */
    @SafeVarargs
    public static <T> void setDisplayLore(ItemStack stack, T... lore) {
        var list = new ArrayList<Component>();
        for (var line : lore) {
            list.add(TextUtils.asComponent(line));
        }
        stack.set(DataComponents.LORE, new ItemLore(list));
    }

    /**
     * Replaces the lore of the stack.
     *
     * @param <T> the type of the lines
     * @param stack the stack to change
     * @param lore the lines, converted with {@link TextUtils#asComponent(Object)}
     */
    public static <T> void setDisplayLore(ItemStack stack, Collection<T> lore) {
        stack.set(DataComponents.LORE, new ItemLore(CollectionUtils.map(lore, new ArrayList<>(), TextUtils::asComponent)));
    }

    /**
     * Adds lines to the end of the lore of the stack.
     *
     * @param <T> the type of the lines
     * @param stack the stack to change
     * @param lore the lines, converted with {@link TextUtils#asComponent(Object)}
     */
    @SafeVarargs
    public static <T> void addDisplayLore(ItemStack stack, T... lore) {
        loreScope(stack, itemLore -> {
            var lines = new ArrayList<>(itemLore.lines());
            for (var line : lore) {
                lines.add(TextUtils.asComponent(line));
            }
            return new ItemLore(lines);
        });
    }

    /**
     * Adds lines to the end of the lore of the stack.
     *
     * @param <T> the type of the lines
     * @param stack the stack to change
     * @param lore the lines, converted with {@link TextUtils#asComponent(Object)}
     */
    public static <T> void addDisplayLore(ItemStack stack, Collection<T> lore) {
        loreScope(stack, itemLore -> {
            var lines = new ArrayList<>(itemLore.lines());
            for (var line : lore) {
                lines.add(TextUtils.asComponent(line));
            }
            return new ItemLore(lines);
        });
    }

    /**
     * Replaces the lore of the stack and parses the placeholders of each line.
     *
     * @param <T> the type of the lines
     * @param stack the stack to change
     * @param contexts the contexts for the placeholders, such as a player
     * @param lore the lines
     */
    @SafeVarargs
    public static <T> void setDisplayLoreWithPlaceholders(ItemStack stack, Collection<Object> contexts, T... lore) {
        var list = new ArrayList<Component>();
        for (var line : lore) {
            list.add(TextUtils.asComponentWithPlaceholders(line, contexts));
        }
        stack.set(DataComponents.LORE, new ItemLore(list));
    }

    /**
     * Replaces the lore of the stack and parses the placeholders of each line.
     *
     * @param <T> the type of the lines
     * @param stack the stack to change
     * @param contexts the contexts for the placeholders, such as a player
     * @param lore the lines
     */
    public static <T> void setDisplayLoreWithPlaceholders(ItemStack stack, Collection<Object> contexts, Collection<T> lore) {
        stack.set(DataComponents.LORE, new ItemLore(CollectionUtils.map(lore, new ArrayList<>(), it -> TextUtils.asComponentWithPlaceholders(it, contexts))));
    }

    /**
     * Adds lines to the end of the lore of the stack and parses the placeholders of each line.
     *
     * @param <T> the type of the lines
     * @param stack the stack to change
     * @param contexts the contexts for the placeholders, such as a player
     * @param lore the lines
     */
    @SafeVarargs
    public static <T> void addDisplayLoreWithPlaceholders(ItemStack stack, Collection<Object> contexts, T... lore) {
        loreScope(stack, itemLore -> {
            var lines = new ArrayList<>(itemLore.lines());
            for (var line : lore) {
                lines.add(TextUtils.asComponentWithPlaceholders(line, contexts));
            }
            return new ItemLore(lines);
        });
    }

    /**
     * Adds lines to the end of the lore of the stack and parses the placeholders of each line.
     *
     * @param <T> the type of the lines
     * @param stack the stack to change
     * @param contexts the contexts for the placeholders, such as a player
     * @param lore the lines
     */
    public static <T> void addDisplayLoreWithPlaceholders(ItemStack stack, Collection<Object> contexts, Collection<T> lore) {
        loreScope(stack, itemLore -> {
            var lines = new ArrayList<>(itemLore.lines());
            for (var line : lore) {
                lines.add(TextUtils.asComponentWithPlaceholders(line, contexts));
            }
            return new ItemLore(lines);
        });
    }

    /**
     * Adds lines to the start of the lore of the stack.
     *
     * @param <T> the type of the lines
     * @param stack the stack to change
     * @param lore the lines, converted with {@link TextUtils#asComponent(Object)}
     */
    @SafeVarargs
    public static <T> void prependDisplayLore(ItemStack stack, T... lore) {
        loreScope(stack, itemLore -> {
            var oldLines = itemLore.lines();
            var lines = new ArrayList<Component>();
            for (var line : lore) {
                lines.add(TextUtils.asComponent(line));
            }
            lines.addAll(oldLines);
            return new ItemLore(lines);
        });
    }

    /**
     * Adds lines to the start of the lore of the stack.
     *
     * @param <T> the type of the lines
     * @param stack the stack to change
     * @param lore the lines, converted with {@link TextUtils#asComponent(Object)}
     */
    public static <T> void prependDisplayLore(ItemStack stack, Collection<T> lore) {
        loreScope(stack, itemLore -> {
            var oldLines = itemLore.lines();
            var lines = new ArrayList<Component>();
            for (var line : lore) {
                lines.add(TextUtils.asComponent(line));
            }
            lines.addAll(oldLines);
            return new ItemLore(lines);
        });
    }

    /**
     * Adds lines to the start of the lore of the stack and parses the placeholders of each line.
     *
     * @param <T> the type of the lines
     * @param stack the stack to change
     * @param contexts the contexts for the placeholders, such as a player
     * @param lore the lines
     */
    @SafeVarargs
    public static <T> void prependDisplayLoreWithPlaceholders(ItemStack stack, Collection<Object> contexts, T... lore) {
        loreScope(stack, itemLore -> {
            var oldLines = itemLore.lines();
            var lines = new ArrayList<Component>();
            for (var line : lore) {
                lines.add(TextUtils.asComponentWithPlaceholders(line, contexts));
            }
            lines.addAll(oldLines);
            return new ItemLore(lines);
        });
    }

    /**
     * Adds lines to the start of the lore of the stack and parses the placeholders of each line.
     *
     * @param <T> the type of the lines
     * @param stack the stack to change
     * @param contexts the contexts for the placeholders, such as a player
     * @param lore the lines
     */
    public static <T> void prependDisplayLoreWithPlaceholders(ItemStack stack, Collection<Object> contexts, Collection<T> lore) {
        loreScope(stack, itemLore -> {
            var oldLines = itemLore.lines();
            var lines = new ArrayList<Component>();
            for (var line : lore) {
                lines.add(TextUtils.asComponentWithPlaceholders(line, contexts));
            }
            lines.addAll(oldLines);
            return new ItemLore(lines);
        });
    }

    /**
     * Replaces the lore of the stack with the result of {@code withLore}.
     *
     * @param stack the stack to change
     * @param withLore gets the current lore, or empty lore if the stack has none, and returns the new lore
     */
    private static void loreScope(ItemStack stack, UnaryOperator<ItemLore> withLore) {
        stack.componentScope(DataComponents.LORE, () -> new ItemLore(List.of()), withLore);
    }

    /**
     * Sets the {@code tooltip} string in the custom data of the stack. Pixelmon shows it as the tooltip.
     *
     * @param stack the stack to change
     * @param tooltip the tooltip text
     */
    public static void setPixelmonTooltip(ItemStack stack, String tooltip) {
        stack.componentScope(DataComponents.CUSTOM_DATA, () -> CustomData.of(new CompoundTag()), data -> data.update(tag -> tag.putString(TAG_TOOLTIP, tooltip)));
    }

    /**
     * Hides the attribute modifiers and the additional tooltip lines. The name, lore and enchantments stay visible.
     */
    public static void hideTooltips(ItemStack stack) {
        stack.componentScope(DataComponents.ATTRIBUTE_MODIFIERS, data -> data.withTooltip(false));
        stack.set(DataComponents.HIDE_ADDITIONAL_TOOLTIP, Unit.INSTANCE);
    }

    /**
     * Hides the whole tooltip of the stack, including the name.
     *
     * @param stack the stack to change
     */
    public static void hideAllTooltips(ItemStack stack) {
        stack.set(DataComponents.HIDE_TOOLTIP, Unit.INSTANCE);
    }

    /**
     * Removes all enchantments of the stack. Stored enchantments of enchanted books do not change.
     *
     * @param stack the stack to change
     */
    public static void clearEnchantments(ItemStack stack) {
        stack.set(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
    }

    /**
     * Removes all enchantments of the stack, then adds the enchantments.
     *
     * @param stack the stack to change
     * @param enchantments the new enchantments
     */
    public static void setEnchantments(ItemStack stack, ItemEnchantments... enchantments) {
        ItemUtils.clearEnchantments(stack);
        ItemUtils.addEnchantments(stack, enchantments);
    }

    /**
     * Adds each enchantment to the stack. See {@link #addEnchantment(ItemStack, Holder, int)}.
     *
     * @param stack the stack to change
     * @param enchantments the enchantments to add
     */
    public static void addEnchantments(ItemStack stack, ItemEnchantments... enchantments) {
        for (var enchantment : enchantments) {
            for (var entry : enchantment.entrySet()) {
                ItemUtils.addEnchantment(stack, entry.getKey(), entry.getIntValue());
            }
        }
    }

    /**
     * On enchanted books, this changes the stored enchantments.
     *
     * @param tier replaces the current level. A level of 0 or less removes the enchantment.
     */
    public static void addEnchantment(ItemStack stack, Holder<Enchantment> enchantment, int tier) {
        EnchantmentHelper.updateEnchantments(stack, enchantments -> enchantments.set(enchantment, tier));
    }

    /**
     * Adds Infinity I so that the item glows. The tooltip also shows the enchantment.
     */
    public static void addDummyEnchantment(ItemStack stack) {
        ItemUtils.addEnchantments(stack, ItemUtils.getDummyEnchantment());
    }

    /**
     * @return the enchantments with only Infinity I
     */
    public static ItemEnchantments getDummyEnchantment() {
        return ItemUtils.DUMMY_ENCHANTMENT.get();
    }

    /**
     * Gets the enchantments of the stack. Stored enchantments of enchanted books are not included.
     *
     * @param stack the stack
     * @return the enchantments, or {@link ItemEnchantments#EMPTY} if the stack has none
     */
    public static ItemEnchantments getEnchantments(ItemStack stack) {
        return stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
    }

    /**
     * Checks if the stack has the enchantment at level 1 or more.
     *
     * @param stack the stack
     * @param enchantment the enchantment
     * @return {@code true} if the stack has the enchantment
     */
    public static boolean hasEnchantment(ItemStack stack, Holder<Enchantment> enchantment) {
        return stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY).getLevel(enchantment) > 0;
    }

    /**
     * Removes the enchantments from the stack. On enchanted books, this changes the stored enchantments.
     *
     * @param stack the stack to change
     * @param enchantments the enchantments to remove
     */
    @SafeVarargs
    public static void removeEnchantments(ItemStack stack, Holder<Enchantment>... enchantments) {
        EnchantmentHelper.updateEnchantments(stack, itemEnchantments -> {
            for (var enchantment : enchantments) {
                itemEnchantments.removeIf(enchantment::is);
            }
        });
    }

    /**
     * Makes the stack glow with the glint override. It adds no enchantment.
     *
     * @param stack the stack to change
     */
    public static void applyGlow(ItemStack stack) {
        stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
    }

    /**
     * Removes the glint override. The item glows again if it has enchantments.
     */
    public static void removeGlow(ItemStack stack) {
        stack.remove(DataComponents.ENCHANTMENT_GLINT_OVERRIDE);
    }

    /**
     * Hides the glint, even if the item has enchantments.
     */
    public static void hideGlow(ItemStack stack) {
        stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, false);
    }

    /**
     * Sets the custom model data of the stack.
     *
     * @param stack the stack to change
     * @param customModelData the custom model data
     */
    public static void setCustomModelData(ItemStack stack, int customModelData) {
        stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(customModelData));
    }

    /**
     * Removes the custom model data of the stack.
     *
     * @param stack the stack to change
     */
    public static void removeCustomModelData(ItemStack stack) {
        stack.remove(DataComponents.CUSTOM_MODEL_DATA);
    }

    /**
     * @return -1 if the stack has no custom model data
     */
    public static int getCustomModelData(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_MODEL_DATA, NO_CUSTOM_MODEL_DATA).value();
    }

    /**
     * Checks if the custom data of the stack has the key.
     *
     * @param stack the stack
     * @param tag the key in the custom data
     * @return {@code true} if the key is present
     */
    public static boolean hasTag(ItemStack stack, String tag) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).contains(tag);
    }

    /**
     * Adds empty custom data if the stack has none. To change the data, use {@link #tag(ItemStack, Consumer)}.
     */
    public static CustomData tag(ItemStack stack) {
        return stack.computeComponentIfAbsent(DataComponents.CUSTOM_DATA, () -> CustomData.of(new CompoundTag()));
    }

    /**
     * Changes a copy of the custom data tag of the stack and sets it on the stack. If the stack has no custom data,
     * it starts from an empty tag.
     *
     * @param stack the stack to change
     * @param tag changes the custom data tag
     */
    public static void tag(ItemStack stack, Consumer<CompoundTag> tag) {
        stack.componentScope(DataComponents.CUSTOM_DATA, () -> CustomData.of(new CompoundTag()), data -> data.update(tag));
    }

    /**
     * Gets the data components of the stack, including the default components of the item.
     *
     * @param stack the stack
     * @return the components
     */
    public static DataComponentMap components(ItemStack stack) {
        return stack.getComponents();
    }

    /**
     * Encodes the data components of the stack. See {@link DataComponentEncoder#encode(DataComponentMap)}.
     *
     * @param <T> the type of the store
     * @param stack the stack
     * @param encoder the encoder, such as {@link DataComponentEncoder#TAG}
     * @return the store with the encoded components
     */
    public static <T> T encodeComponents(ItemStack stack, DataComponentEncoder<T, ?> encoder) {
        return encoder.encode(ItemUtils.components(stack));
    }

    /**
     * Encodes the data components. See {@link DataComponentEncoder#encode(DataComponentMap)}.
     *
     * @param <T> the type of the store
     * @param components the components
     * @param encoder the encoder, such as {@link DataComponentEncoder#TAG}
     * @return the store with the encoded components
     */
    public static <T> T encodeComponents(DataComponentMap components, DataComponentEncoder<T, ?> encoder) {
        return encoder.encode(components);
    }

    /**
     * Decodes data components from the store. See {@link DataComponentEncoder#decode(Object)}.
     *
     * @param <T> the type of the store
     * @param store the store with the encoded components
     * @param encoder the encoder, such as {@link DataComponentEncoder#TAG}
     * @return the decoded components
     */
    public static <T> DataComponentMap decodeComponents(T store, DataComponentEncoder<T, ?> encoder) {
        return encoder.decode(store);
    }

    /**
     * Decodes data components from the store and applies them to the stack.
     *
     * @param <T> the type of the store
     * @param stack the stack to change
     * @param store the store with the encoded components
     * @param encoder the encoder, such as {@link DataComponentEncoder#TAG}
     */
    public static <T> void decodeComponents(ItemStack stack, T store, DataComponentEncoder<T, ?> encoder) {
        stack.applyComponents(ItemUtils.decodeComponents(store, encoder));
    }

    /**
     * Gets the component of the stack. If the stack does not have it, it sets the value from {@code ifAbsent}.
     *
     * @param <T> the type of the component value
     * @param stack the stack
     * @param component the component type
     * @param ifAbsent supplies the value if the stack does not have the component
     * @return the current or the new value
     */
    public static <T> T computeIfAbsent(ItemStack stack, DataComponentType<T> component, Supplier<T> ifAbsent) {
        var data = stack.get(component);
        if (data == null) {
            data = ifAbsent.get();
            stack.set(component, data);
        }
        return data;
    }

    /**
     * Adds a new stack to the inventory of the player. See {@link #giveOrElse(Player, ItemStack, Consumer)}.
     *
     * @param player the player that gets the stack
     * @param stack the stack to give
     * @param orElse gets the part of the stack that does not fit
     * @return {@code true} if the inventory takes the full stack
     */
    public static boolean giveOrElse(Player player, ConfigurableItemStack stack, Consumer<ItemStack> orElse) {
        return ItemUtils.giveOrElse(player, stack.newStack(), orElse);
    }

    /**
     * Adds the stack to the inventory of the player. This changes the count of {@code stack}.
     *
     * @param orElse gets the part of the stack that does not fit
     * @return {@code true} if the inventory takes the full stack
     */
    public static boolean giveOrElse(Player player, ItemStack stack, Consumer<ItemStack> orElse) {
        if (!player.addItem(stack)) {
            orElse.accept(stack);
            return false;
        }
        return true;
    }

    /**
     * Drops a new stack at the position, with no owner.
     * See {@link #drop(ItemStack, LevelPosition, Player, Consumer)}.
     *
     * @param stack the stack to drop
     * @param position the position to drop the stack at
     * @param modifier runs before the entity spawns
     * @return the item entity, or {@code null} if it could not be created
     */
    public static @Nullable ItemEntity drop(ConfigurableItemStack stack, LevelPosition position, Consumer<ItemEntity> modifier) {
        return ItemUtils.drop(stack, position, null, modifier);
    }

    /**
     * Drops the stack at the position, with no owner.
     * See {@link #drop(ItemStack, LevelPosition, Player, Consumer)}.
     *
     * @param stack the stack to drop
     * @param position the position to drop the stack at
     * @param modifier runs before the entity spawns
     * @return the item entity, or {@code null} if it could not be created
     */
    public static @Nullable ItemEntity drop(ItemStack stack, LevelPosition position, Consumer<ItemEntity> modifier) {
        return ItemUtils.drop(stack, position, null, modifier);
    }

    /**
     * Drops a new stack at the position. See {@link #drop(ItemStack, LevelPosition, Player, Consumer)}.
     *
     * @param stack the stack to drop
     * @param position the position to drop the stack at
     * @param owner the thrower of the item, or {@code null} for no owner
     * @param modifier runs before the entity spawns
     * @return the item entity, or {@code null} if it could not be created
     */
    public static @Nullable ItemEntity drop(ConfigurableItemStack stack, LevelPosition position, @Nullable Player owner, Consumer<ItemEntity> modifier) {
        return ItemUtils.drop(stack.newStack(), position, owner, modifier);
    }

    /**
     * Spawns the item entity on the server thread.
     * If you call this from another thread, the entity spawns after this method returns.
     *
     * @param owner becomes the thrower. The item also stays for 10 minutes instead of 5.
     * @param modifier runs before the entity spawns, on the thread of the caller
     */
    public static @Nullable ItemEntity drop(ItemStack stack, LevelPosition position, @Nullable Player owner, Consumer<ItemEntity> modifier) {
        return EntityUtils.createEntity(EntityType.ITEM, position, false, true, entity -> {
            entity.setItem(stack);
            if (owner != null) {
                entity.setExtendedLifetime();
                entity.setThrower(owner);
            }
            modifier.accept(entity);
        });
    }

    /**
     * Adds a new stack to the inventory of the player and drops the part that does not fit.
     * See {@link #giveOrDrop(Player, ItemStack, Consumer, Consumer)}.
     *
     * @param player the player that gets the stack
     * @param stack the stack to give
     * @return {@code true} if the inventory takes the full stack
     */
    public static boolean giveOrDrop(Player player, ConfigurableItemStack stack) {
        return ItemUtils.giveOrDrop(player, stack, item -> {}, entity -> {});
    }

    /**
     * Adds the stack to the inventory of the player and drops the part that does not fit.
     * See {@link #giveOrDrop(Player, ItemStack, Consumer, Consumer)}.
     *
     * @param player the player that gets the stack
     * @param stack the stack to give. Its count changes.
     * @return {@code true} if the inventory takes the full stack
     */
    public static boolean giveOrDrop(Player player, ItemStack stack) {
        return ItemUtils.giveOrDrop(player, stack, item -> {}, entity -> {});
    }

    /**
     * Adds a new stack to the inventory of the player and drops the part that does not fit.
     * See {@link #giveOrDrop(Player, ItemStack, Consumer, Consumer)}.
     *
     * @param player the player that gets the stack
     * @param stack the stack to give
     * @param onGive runs if the inventory takes the full stack
     * @param onDrop runs if an item entity is dropped
     * @return {@code true} if the inventory takes the full stack
     */
    public static boolean giveOrDrop(Player player, ConfigurableItemStack stack, Consumer<ItemStack> onGive, Consumer<ItemEntity> onDrop) {
        return ItemUtils.giveOrDrop(player, stack.newStack(), onGive, onDrop);
    }

    /**
     * Drops the part that does not fit at the player, with the player as owner.
     * See {@link #drop(ItemStack, LevelPosition, Player, Consumer)}.
     *
     * @param onGive runs if the inventory takes the full stack
     * @param onDrop runs if an item entity is dropped
     * @return {@code true} if the inventory takes the full stack
     */
    public static boolean giveOrDrop(Player player, ItemStack stack, Consumer<ItemStack> onGive, Consumer<ItemEntity> onDrop) {
        if (ItemUtils.giveOrElse(player, stack, orElse -> {
            var drop = ItemUtils.drop(orElse, LevelPosition.of(player), player, entity -> {});
            if (drop != null) {
                onDrop.accept(drop);
            }
        })) {
            onGive.accept(stack);
            return true;
        }
        return false;
    }

    /**
     * Checks if any of the stacks matches the object. See {@link ConfigurableItemStack#matches(Object)}.
     *
     * @param itemLike the object to match
     * @param stacks the stacks
     * @return {@code true} if at least one stack matches
     */
    public static boolean matches(Object itemLike, Collection<ConfigurableItemStack> stacks) {
        for (var stack : stacks) {
            if (stack.matches(itemLike)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if any of the stacks matches the object. See {@link ConfigurableItemStack#matches(Object)}.
     *
     * @param itemLike the object to match
     * @param stacks the stacks
     * @return {@code true} if at least one stack matches
     */
    public static boolean matches(Object itemLike, ConfigurableItemStack... stacks) {
        for (var stack : stacks) {
            if (stack.matches(itemLike)) {
                return true;
            }
        }
        return false;
    }
}
