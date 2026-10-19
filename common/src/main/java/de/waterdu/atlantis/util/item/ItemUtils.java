package de.waterdu.atlantis.util.item;

import de.waterdu.atlantis.util.entity.EntityUtils;
import de.waterdu.atlantis.util.java.ExceptionUtils;
import de.waterdu.atlantis.util.java.collection.CollectionUtils;
import de.waterdu.atlantis.util.java.interfaces.Lazy;
import de.waterdu.atlantis.util.level.LevelPosition;
import de.waterdu.atlantis.util.text.TextUtils;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
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

    public static Registry<Item> itemRegistry() {
        return Registries.ITEM.registryFromRegistryKey();
    }

    public static Registry<Block> blockRegistry() {
        return Registries.BLOCK.registryFromRegistryKey();
    }

    public static ResourceKey<Item> key(Item item) {
        return item.key();
    }

    public static ResourceKey<Block> key(Block block) {
        return block.key();
    }

    public static String id(Item item) {
        return item.id().toString();
    }

    public static String id(Block block) {
        return block.id().toString();
    }

    public static Optional<Item> item(String item) {
        return ItemUtils.item(item, true);
    }

    public static Optional<Item> item(String item, boolean airIsNull) {
        return ItemUtils.item(ResourceLocation.tryParse(item), airIsNull);
    }

    public static Optional<Item> item(ResourceLocation location) {
        return ItemUtils.item(location, true);
    }

    public static Optional<Item> item(ResourceLocation location, boolean airIsNull) {
        var item = location == null ? null : ItemUtils.itemRegistry().getOptional(location).orElse(null);
        if (item == null || (airIsNull && item == Items.AIR)) {
            return Optional.empty();
        }
        return ITEMS.computeIfAbsent(location, k -> Optional.of(item));
    }


    public static Optional<Block> getBlock(String block) {
        return ItemUtils.getBlock(block, true);
    }

    public static Optional<Block> getBlock(String block, boolean airIsNull) {
        return ItemUtils.getBlock(ResourceLocation.tryParse(block), airIsNull);
    }

    public static Optional<Block> getBlock(ResourceLocation location) {
        return ItemUtils.getBlock(location, true);
    }

    public static Optional<Block> getBlock(ResourceLocation location, boolean airIsNull) {
        var block = location == null ? null : Registries.BLOCK.registryFromRegistryKey().getOptional(location).orElse(null);
        if (block == null || (airIsNull && block == Blocks.AIR)) {
            return Optional.empty();
        }
        return BLOCKS.computeIfAbsent(location, k -> Optional.of(block));
    }

    public static <T> void setDisplayName(ItemStack stack, T name) {
        stack.set(DataComponents.ITEM_NAME, TextUtils.asComponent(name));
        stack.remove(DataComponents.CUSTOM_NAME);
    }

    public static <T> void setDisplayNameWithPlaceholders(ItemStack stack, T name, Object... contexts) {
        ItemUtils.setDisplayName(stack, TextUtils.asComponentWithPlaceholders(name, contexts));
    }

    public static <T> void setDisplayNameWithPlaceholders(ItemStack stack, T name, Collection<Object> contexts) {
        ItemUtils.setDisplayName(stack, TextUtils.asComponentWithPlaceholders(name, contexts));
    }

    @SafeVarargs
    public static <T> void setDisplayLore(ItemStack stack, T... lore) {
        var list = new ArrayList<Component>();
        for (var line : lore) {
            list.add(TextUtils.asComponent(line));
        }
        stack.set(DataComponents.LORE, new ItemLore(list));
    }

    public static <T> void setDisplayLore(ItemStack stack, Collection<T> lore) {
        stack.set(DataComponents.LORE, new ItemLore(CollectionUtils.map(lore, new ArrayList<>(), TextUtils::asComponent)));
    }

    @SafeVarargs
    public static <T> void addDisplayLore(ItemStack stack, T... lore) {
        stack.componentScope(DataComponents.LORE, () -> new ItemLore(List.of()), itemLore -> {
            var lines = new ArrayList<>(itemLore.lines());
            for (var line : lore) {
                lines.add(TextUtils.asComponent(line));
            }
            return new ItemLore(lines);
        });
    }

    public static <T> void addDisplayLore(ItemStack stack, Collection<T> lore) {
        stack.componentScope(DataComponents.LORE, () -> new ItemLore(List.of()), itemLore -> {
            var lines = new ArrayList<>(itemLore.lines());
            for (var line : lore) {
                lines.add(TextUtils.asComponent(line));
            }
            return new ItemLore(lines);
        });
    }

    @SafeVarargs
    public static <T> void setDisplayLoreWithPlaceholders(ItemStack stack, Collection<Object> contexts, T... lore) {
        var list = new ArrayList<Component>();
        for (var line : lore) {
            list.add(TextUtils.asComponentWithPlaceholders(line, contexts));
        }
        stack.set(DataComponents.LORE, new ItemLore(list));
    }

    public static <T> void setDisplayLoreWithPlaceholders(ItemStack stack, Collection<Object> contexts, Collection<T> lore) {
        stack.set(DataComponents.LORE, new ItemLore(CollectionUtils.map(lore, new ArrayList<>(), it -> TextUtils.asComponentWithPlaceholders(it, contexts))));
    }

    @SafeVarargs
    public static <T> void addDisplayLoreWithPlaceholders(ItemStack stack, Collection<Object> contexts, T... lore) {
        stack.componentScope(DataComponents.LORE, () -> new ItemLore(List.of()), itemLore -> {
            var lines = new ArrayList<>(itemLore.lines());
            for (var line : lore) {
                lines.add(TextUtils.asComponentWithPlaceholders(line, contexts));
            }
            return new ItemLore(lines);
        });
    }

    public static <T> void addDisplayLoreWithPlaceholders(ItemStack stack, Collection<Object> contexts, Collection<T> lore) {
        stack.componentScope(DataComponents.LORE, () -> new ItemLore(List.of()), itemLore -> {
            var lines = new ArrayList<>(itemLore.lines());
            for (var line : lore) {
                lines.add(TextUtils.asComponentWithPlaceholders(line, contexts));
            }
            return new ItemLore(lines);
        });
    }

    @SafeVarargs
    public static <T> void prependDisplayLore(ItemStack stack, T... lore) {
        stack.componentScope(DataComponents.LORE, () -> new ItemLore(List.of()), itemLore -> {
            var oldLines = itemLore.lines();
            var lines = new ArrayList<Component>();
            for (var line : lore) {
                lines.add(TextUtils.asComponent(line));
            }
            lines.addAll(oldLines);
            return new ItemLore(lines);
        });
    }

    public static <T> void prependDisplayLore(ItemStack stack, Collection<T> lore) {
        stack.componentScope(DataComponents.LORE, () -> new ItemLore(List.of()), itemLore -> {
            var oldLines = itemLore.lines();
            var lines = new ArrayList<Component>();
            for (var line : lore) {
                lines.add(TextUtils.asComponent(line));
            }
            lines.addAll(oldLines);
            return new ItemLore(lines);
        });
    }

    @SafeVarargs
    public static <T> void prependDisplayLoreWithPlaceholders(ItemStack stack, Collection<Object> contexts, T... lore) {
        stack.componentScope(DataComponents.LORE, () -> new ItemLore(List.of()), itemLore -> {
            var oldLines = itemLore.lines();
            var lines = new ArrayList<Component>();
            for (var line : lore) {
                lines.add(TextUtils.asComponentWithPlaceholders(line, contexts));
            }
            lines.addAll(oldLines);
            return new ItemLore(lines);
        });
    }

    public static <T> void prependDisplayLoreWithPlaceholders(ItemStack stack, Collection<Object> contexts, Collection<T> lore) {
        stack.componentScope(DataComponents.LORE, () -> new ItemLore(List.of()), itemLore -> {
            var oldLines = itemLore.lines();
            var lines = new ArrayList<Component>();
            for (var line : lore) {
                lines.add(TextUtils.asComponentWithPlaceholders(line, contexts));
            }
            lines.addAll(oldLines);
            return new ItemLore(lines);
        });
    }

    public static void setPixelmonTooltip(ItemStack stack, String tooltip) {
        stack.componentScope(DataComponents.CUSTOM_DATA, () -> CustomData.of(new CompoundTag()), data -> data.update(tag -> tag.putString(TAG_TOOLTIP, tooltip)));
    }

    public static void hideTooltips(ItemStack stack) {
        stack.componentScope(DataComponents.ATTRIBUTE_MODIFIERS, data -> data.withTooltip(false));
        stack.set(DataComponents.HIDE_ADDITIONAL_TOOLTIP, Unit.INSTANCE);
    }

    public static void hideAllTooltips(ItemStack stack) {
        stack.set(DataComponents.HIDE_TOOLTIP, Unit.INSTANCE);
    }

    public static void clearEnchantments(ItemStack stack) {
        stack.set(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
    }

    public static void setEnchantments(ItemStack stack, ItemEnchantments... enchantments) {
        ItemUtils.clearEnchantments(stack);
        ItemUtils.addEnchantments(stack, enchantments);
    }

    public static void addEnchantments(ItemStack stack, ItemEnchantments... enchantments) {
        for (var enchantment : enchantments) {
            for (var entry : enchantment.entrySet()) {
                ItemUtils.addEnchantment(stack, entry.getKey(), entry.getIntValue());
            }
        }
    }

    public static void addEnchantment(ItemStack stack, Holder<Enchantment> enchantment, int tier) {
        EnchantmentHelper.updateEnchantments(stack, enchantments -> enchantments.set(enchantment, tier));
    }

    public static void addDummyEnchantment(ItemStack stack) {
        ItemUtils.addEnchantments(stack, ItemUtils.getDummyEnchantment());
    }

    public static ItemEnchantments getDummyEnchantment() {
        return ItemUtils.DUMMY_ENCHANTMENT.get();
    }

    public static ItemEnchantments getEnchantments(ItemStack stack) {
        return stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
    }

    public static boolean hasEnchantment(ItemStack stack, Holder<Enchantment> enchantment) {
        return stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY).getLevel(enchantment) > 0;
    }

    @SafeVarargs
    public static void removeEnchantments(ItemStack stack, Holder<Enchantment>... enchantments) {
        EnchantmentHelper.updateEnchantments(stack, itemEnchantments -> {
            for (var enchantment : enchantments) {
                itemEnchantments.removeIf(enchantment::is);
            }
        });
    }

    public static void applyGlow(ItemStack stack) {
        stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
    }

    public static void removeGlow(ItemStack stack) {
        stack.remove(DataComponents.ENCHANTMENT_GLINT_OVERRIDE);
    }

    public static void hideGlow(ItemStack stack) {
        stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, false);
    }

    public static void setCustomModelData(ItemStack stack, int customModelData) {
        stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(customModelData));
    }

    public static void removeCustomModelData(ItemStack stack) {
        stack.remove(DataComponents.CUSTOM_MODEL_DATA);
    }

    public static int getCustomModelData(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_MODEL_DATA, NO_CUSTOM_MODEL_DATA).value();
    }

    public static boolean hasTag(ItemStack stack, String tag) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).contains(tag);
    }

    public static CustomData tag(ItemStack stack) {
        return stack.computeComponentIfAbsent(DataComponents.CUSTOM_DATA, () -> CustomData.of(new CompoundTag()));
    }

    public static void tag(ItemStack stack, Consumer<CompoundTag> tag) {
        stack.componentScope(DataComponents.CUSTOM_DATA, () -> CustomData.of(new CompoundTag()), data -> data.update(tag));
    }

    public static DataComponentMap components(ItemStack stack) {
        return stack.getComponents();
    }

    public static <T> T encodeComponents(ItemStack stack, DataComponentEncoder<T, ?> encoder) {
        return encoder.encode(ItemUtils.components(stack));
    }

    public static <T> T encodeComponents(DataComponentMap components, DataComponentEncoder<T, ?> encoder) {
        return encoder.encode(components);
    }

    public static <T> DataComponentMap decodeComponents(T store, DataComponentEncoder<T, ?> encoder) {
        return encoder.decode(store);
    }
    
    public static <T> void decodeComponents(ItemStack stack, T store, DataComponentEncoder<T, ?> encoder) {
        stack.applyComponents(ItemUtils.decodeComponents(store, encoder));
    }

    public static <T> T computeIfAbsent(ItemStack stack, DataComponentType<T> component, Supplier<T> ifAbsent) {
        var data = stack.get(component);
        if (data == null) {
            data = ifAbsent.get();
            stack.set(component, data);
        }
        return data;
    }

    public static boolean giveOrElse(Player player, ConfigurableItemStack stack, Consumer<ItemStack> orElse) {
        return ItemUtils.giveOrElse(player, stack.newStack(), orElse);
    }

    public static boolean giveOrElse(Player player, ItemStack stack, Consumer<ItemStack> orElse) {
        if (!player.addItem(stack)) {
            orElse.accept(stack);
            return false;
        }
        return true;
    }

    public static Optional<ItemEntity> drop(ConfigurableItemStack stack, LevelPosition position, Consumer<ItemEntity> modifier) {
        return ItemUtils.drop(stack, position, null, modifier);
    }

    public static Optional<ItemEntity> drop(ItemStack stack, LevelPosition position, Consumer<ItemEntity> modifier) {
        return ItemUtils.drop(stack, position, null, modifier);
    }

    public static Optional<ItemEntity> drop(ConfigurableItemStack stack, LevelPosition position, @Nullable Player owner, Consumer<ItemEntity> modifier) {
        return ItemUtils.drop(stack.newStack(), position, owner, modifier);
    }

    public static Optional<ItemEntity> drop(ItemStack stack, LevelPosition position, @Nullable Player owner, Consumer<ItemEntity> modifier) {
        return EntityUtils.createEntity(EntityType.ITEM, position, false, true, entity -> {
            entity.setItem(stack);
            if (owner != null) {
                entity.setExtendedLifetime();
                entity.setThrower(owner);
            }
            modifier.accept(entity);
        });
    }

    public static boolean giveOrDrop(Player player, ConfigurableItemStack stack) {
        return ItemUtils.giveOrDrop(player, stack, item -> {}, entity -> {});
    }

    public static boolean giveOrDrop(Player player, ItemStack stack) {
        return ItemUtils.giveOrDrop(player, stack, item -> {}, entity -> {});
    }

    public static boolean giveOrDrop(Player player, ConfigurableItemStack stack, Consumer<ItemStack> onGive, Consumer<ItemEntity> onDrop) {
        return ItemUtils.giveOrDrop(player, stack.newStack(), onGive, onDrop);
    }

    public static boolean giveOrDrop(Player player, ItemStack stack, Consumer<ItemStack> onGive, Consumer<ItemEntity> onDrop) {
        if (ItemUtils.giveOrElse(player, stack, orElse -> ItemUtils.drop(orElse, LevelPosition.of(player), player, entity -> {}).ifPresent(onDrop))) {
            onGive.accept(stack);
            return true;
        }
        return false;
    }

    public static boolean matches(Object itemLike, Collection<ConfigurableItemStack> stacks) {
        for (ConfigurableItemStack stack : stacks) {
            if (stack.matches(itemLike)) {
                return true;
            }
        }
        return false;
    }

    public static boolean matches(Object itemLike, ConfigurableItemStack... stacks) {
        for (ConfigurableItemStack stack : stacks) {
            if (stack.matches(itemLike)) {
                return true;
            }
        }
        return false;
    }
}
