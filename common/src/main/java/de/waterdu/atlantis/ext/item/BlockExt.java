package de.waterdu.atlantis.ext.item;

import de.waterdu.atlantis.util.item.ConfigurableItemStack;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;

/**
 * Adds registry and item stack methods to {@link Block} through interface injection.
 */
public interface BlockExt {

    /**
     * @return the key of this block in the built-in block registry
     */
    default ResourceKey<Block> key() {
        return null;
    }

    /**
     * @return the registry id of this block
     */
    default ResourceLocation id() {
        return null;
    }

    /**
     * Creates a {@link ConfigurableItemStack} of this block.
     *
     * @return a new configurable stack
     */
    default ConfigurableItemStack configurable() {
        return null;
    }
}
