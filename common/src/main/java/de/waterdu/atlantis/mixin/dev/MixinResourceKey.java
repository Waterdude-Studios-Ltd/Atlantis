package de.waterdu.atlantis.mixin.dev;

import de.waterdu.atlantis.ext.dev.ResourceKeyExt;
import de.waterdu.atlantis.util.server.ServerUtils;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = ResourceKey.class)
public abstract class MixinResourceKey<T> implements ResourceKeyExt<T> {

    @Shadow @Final private ResourceLocation location;

    @Shadow public abstract ResourceKey<Registry<T>> registryKey();

    @Override
    public Registry<T> registryAccess() {
        return ServerUtils.registry(this.registryKey());
    }

    @Override
    public T get() {
        return this.registryAccess().get(this.location);
    }

    @SuppressWarnings("unchecked")
    @Override
    public T registryFromRegistryKey() {
        return (T) ServerUtils.registry((ResourceKey<Registry<T>>) (Object) this);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Holder<T> holder() {
        return this.registryAccess().getHolderOrThrow((ResourceKey<T>) (Object) this);
    }
}
