package de.waterdu.atlantis.mixin.item;

import de.waterdu.atlantis.ext.item.ItemExt;
import de.waterdu.atlantis.util.item.ConfigurableItemStack;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(value = Item.class)
public abstract class MixinItem implements ItemExt {

    @Override
    public ResourceKey<Item> key() {
        return this.self().builtInRegistryHolder().key();
    }

    @Override
    public ResourceLocation id() {
        return this.key().location();
    }

    @Override
    public ConfigurableItemStack configurable() {
        return ConfigurableItemStack.of(this.self());
    }

    @Unique
    private Item self() {
        return (Item) (Object) this;
    }
}
