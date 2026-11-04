package de.waterdu.atlantis.ext.item;

import de.waterdu.atlantis.util.item.ConfigurableItemStack;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

/**
 * Adds registry and config helpers to {@link Item}. The interface is injected into the item class.
 */
public interface ItemExt {

    /**
     * @return the registry key of this item
     */
    default ResourceKey<Item> key() {
        return null;
    }

    /**
     * @return the registry ID of this item
     */
    default ResourceLocation id() {
        return null;
    }

    /**
     * Creates a configurable copy of this item.
     *
     * @return a new configurable item stack of this item
     */
    default ConfigurableItemStack configurable() {
        return null;
    }
}
