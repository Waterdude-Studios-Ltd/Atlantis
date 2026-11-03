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

/**
 * Builds an {@link ItemStack} with chained calls. The builder changes one stack, so the methods change the stack
 * that {@link #buildAsStack()} returns.
 */
public final class ItemBuilder {

    private ItemStack stack;
    private ServerPlayer target;

    /**
     * Creates a builder for the stack, with no placeholder target.
     *
     * @param stack the stack to change, or {@code null} for an empty stack
     */
    private ItemBuilder(ItemStack stack) {
        this.stack = stack;
        this.target = null;
    }

    /**
     * Set a stack with {@link #item} or {@link #stack} before you call the other methods.
     */
    public static ItemBuilder builder() {
        return ItemBuilder.builder((ItemStack) null);
    }

    /**
     * Creates a builder for a new stack from the item string.
     *
     * @param item the item string. See {@link ConfigurableItemStack}.
     * @return the new builder
     */
    public static ItemBuilder builder(String item) {
        return ItemBuilder.builder(ConfigurableItemStack.of(item));
    }

    /**
     * Creates a builder for a new stack of one item.
     *
     * @param like the item or block
     * @return the new builder
     */
    public static ItemBuilder builder(ItemLike like) {
        return ItemBuilder.builder(new ItemStack(like));
    }

    /**
     * Creates a builder for a new copy of the stack.
     *
     * @param stack the stack to copy
     * @return the new builder
     */
    public static ItemBuilder builder(ConfigurableItemStack stack) {
        return ItemBuilder.builder(stack.newStack());
    }

    /**
     * Creates a builder that changes the stack. It does not copy the stack.
     *
     * @param stack the stack to change
     * @return the new builder
     */
    public static ItemBuilder builder(ItemStack stack) {
        return new ItemBuilder(stack);
    }

    /**
     * Gets the stack of the builder. If no stack is set, it sets {@link ItemStack#EMPTY}.
     *
     * @return the stack
     */
    private ItemStack getStackInternal() {
        if (this.stack == null) {
            this.stack = ItemStack.EMPTY;
        }
        return this.stack;
    }

    /**
     * Checks if a player is set for placeholders.
     *
     * @return {@code true} if a placeholder target is set
     */
    private boolean hasPlaceholderTarget() {
        return this.target != null;
    }

    /**
     * Parses placeholders for the target in the name and lore that you set after this call.
     */
    public ItemBuilder placeholders(ServerPlayer target) {
        this.target = target;
        return this;
    }

    /**
     * Replaces the whole stack, including the count and components.
     */
    public ItemBuilder item(String item) {
        return this.stack(ConfigurableItemStack.of(item));
    }

    /**
     * Changes the item and keeps the count and components of the current stack.
     */
    public ItemBuilder item(ItemLike like) {
        var old = this.getStackInternal();
        if (old.isEmpty()) {
            return this.stack(new ItemStack(like), false);
        }
        return this.stack(new ItemStack(like.asItem().builtInRegistryHolder(), old.getCount(), old.getComponentsPatch()), false);
    }

    /**
     * Replaces the stack with a new copy of {@code stack}.
     *
     * @param stack the stack to copy
     * @return this builder
     */
    public ItemBuilder stack(ConfigurableItemStack stack) {
        return this.stack(stack.newStack(), false);
    }

    /**
     * Uses a copy of {@code stack}.
     */
    public ItemBuilder stack(ItemStack stack) {
        return this.stack(stack, true);
    }

    /**
     * Replaces the stack.
     *
     * @param stack the new stack
     * @param copy {@code true} to use a copy of {@code stack}, {@code false} to change {@code stack} itself
     * @return this builder
     */
    public ItemBuilder stack(ItemStack stack, boolean copy) {
        this.stack = copy ? stack.copy() : stack;
        return this;
    }

    /**
     * Sets the count of the stack.
     *
     * @param count the new count
     * @return this builder
     */
    public ItemBuilder count(int count) {
        this.stack.setCount(count);
        return this;
    }

    /**
     * Increases the count of the stack.
     *
     * @param amount the amount to add
     * @return this builder
     */
    public ItemBuilder grow(int amount) {
        this.stack.grow(amount);
        return this;
    }

    /**
     * Decreases the count of the stack.
     *
     * @param amount the amount to remove
     * @return this builder
     */
    public ItemBuilder shrink(int amount) {
        this.stack.shrink(amount);
        return this;
    }

    /**
     * Sets the damage of the stack.
     *
     * @param damage the new damage value
     * @return this builder
     */
    public ItemBuilder damage(int damage) {
        this.stack.setDamageValue(damage);
        return this;
    }

    /**
     * Sets the name of the stack. See {@link ItemUtils#setDisplayName(ItemStack, Object)}.
     * If a placeholder target is set, it parses the placeholders for the target.
     *
     * @param name the name
     * @return this builder
     */
    public ItemBuilder name(Object name) {
        if (this.hasPlaceholderTarget()) {
            ItemUtils.setDisplayNameWithPlaceholders(this.stack, name, this.target);
        } else {
            ItemUtils.setDisplayName(this.stack, name);
        }
        return this;
    }

    /**
     * Replaces the lore of the stack. If a placeholder target is set, it parses the placeholders for the target.
     *
     * @param lore the lines of the lore
     * @return this builder
     */
    public ItemBuilder lore(Object... lore) {
        if (this.hasPlaceholderTarget()) {
            ItemUtils.setDisplayLoreWithPlaceholders(this.stack, Collections.singletonList(this.target), lore);
        } else {
            ItemUtils.setDisplayLore(this.stack, lore);
        }
        return this;
    }

    /**
     * Replaces the lore of the stack. If a placeholder target is set, it parses the placeholders for the target.
     *
     * @param lore the lines of the lore
     * @return this builder
     */
    public ItemBuilder lore(Collection<Object> lore) {
        if (this.hasPlaceholderTarget()) {
            ItemUtils.setDisplayLoreWithPlaceholders(this.stack, Collections.singletonList(this.target), lore);
        } else {
            ItemUtils.setDisplayLore(this.stack, lore);
        }
        return this;
    }

    /**
     * Adds lines to the end of the lore of the stack. If a placeholder target is set, it parses the placeholders
     * for the target.
     *
     * @param lore the lines to add
     * @return this builder
     */
    public ItemBuilder addLore(Object... lore) {
        if (this.hasPlaceholderTarget()) {
            ItemUtils.addDisplayLoreWithPlaceholders(this.stack, Collections.singletonList(this.target), lore);
        } else {
            ItemUtils.addDisplayLore(this.stack, lore);
        }
        return this;
    }

    /**
     * Adds lines to the end of the lore of the stack. If a placeholder target is set, it parses the placeholders
     * for the target.
     *
     * @param lore the lines to add
     * @return this builder
     */
    public ItemBuilder addLore(Collection<Object> lore) {
        if (this.hasPlaceholderTarget()) {
            ItemUtils.addDisplayLoreWithPlaceholders(this.stack, Collections.singletonList(this.target), lore);
        } else {
            ItemUtils.addDisplayLore(this.stack, lore);
        }
        return this;
    }

    /**
     * Makes the stack glow if {@code glow} is {@code true}. If it is {@code false}, nothing changes.
     *
     * @param glow {@code true} to make the stack glow
     * @return this builder
     */
    public ItemBuilder glow(boolean glow) {
        if (glow) {
            this.glow();
        }
        return this;
    }

    /**
     * Makes the stack glow. See {@link ItemUtils#applyGlow(ItemStack)}.
     *
     * @return this builder
     */
    public ItemBuilder glow() {
        ItemUtils.applyGlow(this.stack);
        return this;
    }

    /**
     * Hides the attribute modifiers and the additional tooltip lines. See {@link ItemUtils#hideTooltips(ItemStack)}.
     *
     * @return this builder
     */
    public ItemBuilder hideTooltips() {
        ItemUtils.hideTooltips(this.stack);
        return this;
    }

    /**
     * Hides the tooltips if {@code hideTooltips} is {@code true}. If it is {@code false}, nothing changes.
     *
     * @param hideTooltips {@code true} to hide the tooltips
     * @return this builder
     */
    public ItemBuilder hideTooltips(boolean hideTooltips) {
        if (hideTooltips) {
            this.hideTooltips();
        }
        return this;
    }

    /**
     * Sets the enchantment on the stack at level 1.
     *
     * @param enchantment the enchantment
     * @return this builder
     */
    public ItemBuilder enchantment(Holder<Enchantment> enchantment) {
        return this.enchantment(enchantment, 1);
    }

    /**
     * Sets the enchantment on the stack. See {@link ItemUtils#addEnchantment(ItemStack, Holder, int)}.
     *
     * @param enchantment the enchantment
     * @param tier the level of the enchantment
     * @return this builder
     */
    public ItemBuilder enchantment(Holder<Enchantment> enchantment, int tier) {
        ItemUtils.addEnchantment(this.stack, enchantment, tier);
        return this;
    }

    /**
     * Replaces all enchantments of the stack.
     *
     * @param enchantments the new enchantments
     * @return this builder
     */
    public ItemBuilder enchantments(ItemEnchantments enchantments) {
        ItemUtils.setEnchantments(this.stack, enchantments);
        return this;
    }

    /**
     * Adds an attribute modifier to the stack, with no slot group.
     *
     * @param attribute the attribute to change
     * @param modifier the modifier
     * @return this builder
     */
    public ItemBuilder attribute(Holder<Attribute> attribute, AttributeModifier modifier) {
        return this.attribute(attribute, modifier, null);
    }

    /**
     * Adds an attribute modifier to the stack.
     *
     * @param attribute the attribute to change
     * @param modifier the modifier
     * @param slot the slot group in which the modifier applies
     * @return this builder
     */
    public ItemBuilder attribute(Holder<Attribute> attribute, AttributeModifier modifier, EquipmentSlotGroup slot) {
        this.stack.componentScope(
                DataComponents.ATTRIBUTE_MODIFIERS,
                () -> ItemAttributeModifiers.EMPTY,
                modifiers -> modifiers.withModifierAdded(attribute, modifier, slot)
        );
        return this;
    }

    /**
     * Sets the entity that the stack belongs to, such as an item frame.
     *
     * @param representation the entity
     * @return this builder
     */
    public ItemBuilder representation(Entity representation) {
        this.stack.setEntityRepresentation(representation);
        return this;
    }

    /**
     * Sets the pop time of the stack. This is the time of the pop animation when the stack enters an inventory.
     *
     * @param popTime the pop time, in ticks
     * @return this builder
     */
    public ItemBuilder popTime(int popTime) {
        this.stack.setPopTime(popTime);
        return this;
    }

    /**
     * Sets the repair cost component of the stack.
     *
     * @param repairCost the repair cost
     * @return this builder
     */
    public ItemBuilder repairCost(int repairCost) {
        this.stack.set(DataComponents.REPAIR_COST, repairCost);
        return this;
    }

    /**
     * Changes the custom data of the stack. See {@link ItemUtils#tag(ItemStack, Consumer)}.
     *
     * @param tag changes the custom data tag
     * @return this builder
     */
    public ItemBuilder tag(Consumer<CompoundTag> tag) {
        ItemUtils.tag(this.stack, tag);
        return this;
    }

    /**
     * Replaces all custom data of the stack.
     */
    public ItemBuilder tag(CompoundTag tag) {
        this.stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        return this;
    }

    /**
     * Sets the custom model data of the stack.
     *
     * @param customModelData the custom model data
     * @return this builder
     */
    public ItemBuilder customModelData(int customModelData) {
        ItemUtils.setCustomModelData(this.stack, customModelData);
        return this;
    }

    /**
     * Sets a data component on the stack.
     *
     * @param <T> the type of the component value
     * @param type the component type
     * @param component the component value
     * @return this builder
     */
    public <T> ItemBuilder component(DataComponentType<T> type, T component) {
        this.stack.set(type, component);
        return this;
    }

    /**
     * @return the stack of the builder, not a copy. Later builder calls change it.
     */
    public ItemStack buildAsStack() {
        return this.getStackInternal();
    }

    /**
     * Creates a {@link ConfigurableItemStack} from the stack. See {@link ConfigurableItemStack#of(ItemStack)}.
     *
     * @return the new stack
     */
    public ConfigurableItemStack buildAsParsedStack() {
        return ConfigurableItemStack.of(this.buildAsStack());
    }

    /**
     * Adds the stack to the inventory of the player. This changes the count of the stack of the builder.
     * See {@link ItemUtils#giveOrElse(Player, ItemStack, Consumer)}.
     *
     * @param player the player that gets the stack
     * @param orElse gets the part of the stack that does not fit
     * @return {@code true} if the inventory takes the full stack
     */
    public boolean giveOrElse(Player player, Consumer<ItemStack> orElse) {
        return ItemUtils.giveOrElse(player, this.buildAsStack(), orElse);
    }

    /**
     * Adds the stack to the inventory of the player and drops the part that does not fit.
     * See {@link ItemUtils#giveOrDrop(Player, ItemStack, Consumer, Consumer)}.
     *
     * @param player the player that gets the stack
     * @return {@code true} if the inventory takes the full stack
     */
    public boolean giveOrDrop(Player player) {
        return ItemUtils.giveOrDrop(player, this.buildAsStack());
    }
}
