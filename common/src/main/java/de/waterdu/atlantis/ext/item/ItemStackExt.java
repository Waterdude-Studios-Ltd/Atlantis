package de.waterdu.atlantis.ext.item;

import de.waterdu.atlantis.util.item.ConfigurableItemStack;
import de.waterdu.atlantis.util.item.NameProvider;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TooltipFlag;

import java.util.Collection;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

/**
 * Adds name, component and config helpers to {@link net.minecraft.world.item.ItemStack}. The interface is injected
 * into the item stack class.
 */
public interface ItemStackExt {

    /**
     * @return the registry key of the item of this stack
     */
    default ResourceKey<Item> key() {
        return null;
    }

    /**
     * @return the registry ID of the item of this stack
     */
    default ResourceLocation id() {
        return null;
    }

    /**
     * Sets the {@code ITEM_NAME} component and removes the {@code CUSTOM_NAME} component.
     *
     * @param name any object that {@link de.waterdu.atlantis.util.text.TextUtils#asComponent(Object)} accepts
     */
    default <T> void setDisplayName(T name) {}

    /**
     * {@link #setDisplayName(Object)} with the placeholders in the name replaced for the given contexts.
     */
    default <T> void setDisplayNameWithPlaceholders(T name, Object... contexts) {}

    /**
     * {@link #setDisplayName(Object)} with the placeholders in the name replaced for the given contexts.
     */
    default <T> void setDisplayNameWithPlaceholders(T name, Collection<Object> contexts) {}

    /**
     * Lets the {@link NameProvider} in the component change the name. Does nothing when the stack does not have the component.
     *
     * @param name the name to change. The provider sets the new name in this reference.
     */
    default <T extends NameProvider> void addToName(DataComponentType<T> component, Item.TooltipContext context, AtomicReference<Component> name, TooltipFlag tooltipFlag) {}

    /**
     * @return the component value. If the stack does not have the component, the value from {@code ifAbsent}, which is
     *     also set on the stack.
     */
    default <T> T computeComponentIfAbsent(DataComponentType<T> component, Supplier<T> ifAbsent) { return null; }

    /**
     * Replaces the component value with the result of {@code scope}. Does nothing when the stack does not have the component.
     */
    default <T> void componentScope(DataComponentType<T> component, UnaryOperator<T> scope) {}

    /**
     * Replaces the component value with the result of {@code scope}.
     * If the stack does not have the component, {@code scope} gets the value from {@code ifAbsent}.
     */
    default <T> void componentScope(DataComponentType<T> component, Supplier<T> ifAbsent, UnaryOperator<T> scope) {}

    /**
     * Removes the {@code null} component type from the component map of this stack.
     */
    default void sanitizeComponents() {}

    /**
     * Creates a configurable copy of this stack.
     *
     * @return a new configurable item stack of this stack
     */
    default ConfigurableItemStack configurable() {
        return null;
    }
}
