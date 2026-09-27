package de.waterdu.atlantis.mixin.dev;

import de.waterdu.atlantis.ext.dev.ResourceLocationExt;
import de.waterdu.atlantis.util.item.ConfigurableItemStack;
import de.waterdu.atlantis.util.item.ItemUtils;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(value = ResourceLocation.class)
public abstract class MixinResourceLocation implements ResourceLocationExt {

    @SuppressWarnings("unchecked")
    @Override
    public <T> ResourceKey<T> toResourceKey(ResourceKey<? extends Registry<? extends T>> registry) {
        return ResourceKey.create((ResourceKey<? extends Registry<T>>) registry, (ResourceLocation) (Object) this);
    }

    @Override
    public <T> T get(ResourceKey<? extends Registry<? extends T>> registry, HolderLookup.Provider registryAccess) {
        return registryAccess.lookupOrThrow(registry).getOrThrow(this.toResourceKey(registry)).value();
    }

    @Override
    public <T> T get(Registry<? extends T> registry) {
        return registry.get((ResourceLocation) (Object) this);
    }

    @Override
    public Item asItem() {
        return ItemUtils.itemRegistry().get(this.self());
    }

    @Override
    public Block asBlock() {
        return ItemUtils.blockRegistry().get(this.self());
    }

    @Override
    public ConfigurableItemStack asConfigurableItemStack() {
        return ConfigurableItemStack.of(this.self());
    }

    @Unique
    private ResourceLocation self() {
        return (ResourceLocation) (Object) this;
    }
}
