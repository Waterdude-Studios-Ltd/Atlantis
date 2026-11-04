package de.waterdu.atlantis.mixin.item;

import de.waterdu.atlantis.ext.item.BlockExt;
import de.waterdu.atlantis.util.item.ConfigurableItemStack;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/**
 * Implements {@link BlockExt} on {@link Block}.
 */
@Mixin(value = Block.class)
public abstract class MixinBlock implements BlockExt {

    /**
     * Gets the key of the built-in registry holder of this block.
     *
     * @return the registry key of this block
     */
    @Override
    public ResourceKey<Block> key() {
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
     * Creates a configurable item stack of this block.
     *
     * @return the new configurable item stack
     */
    @Override
    public ConfigurableItemStack configurable() {
        return ConfigurableItemStack.of(this.self());
    }

    /**
     * @return this mixin cast to {@link Block}
     */
    @Unique
    private Block self() {
        return (Block) (Object) this;
    }
}
