package de.waterdu.atlantis.ext.dev;

import de.waterdu.atlantis.util.item.ConfigurableItemStack;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public interface ResourceLocationExt {

    default <T> ResourceKey<T> toResourceKey(ResourceKey<? extends Registry<? extends T>> registry) {
        return null;
    }

    default <T> T get(ResourceKey<? extends Registry<? extends T>> registry, HolderLookup.Provider registryAccess) {
        return null;
    }

    default <T> T get(Registry<? extends T> registry) {
        return null;
    }

    default Item asItem() {
        return null;
    }

    default Block asBlock() {
        return null;
    }

    default ConfigurableItemStack asConfigurableItemStack() {
        return null;
    }
}
