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

@Mixin(value = ItemStack.class)
public abstract class MixinItemStack implements ItemStackExt, DataComponentHolder {

    @Shadow
    @Final
    PatchedDataComponentMap components;

    @ModifyExpressionValue(method = "getTooltipLines", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;getHoverName()Lnet/minecraft/network/chat/Component;"))
    private Component modifyHoverName(Component original, @Local(argsOnly = true) Item.TooltipContext tooltipContext, @Local(argsOnly = true) Player player, @Local(argsOnly = true) TooltipFlag tooltipFlag) {
        var name = new AtomicReference<>(original);
        Mods.events().emitItemName(this.self(), player, tooltipContext, name, tooltipFlag);
        return name.get();
    }

    @Override
    public ResourceKey<Item> key() {
        return this.self().getItem().builtInRegistryHolder().key();
    }

    @Override
    public ResourceLocation id() {
        return this.key().location();
    }

    @Override
    public ConfigurableItemStack configurable() {
        return ConfigurableItemStack.of(this.self());
    }

    @Override
    public <T> void setDisplayName(T name) {
        ItemUtils.setDisplayName(this.self(), name);
    }

    @Override
    public <T> void setDisplayNameWithPlaceholders(T name, Object... contexts) {
        ItemUtils.setDisplayNameWithPlaceholders(this.self(), name, contexts);
    }

    @Override
    public <T> void setDisplayNameWithPlaceholders(T name, Collection<Object> contexts) {
        ItemUtils.setDisplayNameWithPlaceholders(this.self(), name, contexts);
    }

    @Override
    public <T extends NameProvider> void addToName(DataComponentType<T> component, Item.TooltipContext context, AtomicReference<Component> name, TooltipFlag tooltipFlag) {
        NameProvider.addToName(this.self(), component, context, name, tooltipFlag);
    }

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

    @Override
    public <T> void componentScope(DataComponentType<T> component, UnaryOperator<T> scope) {
        var self = this.self();
        var data = self.get(component);
        if (data != null) {
            self.set(component, scope.apply(data));
        }
    }

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

    @Override
    public void sanitizeComponents() {
        this.components.remove(null);
    }

    @Unique
    private ItemStack self() {
        return (ItemStack) (Object) this;
    }
}
