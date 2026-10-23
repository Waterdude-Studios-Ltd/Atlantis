package de.waterdu.atlantis.gametest;

import de.waterdu.atlantis.level.level.AtlantisLevel;
import de.waterdu.atlantis.level.level.AtlantisLevelRegistry;
import de.waterdu.atlantis.mod.impl.common.event.registry.RegistryInjectEventPayload;
import de.waterdu.atlantis.mod.wrapper.modloader.event.ListenerEvent;
import de.waterdu.atlantis.mod.wrapper.modloader.event.UEvent;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.Level;

import java.lang.reflect.Method;

/**
 * An Atlantis level the test mod registers before the server loads its registries, so REGISTRY_INJECT can be
 * tested without writing Atlantis' level config first.
 */
public final class GameTestLevels {

    public static final ResourceKey<Level> INJECTED_KEY = ResourceKey.create(Registries.DIMENSION, ResourceLocation.fromNamespaceAndPath(AtlantisGameTests.NAMESPACE, "injected"));

    private static AtlantisLevel injected;

    private GameTestLevels() {}

    public static AtlantisLevel injected() {
        return injected;
    }

    static synchronized void registerOnce() {
        if (injected != null) {
            return;
        }
        injected = AtlantisLevel.builder(INJECTED_KEY.location().getNamespace(), INJECTED_KEY.location().getPath())
                .chunkGenerator(AtlantisLevelRegistry.SUPERFLAT_CHUNK_GENERATOR, "minecraft:stone")
                .dimensionType(AtlantisLevelRegistry.UNIQUE_OVERWORLD_DIMENSION_TYPE)
                .build(false);
        var onRegistryInject = onRegistryInject();
        ListenerEvent.of(UEvent.REGISTRY_INJECT).listen(event -> {
            try {
                return (InteractionResult) onRegistryInject.invoke(injected, event);
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("Could not inject the GameTest level", e);
            }
        });
    }

    private static Method onRegistryInject() {
        try {
            var method = AtlantisLevel.class.getDeclaredMethod("onRegistryInject", RegistryInjectEventPayload.class);
            method.setAccessible(true);
            return method;
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException(e);
        }
    }
}
