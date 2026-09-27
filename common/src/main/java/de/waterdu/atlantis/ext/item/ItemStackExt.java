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

public interface ItemStackExt {

    default ResourceKey<Item> key() {
        return null;
    }

    default ResourceLocation id() {
        return null;
    }

    default <T> void setDisplayName(T name) {}

    default <T> void setDisplayNameWithPlaceholders(T name, Object... contexts) {}

    default <T> void setDisplayNameWithPlaceholders(T name, Collection<Object> contexts) {}

    default <T extends NameProvider> void addToName(DataComponentType<T> component, Item.TooltipContext context, AtomicReference<Component> name, TooltipFlag tooltipFlag) {}

    default <T> T computeComponentIfAbsent(DataComponentType<T> component, Supplier<T> ifAbsent) { return null; }

    default <T> void componentScope(DataComponentType<T> component, UnaryOperator<T> scope) {}

    default <T> void componentScope(DataComponentType<T> component, Supplier<T> ifAbsent, UnaryOperator<T> scope) {}

    default void sanitizeComponents() {}

    default ConfigurableItemStack configurable() {
        return null;
    }
}
