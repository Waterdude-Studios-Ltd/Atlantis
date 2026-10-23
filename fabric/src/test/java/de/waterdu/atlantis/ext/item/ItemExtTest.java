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

class ItemExtTest {

    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void keyAndIdComeFromTheBuiltInRegistry() {
        var id = ResourceLocation.withDefaultNamespace("diamond_sword");

        assertThat(Items.DIAMOND_SWORD.key()).isEqualTo(ResourceKey.create(Registries.ITEM, id));
        assertThat(Items.DIAMOND_SWORD.id()).isEqualTo(id);
    }

    @Test
    void blockItemsUseTheirOwnId() {
        assertThat(Items.STONE.id()).isEqualTo(ResourceLocation.withDefaultNamespace("stone"));
    }

    @Test
    void configurableIsBuiltFromTheId() {
        assertThat(Items.DIAMOND_SWORD.configurable().originalString()).isEqualTo("minecraft:diamond_sword");
    }
}
