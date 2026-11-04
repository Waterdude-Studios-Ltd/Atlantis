package de.waterdu.atlantis.ext.item;

import net.minecraft.SharedConstants;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.block.Blocks;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests the methods that {@link BlockExt} adds to blocks.
 */
class BlockExtTest {

    /**
     * Bootstraps Minecraft once before the tests, so that the registries are filled.
     */
    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    /**
     * Checks that the key and the ID of a block are those of its entry in the built-in block registry.
     */
    @Test
    void keyAndIdComeFromTheBuiltInRegistry() {
        var id = ResourceLocation.withDefaultNamespace("oak_log");

        assertThat(Blocks.OAK_LOG.key()).isEqualTo(ResourceKey.create(Registries.BLOCK, id));
        assertThat(Blocks.OAK_LOG.id()).isEqualTo(id);
    }

    /**
     * Checks that {@code configurable} uses the item of the block.
     */
    @Test
    void configurableUsesTheBlockItem() {
        assertThat(Blocks.OAK_LOG.configurable().originalString()).isEqualTo("minecraft:oak_log");
    }

    /**
     * Checks that {@code configurable} gives air for a block that has no item.
     */
    @Test
    void blocksWithoutAnItemBecomeAir() {
        assertThat(Blocks.FIRE.configurable().originalString()).isEqualTo("minecraft:air");
    }
}
