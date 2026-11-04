package de.waterdu.atlantis.ext.dev;

import de.waterdu.atlantis.util.item.ConfigurableItemStack;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

/**
 * Adds registry lookups to {@link net.minecraft.resources.ResourceLocation} through interface injection.
 */
public interface ResourceLocationExt {

    /**
     * Creates a key for this id in the given registry.
     *
     * @param registry the key of the registry
     * @return the key of this id in the registry
     * @param <T> the type of the registry values
     */
    default <T> ResourceKey<T> toResourceKey(ResourceKey<? extends Registry<? extends T>> registry) {
        return null;
    }

    /**
     * Throws an exception when the registry or the value is not present.
     */
    default <T> T get(ResourceKey<? extends Registry<? extends T>> registry, HolderLookup.Provider registryAccess) {
        return null;
    }

    /**
     * @return the registered value. Can be null, or the registry default, when the id is not registered.
     */
    default <T> T get(Registry<? extends T> registry) {
        return null;
    }

    /**
     * Throws an exception when no server runs.
     *
     * @return the registered item, or air when the id is not registered
     */
    default Item asItem() {
        return null;
    }

    /**
     * Throws an exception when no server runs.
     *
     * @return the registered block, or air when the id is not registered
     */
    default Block asBlock() {
        return null;
    }

    /**
     * Creates a {@link ConfigurableItemStack} of the item with this id.
     *
     * @return a new configurable stack
     */
    default ConfigurableItemStack asConfigurableItemStack() {
        return null;
    }
}
