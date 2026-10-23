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

class BlockExtTest {

    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void keyAndIdComeFromTheBuiltInRegistry() {
        var id = ResourceLocation.withDefaultNamespace("oak_log");

        assertThat(Blocks.OAK_LOG.key()).isEqualTo(ResourceKey.create(Registries.BLOCK, id));
        assertThat(Blocks.OAK_LOG.id()).isEqualTo(id);
    }

    @Test
    void configurableUsesTheBlockItem() {
        assertThat(Blocks.OAK_LOG.configurable().originalString()).isEqualTo("minecraft:oak_log");
    }

    @Test
    void blocksWithoutAnItemBecomeAir() {
        assertThat(Blocks.FIRE.configurable().originalString()).isEqualTo("minecraft:air");
    }
}
