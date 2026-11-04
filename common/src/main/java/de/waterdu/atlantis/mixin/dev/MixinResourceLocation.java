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

/**
 * Implements {@link ResourceLocationExt} on {@link ResourceLocation} with registry lookups.
 */
@Mixin(value = ResourceLocation.class)
public abstract class MixinResourceLocation implements ResourceLocationExt {

    /**
     * Creates a key in the registry with this location.
     *
     * @param registry the key of the registry
     * @param <T> the type of the registry values
     * @return the new key
     */
    @SuppressWarnings("unchecked")
    @Override
    public <T> ResourceKey<T> toResourceKey(ResourceKey<? extends Registry<? extends T>> registry) {
        return ResourceKey.create((ResourceKey<? extends Registry<T>>) registry, (ResourceLocation) (Object) this);
    }

    /**
     * Gets the value with this location from the registry lookup of the provider.
     *
     * @param registry the key of the registry
     * @param registryAccess the provider of the registry lookups
     * @param <T> the type of the registry values
     * @return the registered value
     */
    @Override
    public <T> T get(ResourceKey<? extends Registry<? extends T>> registry, HolderLookup.Provider registryAccess) {
        return registryAccess.lookupOrThrow(registry).getOrThrow(this.toResourceKey(registry)).value();
    }

    /**
     * Gets the value with this location from the registry.
     *
     * @param registry the registry
     * @param <T> the type of the registry values
     * @return the registered value
     */
    @Override
    public <T> T get(Registry<? extends T> registry) {
        return registry.get((ResourceLocation) (Object) this);
    }

    /**
     * Gets the item with this location from the item registry.
     *
     * @return the item
     */
    @Override
    public Item asItem() {
        return ItemUtils.itemRegistry().get(this.self());
    }

    /**
     * Gets the block with this location from the block registry.
     *
     * @return the block
     */
    @Override
    public Block asBlock() {
        return ItemUtils.blockRegistry().get(this.self());
    }

    /**
     * Creates a configurable item stack of the item with this location.
     *
     * @return the new configurable item stack
     */
    @Override
    public ConfigurableItemStack asConfigurableItemStack() {
        return ConfigurableItemStack.of(this.self());
    }

    /**
     * @return this mixin cast to {@link ResourceLocation}
     */
    @Unique
    private ResourceLocation self() {
        return (ResourceLocation) (Object) this;
    }
}
