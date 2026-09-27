package de.waterdu.atlantis.ext.item;

import de.waterdu.atlantis.util.item.ConfigurableItemStack;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

public interface ItemExt {

    default ResourceKey<Item> key() {
        return null;
    }

    default ResourceLocation id() {
        return null;
    }

    default ConfigurableItemStack configurable() {
        return null;
    }
}
