package de.waterdu.atlantis.mixin.registry;

import net.minecraft.core.WritableRegistry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(targets = "net.minecraft.resources.RegistryDataLoader$Loader")
public interface MixinRegistryDataLoaderLoader {
    @Accessor("registry")
    WritableRegistry<?> atlantis$getRegistry();
}
