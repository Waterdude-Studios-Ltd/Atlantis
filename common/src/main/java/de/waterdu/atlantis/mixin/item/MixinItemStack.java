package de.waterdu.atlantis.mixin.item;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import de.waterdu.atlantis.mod.Mods;
import de.waterdu.atlantis.ext.item.ItemStackExt;
import de.waterdu.atlantis.util.item.ConfigurableItemStack;
import de.waterdu.atlantis.util.item.ItemUtils;
import de.waterdu.atlantis.util.item.NameProvider;
import net.minecraft.core.component.DataComponentHolder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.PatchedDataComponentMap;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Collection;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

/**
 * Implements {@link ItemStackExt} on {@link ItemStack}, and emits ITEM_NAME when the tooltip gets the name of the
 * stack.
 */
@Mixin(value = ItemStack.class)
public abstract class MixinItemStack implements ItemStackExt, DataComponentHolder {

    @Shadow
    @Final
    PatchedDataComponentMap components;

    /**
     * Emits ITEM_NAME and uses the name that the listeners set as the first tooltip line.
     *
     * @param original the hover name of the stack
     * @param tooltipContext the tooltip context
     * @param player the player that sees the tooltip, or {@code null} if there is none
     * @param tooltipFlag the tooltip flag
     * @return the name after all listeners ran
     */
    @ModifyExpressionValue(method = "getTooltipLines", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;getHoverName()Lnet/minecraft/network/chat/Component;"))
    private Component modifyHoverName(Component original, @Local(argsOnly = true) Item.TooltipContext tooltipContext, @Local(argsOnly = true) Player player, @Local(argsOnly = true) TooltipFlag tooltipFlag) {
        var name = new AtomicReference<>(original);
        Mods.events().emitItemName(this.self(), player, tooltipContext, name, tooltipFlag);
        return name.get();
    }

    /**
     * Gets the key of the built-in registry holder of the item of this stack.
     *
     * @return the registry key of the item of this stack
     */
    @Override
    public ResourceKey<Item> key() {
        return this.self().getItem().builtInRegistryHolder().key();
    }

    /**
     * @return the location of the {@link #key()}
     */
    @Override
    public ResourceLocation id() {
        return this.key().location();
    }

    /**
     * Creates a configurable copy of this stack.
     *
     * @return the new configurable item stack
     */
    @Override
    public ConfigurableItemStack configurable() {
        return ConfigurableItemStack.of(this.self());
    }

    /**
     * Sets the display name with {@link ItemUtils#setDisplayName}.
     *
     * @param name any object that {@link de.waterdu.atlantis.util.text.TextUtils#asComponent(Object)} accepts
     * @param <T> the type of the name
     */
    @Override
    public <T> void setDisplayName(T name) {
        ItemUtils.setDisplayName(this.self(), name);
    }

    /**
     * Sets the display name with {@link ItemUtils#setDisplayNameWithPlaceholders}.
     *
     * @param name the name with placeholders
     * @param contexts the contexts to replace the placeholders for
     * @param <T> the type of the name
     */
    @Override
    public <T> void setDisplayNameWithPlaceholders(T name, Object... contexts) {
        ItemUtils.setDisplayNameWithPlaceholders(this.self(), name, contexts);
    }

    /**
     * Sets the display name with {@link ItemUtils#setDisplayNameWithPlaceholders}.
     *
     * @param name the name with placeholders
     * @param contexts the contexts to replace the placeholders for
     * @param <T> the type of the name
     */
    @Override
    public <T> void setDisplayNameWithPlaceholders(T name, Collection<Object> contexts) {
        ItemUtils.setDisplayNameWithPlaceholders(this.self(), name, contexts);
    }

    /**
     * Lets the {@link NameProvider} in the component change the name, with {@link NameProvider#addToName}.
     *
     * @param component the component type that holds the name provider
     * @param context the tooltip context
     * @param name the name to change. The provider sets the new name in this reference.
     * @param tooltipFlag the tooltip flag
     * @param <T> the type of the name provider
     */
    @Override
    public <T extends NameProvider> void addToName(DataComponentType<T> component, Item.TooltipContext context, AtomicReference<Component> name, TooltipFlag tooltipFlag) {
        NameProvider.addToName(this.self(), component, context, name, tooltipFlag);
    }

    /**
     * Gets the component value. If the stack does not have the component, sets the value from {@code ifAbsent}.
     *
     * @param component the component type
     * @param ifAbsent supplies the value when the stack does not have the component
     * @param <T> the type of the component value
     * @return the component value
     */
    @Override
    public <T> T computeComponentIfAbsent(DataComponentType<T> component, Supplier<T> ifAbsent) {
        var self = this.self();
        var data = self.get(component);
        if (data == null) {
            data = ifAbsent.get();
            self.set(component, data);
        }
        return data;
    }

    /**
     * Replaces the component value with the result of {@code scope}. Does nothing when the stack does not have the
     * component.
     *
     * @param component the component type
     * @param scope changes the component value
     * @param <T> the type of the component value
     */
    @Override
    public <T> void componentScope(DataComponentType<T> component, UnaryOperator<T> scope) {
        var self = this.self();
        var data = self.get(component);
        if (data != null) {
            self.set(component, scope.apply(data));
        }
    }

    /**
     * Replaces the component value with the result of {@code scope}. If the stack does not have the component,
     * {@code scope} gets the value from {@code ifAbsent}. Sets nothing if both values are {@code null}.
     *
     * @param component the component type
     * @param ifAbsent supplies the value when the stack does not have the component
     * @param scope changes the component value
     * @param <T> the type of the component value
     */
    @Override
    public <T> void componentScope(DataComponentType<T> component, Supplier<T> ifAbsent, UnaryOperator<T> scope) {
        var self = this.self();
        var data = self.get(component);
        if (data == null) {
            data = ifAbsent.get();
        }
        var newData = scope.apply(data);
        if (data == null && newData == null) {
            return;
        }
        self.set(component, newData);
    }

    /**
     * Removes the {@code null} component type from the component map of this stack.
     */
    @Override
    public void sanitizeComponents() {
        this.components.remove(null);
    }

    /**
     * @return this mixin cast to {@link ItemStack}
     */
    @Unique
    private ItemStack self() {
        return (ItemStack) (Object) this;
    }
}
