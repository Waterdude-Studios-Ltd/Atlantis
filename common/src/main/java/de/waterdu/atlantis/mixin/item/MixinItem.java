package de.waterdu.atlantis.mixin.item;

import de.waterdu.atlantis.ext.item.ItemExt;
import de.waterdu.atlantis.util.item.ConfigurableItemStack;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/**
 * Implements {@link ItemExt} on {@link Item}.
 */
@Mixin(value = Item.class)
public abstract class MixinItem implements ItemExt {

    /**
     * Gets the key of the built-in registry holder of this item.
     *
     * @return the registry key of this item
     */
    @Override
    public ResourceKey<Item> key() {
        return this.self().builtInRegistryHolder().key();
    }

    /**
     * @return the location of the {@link #key()}
     */
    @Override
    public ResourceLocation id() {
        return this.key().location();
    }

    /**
     * Creates a configurable item stack of this item.
     *
     * @return the new configurable item stack
     */
    @Override
    public ConfigurableItemStack configurable() {
        return ConfigurableItemStack.of(this.self());
    }

    /**
     * @return this mixin cast to {@link Item}
     */
    @Unique
    private Item self() {
        return (Item) (Object) this;
    }
}
