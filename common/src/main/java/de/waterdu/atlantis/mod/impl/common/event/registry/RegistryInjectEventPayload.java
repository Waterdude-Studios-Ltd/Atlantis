package de.waterdu.atlantis.mod.impl.common.event.registry;

import com.mojang.serialization.Lifecycle;
import de.waterdu.atlantis.Atlantis;
import de.waterdu.atlantis.AtlantisLogger;
import de.waterdu.atlantis.mod.impl.common.event.EventPayload;
import de.waterdu.atlantis.mod.wrapper.modloader.event.UEvent;
import de.waterdu.atlantis.util.config.Key;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistrationInfo;
import net.minecraft.core.Registry;
import net.minecraft.core.WritableRegistry;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.repository.KnownPack;

import java.util.Optional;

public class RegistryInjectEventPayload implements EventPayload {

    private static final RegistrationInfo ATLANTIS_INFO = makeRegistrationInfoForMod(Atlantis.ID, Atlantis.VERSION);

    private final WritableRegistry<?> registry;
    private final RegistryOps.RegistryInfoLookup lookup;

    public RegistryInjectEventPayload(WritableRegistry<?> registry, RegistryOps.RegistryInfoLookup lookup) {
        AtlantisLogger.debug("Injecting into registry {}", registry.key().location());
        this.registry = registry;
        this.lookup = lookup;
    }

    public static RegistrationInfo makeRegistrationInfoForMod(String modID, String version) {
        return new RegistrationInfo(Optional.of(new KnownPack("atlantis", "mod/" + modID, version)), Lifecycle.stable());
    }

    @Override
    public Key<?> type() {
        return UEvent.REGISTRY_INJECT;
    }

    public boolean isForRegistry(ResourceKey<? extends Registry<?>> key) {
        return this.registry.key().equals(key);
    }

    public <E> Optional<RegistryOps.RegistryInfo<E>> lookup(ResourceKey<? extends Registry<E>> registry) {
        return this.lookup.lookup(registry);
    }

    public <E> Optional<Holder<E>> get(ResourceKey<? extends Registry<E>> registry, ResourceKey<E> key) {
        return this.lookup(registry).flatMap(info -> info.getter().get(key));
    }

    public <E> Holder<E> register(String key, E value) {
        return this.register(key, value, ATLANTIS_INFO);
    }

    @SuppressWarnings("unchecked")
    public <E> Holder<E> register(String key, E value, RegistrationInfo info) {
        return this.register((ResourceKey<E>) ResourceKey.create(this.registry.key(), ResourceLocation.parse(key)), value, info);
    }

    public <E> Holder<E> register(ResourceKey<E> key, E value) {
        return this.register(key, value, ATLANTIS_INFO);
    }

    @SuppressWarnings("unchecked")
    public <E> Holder<E> register(ResourceKey<E> key, E value, RegistrationInfo info) {
        var registry = (WritableRegistry<E>) this.registry;
        return registry.register(key, value, info);
    }
}
