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

/**
 * Holds the data of {@link UEvent#REGISTRY_INJECT}, which fires for each data-driven registry after it loads and
 * before it freezes. Listeners can add entries to the registry.
 */
public class RegistryInjectEventPayload implements EventPayload {

    private static final RegistrationInfo ATLANTIS_INFO = makeRegistrationInfoForMod(Atlantis.ID, Atlantis.VERSION);

    private final WritableRegistry<?> registry;
    private final RegistryOps.RegistryInfoLookup lookup;

    /**
     * Creates the payload for one registry and logs a debug message.
     *
     * @param registry the registry that listeners can add entries to
     * @param lookup the lookup for the other registries that are loading
     */
    public RegistryInjectEventPayload(WritableRegistry<?> registry, RegistryOps.RegistryInfoLookup lookup) {
        AtlantisLogger.debug("Injecting into registry {}", registry.key().location());
        this.registry = registry;
        this.lookup = lookup;
    }

    /**
     * Creates registration info with a stable lifecycle that marks entries as added by a mod.
     *
     * @param modId the ID of the mod that adds the entries
     * @param version the version of the mod
     * @return the registration info, with the known pack {@code atlantis:mod/<modId>}
     */
    public static RegistrationInfo makeRegistrationInfoForMod(String modId, String version) {
        return new RegistrationInfo(Optional.of(new KnownPack("atlantis", "mod/" + modId, version)), Lifecycle.stable());
    }

    /**
     * @return {@link UEvent#REGISTRY_INJECT}
     */
    @Override
    public Key<?> type() {
        return UEvent.REGISTRY_INJECT;
    }

    /**
     * Checks if this event is for the given registry.
     *
     * @param key the key of the registry to check
     * @return {@code true} if the registry of this event has that key
     */
    public boolean isForRegistry(ResourceKey<? extends Registry<?>> key) {
        return this.registry.key().equals(key);
    }

    /**
     * Gets the info of another registry that is loading.
     *
     * @param registry the key of the registry
     * @param <E> the type of the registry entries
     * @return the registry info, or empty if the lookup does not know the registry
     */
    public <E> Optional<RegistryOps.RegistryInfo<E>> lookup(ResourceKey<? extends Registry<E>> registry) {
        return this.lookup.lookup(registry);
    }

    /**
     * Gets an entry of another registry that is loading.
     *
     * @param registry the key of the registry
     * @param key the key of the entry
     * @param <E> the type of the registry entries
     * @return the holder of the entry, or empty if the registry or the entry is not found
     */
    public <E> Optional<Holder<E>> get(ResourceKey<? extends Registry<E>> registry, ResourceKey<E> key) {
        return this.lookup(registry).flatMap(info -> info.getter().get(key));
    }

    /**
     * Registers a value in the registry of this event with the registration info of Atlantis.
     *
     * @param key the resource location of the entry, parsed with {@link ResourceLocation#parse}
     * @param value the value to register
     * @param <E> the type of the registry entries
     * @return the holder of the new entry
     */
    public <E> Holder<E> register(String key, E value) {
        return this.register(key, value, ATLANTIS_INFO);
    }

    /**
     * Registers a value in the registry of this event.
     *
     * @param key the resource location of the entry, parsed with {@link ResourceLocation#parse}
     * @param value the value to register
     * @param info the registration info of the entry
     * @param <E> the type of the registry entries
     * @return the holder of the new entry
     */
    @SuppressWarnings("unchecked")
    public <E> Holder<E> register(String key, E value, RegistrationInfo info) {
        return this.register((ResourceKey<E>) ResourceKey.create(this.registry.key(), ResourceLocation.parse(key)), value, info);
    }

    /**
     * Registers a value in the registry of this event with the registration info of Atlantis.
     *
     * @param key the key of the entry
     * @param value the value to register
     * @param <E> the type of the registry entries
     * @return the holder of the new entry
     */
    public <E> Holder<E> register(ResourceKey<E> key, E value) {
        return this.register(key, value, ATLANTIS_INFO);
    }

    /**
     * Registers a value in the registry of this event. The type of the value is not checked against the registry.
     *
     * @param key the key of the entry
     * @param value the value to register
     * @param info the registration info of the entry
     * @param <E> the type of the registry entries
     * @return the holder of the new entry
     */
    @SuppressWarnings("unchecked")
    public <E> Holder<E> register(ResourceKey<E> key, E value, RegistrationInfo info) {
        var registry = (WritableRegistry<E>) this.registry;
        return registry.register(key, value, info);
    }
}
