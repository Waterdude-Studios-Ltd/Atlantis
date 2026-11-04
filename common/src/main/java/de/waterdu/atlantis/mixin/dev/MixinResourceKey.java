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

/**
 * Implements {@link ResourceKeyExt} on {@link ResourceKey} with lookups in the server registries.
 *
 * @param <T> the type of the value that the key names
 */
@Mixin(value = ResourceKey.class)
public abstract class MixinResourceKey<T> implements ResourceKeyExt<T> {

    @Shadow @Final private ResourceLocation location;

    /**
     * @return the key of the registry that holds the value of this key
     */
    @Shadow public abstract ResourceKey<Registry<T>> registryKey();

    /**
     * Gets the server registry for the registry key of this key.
     *
     * @return the registry that holds the value of this key
     */
    @Override
    public Registry<T> registryAccess() {
        return ServerUtils.registry(this.registryKey());
    }

    /**
     * Gets the value for the location of this key from {@link #registryAccess()}.
     *
     * @return the registered value
     */
    @Override
    public T get() {
        return this.registryAccess().get(this.location);
    }

    /**
     * Gets the server registry that this key names, cast to {@code T}.
     *
     * @return the registry that this key names
     */
    @SuppressWarnings("unchecked")
    @Override
    public T registryFromRegistryKey() {
        return (T) ServerUtils.registry((ResourceKey<Registry<T>>) (Object) this);
    }

    /**
     * Gets the holder of this key from {@link #registryAccess()}.
     *
     * @return the holder of the registered value
     */
    @SuppressWarnings("unchecked")
    @Override
    public Holder<T> holder() {
        return this.registryAccess().getHolderOrThrow((ResourceKey<T>) (Object) this);
    }
}
