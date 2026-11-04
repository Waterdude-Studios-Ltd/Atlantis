package de.waterdu.atlantis.mixin.registry;

import net.minecraft.core.WritableRegistry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Gives access to the registry of a {@code RegistryDataLoader.Loader}.
 */
@Mixin(targets = "net.minecraft.resources.RegistryDataLoader$Loader")
public interface MixinRegistryDataLoaderLoader {
    /**
     * @return the registry that this loader fills
     */
    @Accessor("registry")
    WritableRegistry<?> atlantis$getRegistry();
}
