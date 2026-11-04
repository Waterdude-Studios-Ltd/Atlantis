package de.waterdu.atlantis.ext.item;

import net.minecraft.SharedConstants;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests the methods that {@link ItemExt} adds to items.
 */
class ItemExtTest {

    /**
     * Bootstraps Minecraft once before the tests, so that the registries are filled.
     */
    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    /**
     * Checks that the key and the ID of an item are those of its entry in the built-in item registry.
     */
    @Test
    void keyAndIdComeFromTheBuiltInRegistry() {
        var id = ResourceLocation.withDefaultNamespace("diamond_sword");

        assertThat(Items.DIAMOND_SWORD.key()).isEqualTo(ResourceKey.create(Registries.ITEM, id));
        assertThat(Items.DIAMOND_SWORD.id()).isEqualTo(id);
    }

    /**
     * Checks that a block item has its own item ID.
     */
    @Test
    void blockItemsUseTheirOwnId() {
        assertThat(Items.STONE.id()).isEqualTo(ResourceLocation.withDefaultNamespace("stone"));
    }

    /**
     * Checks that {@code configurable} uses the item ID as the original string.
     */
    @Test
    void configurableIsBuiltFromTheId() {
        assertThat(Items.DIAMOND_SWORD.configurable().originalString()).isEqualTo("minecraft:diamond_sword");
    }
}
