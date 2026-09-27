package de.waterdu.atlantis.mixin.registry;

import de.waterdu.atlantis.mod.Mods;
import de.waterdu.atlantis.mod.impl.common.event.registry.RegistryInjectEventPayload;
import de.waterdu.atlantis.mod.wrapper.modloader.event.UEvent;
import net.minecraft.resources.RegistryDataLoader;
import net.minecraft.resources.RegistryOps;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

@Mixin(value = RegistryDataLoader.class)
public abstract class MixinRegistryDataLoader {

    @Unique
    private static final AtomicReference<RegistryOps.RegistryInfoLookup> atlantis$REGISTRY_INFO_LOOKUP = new AtomicReference<>();

    @ModifyVariable(
            method = "load(Lnet/minecraft/resources/RegistryDataLoader$LoadingFunction;Lnet/minecraft/core/RegistryAccess;Ljava/util/List;)Lnet/minecraft/core/RegistryAccess$Frozen;",
            at = @At(value = "STORE")
    )
    private static RegistryOps.RegistryInfoLookup retrieveRegistryInfoLookup(RegistryOps.RegistryInfoLookup value) {
        atlantis$REGISTRY_INFO_LOOKUP.set(value);
        return value;
    }

    @ModifyArg(
            method = "load(Lnet/minecraft/resources/RegistryDataLoader$LoadingFunction;Lnet/minecraft/core/RegistryAccess;Ljava/util/List;)Lnet/minecraft/core/RegistryAccess$Frozen;",
            at = @At(value = "INVOKE", target = "Ljava/util/List;forEach(Ljava/util/function/Consumer;)V", ordinal = 1)
    )
    private static Consumer<Object> beforeFreeze(Consumer<Object> original) {
        return loader -> {
            var accessor = (MixinRegistryDataLoaderLoader) loader;
            Mods.modLoader().event(UEvent.REGISTRY_INJECT).emit(new RegistryInjectEventPayload(accessor.atlantis$getRegistry(), atlantis$REGISTRY_INFO_LOOKUP.get()));
            original.accept(loader);
        };
    }
}
