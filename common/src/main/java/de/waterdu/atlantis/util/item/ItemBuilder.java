package de.waterdu.atlantis.util.item;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.ItemLike;

import java.util.Collection;
import java.util.Collections;
import java.util.function.Consumer;

public final class ItemBuilder {

    private ItemStack stack;
    private ServerPlayer target;

    private ItemBuilder(ItemStack stack) {
        this.stack = stack;
        this.target = null;
    }

    public static ItemBuilder builder() {
        return ItemBuilder.builder((ItemStack) null);
    }

    public static ItemBuilder builder(String item) {
        return ItemBuilder.builder(ConfigurableItemStack.of(item));
    }

    public static ItemBuilder builder(ItemLike like) {
        return ItemBuilder.builder(new ItemStack(like));
    }

    public static ItemBuilder builder(ConfigurableItemStack stack) {
        return ItemBuilder.builder(stack.newStack());
    }

    public static ItemBuilder builder(ItemStack stack) {
        return new ItemBuilder(stack);
    }

    private ItemStack getStackInternal() {
        if (this.stack == null) {
            this.stack = ItemStack.EMPTY;
        }
        return this.stack;
    }

    private boolean hasPlaceholderTarget() {
        return this.target != null;
    }

    public ItemBuilder placeholders(ServerPlayer target) {
        this.target = target;
        return this;
    }

    public ItemBuilder item(String item) {
        return this.stack(ConfigurableItemStack.of(item));
    }

    public ItemBuilder item(ItemLike like) {
        ItemStack old = this.getStackInternal();
        return this.stack(new ItemStack(like.asItem().builtInRegistryHolder(), old.getCount(), old.getComponentsPatch()), false);
    }

    public ItemBuilder stack(ConfigurableItemStack stack) {
        return this.stack(stack.newStack(), false);
    }

    public ItemBuilder stack(ItemStack stack) {
        return this.stack(stack, true);
    }

    public ItemBuilder stack(ItemStack stack, boolean copy) {
        this.stack = copy ? stack.copy() : stack;
        return this;
    }

    public ItemBuilder count(int count) {
        this.stack.setCount(count);
        return this;
    }

    public ItemBuilder grow(int amount) {
        this.stack.grow(amount);
        return this;
    }

    public ItemBuilder shrink(int amount) {
        this.stack.shrink(amount);
        return this;
    }

    public ItemBuilder damage(int damage) {
        this.stack.setDamageValue(damage);
        return this;
    }

    public ItemBuilder name(Object name) {
        if (this.hasPlaceholderTarget()) {
            ItemUtils.setDisplayNameWithPlaceholders(this.stack, name, this.target);
        } else {
            ItemUtils.setDisplayName(this.stack, name);
        }
        return this;
    }

    public ItemBuilder lore(Object... lore) {
        if (this.hasPlaceholderTarget()) {
            ItemUtils.setDisplayLoreWithPlaceholders(this.stack, Collections.singletonList(this.target), lore);
        } else {
            ItemUtils.setDisplayLore(this.stack, lore);
        }
        return this;
    }

    public ItemBuilder lore(Collection<Object> lore) {
        if (this.hasPlaceholderTarget()) {
            ItemUtils.setDisplayLoreWithPlaceholders(this.stack, Collections.singletonList(this.target), lore);
        } else {
            ItemUtils.setDisplayLore(this.stack, lore);
        }
        return this;
    }

    public ItemBuilder addLore(Object... lore) {
        if (this.hasPlaceholderTarget()) {
            ItemUtils.addDisplayLoreWithPlaceholders(this.stack, Collections.singletonList(this.target), lore);
        } else {
            ItemUtils.addDisplayLore(this.stack, lore);
        }
        return this;
    }

    public ItemBuilder addLore(Collection<Object> lore) {
        if (this.hasPlaceholderTarget()) {
            ItemUtils.addDisplayLoreWithPlaceholders(this.stack, Collections.singletonList(this.target), lore);
        } else {
            ItemUtils.addDisplayLore(this.stack, lore);
        }
        return this;
    }

    public ItemBuilder glow(boolean glow) {
        if (glow) {
            this.glow();
        }
        return this;
    }

    public ItemBuilder glow() {
        ItemUtils.applyGlow(this.stack);
        return this;
    }

    public ItemBuilder hideTooltips() {
        ItemUtils.hideTooltips(this.stack);
        return this;
    }

    public ItemBuilder hideTooltips(boolean hideTooltips) {
        if (hideTooltips) {
            this.hideTooltips();
        }
        return this;
    }

    public ItemBuilder enchantment(Holder<Enchantment> enchantment) {
        return this.enchantment(enchantment, 1);
    }

    public ItemBuilder enchantment(Holder<Enchantment> enchantment, int tier) {
        ItemUtils.addEnchantment(this.stack, enchantment, tier);
        return this;
    }

    public ItemBuilder enchantments(ItemEnchantments enchantments) {
        ItemUtils.setEnchantments(this.stack, enchantments);
        return this;
    }

    public ItemBuilder attribute(Holder<Attribute> attribute, AttributeModifier modifier) {
        return this.attribute(attribute, modifier, null);
    }

    public ItemBuilder attribute(Holder<Attribute> attribute, AttributeModifier modifier, EquipmentSlotGroup slot) {
        this.stack.componentScope(
                DataComponents.ATTRIBUTE_MODIFIERS,
                () -> ItemAttributeModifiers.EMPTY,
                modifiers -> modifiers.withModifierAdded(attribute, modifier, slot)
        );
        return this;
    }

    public ItemBuilder representation(Entity representation) {
        this.stack.setEntityRepresentation(representation);
        return this;
    }

    public ItemBuilder popTime(int popTime) {
        this.stack.setPopTime(popTime);
        return this;
    }

    public ItemBuilder repairCost(int repairCost) {
        this.stack.set(DataComponents.REPAIR_COST, repairCost);
        return this;
    }

    public ItemBuilder tag(Consumer<CompoundTag> tag) {
        ItemUtils.tag(this.stack, tag);
        return this;
    }

    public ItemBuilder tag(CompoundTag tag) {
        this.stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        return this;
    }

    public ItemBuilder customModelData(int customModelData) {
        ItemUtils.setCustomModelData(this.stack, customModelData);
        return this;
    }

    public <T> ItemBuilder component(DataComponentType<T> type, T component) {
        this.stack.set(type, component);
        return this;
    }

    public ItemStack buildAsStack() {
        return this.getStackInternal();
    }

    public ConfigurableItemStack buildAsParsedStack() {
        return ConfigurableItemStack.of(this.buildAsStack());
    }

    public boolean giveOrElse(Player player, Consumer<ItemStack> orElse) {
        return ItemUtils.giveOrElse(player, this.buildAsStack(), orElse);
    }

    public boolean giveOrDrop(Player player) {
        return ItemUtils.giveOrDrop(player, this.buildAsStack());
    }
}
