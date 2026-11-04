package de.waterdu.atlantis.ext.dev;

import net.minecraft.SharedConstants;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests the methods that {@link ResourceLocationExt} adds to {@link ResourceLocation}.
 */
class ResourceLocationExtTest {

    /**
     * Bootstraps Minecraft once before the tests, so that the registries are filled.
     */
    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    /**
     * Checks that {@code toResourceKey} creates a key in the given registry with the same location.
     */
    @Test
    void toResourceKeyKeepsTheLocation() {
        var location = ResourceLocation.withDefaultNamespace("diamond");

        assertThat(location.<net.minecraft.world.item.Item>toResourceKey(Registries.ITEM)).isEqualTo(ResourceKey.create(Registries.ITEM, location));
    }

    /**
     * Checks that {@code get} finds the entry with the location in the given registry.
     */
    @Test
    void getLooksUpTheGivenRegistry() {
        assertThat(ResourceLocation.withDefaultNamespace("diamond").get(BuiltInRegistries.ITEM)).isSameAs(Items.DIAMOND);
        assertThat(ResourceLocation.withDefaultNamespace("stone").get(BuiltInRegistries.BLOCK)).isSameAs(Blocks.STONE);
    }

    /**
     * Checks that {@code get} gives the default entry of the registry for an unknown location.
     */
    @Test
    void getFallsBackToTheRegistryDefault() {
        assertThat(ResourceLocation.fromNamespaceAndPath("atlantis", "missing").get(BuiltInRegistries.ITEM)).isSameAs(Items.AIR);
    }

    /**
     * Checks that {@code asConfigurableItemStack} keeps the full ID as the original string.
     */
    @Test
    void asConfigurableItemStackKeepsTheId() {
        assertThat(ResourceLocation.withDefaultNamespace("diamond").asConfigurableItemStack().originalString()).isEqualTo("minecraft:diamond");
    }
}
