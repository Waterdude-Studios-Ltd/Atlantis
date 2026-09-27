package de.waterdu.atlantis.mixin.item;

import de.waterdu.atlantis.ext.item.BlockExt;
import de.waterdu.atlantis.util.item.ConfigurableItemStack;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(value = Block.class)
public abstract class MixinBlock implements BlockExt {

    @Override
    public ResourceKey<Block> key() {
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
    private Block self() {
        return (Block) (Object) this;
    }
}
